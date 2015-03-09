package com.yili.wormhole.plugins.writer.hdfswriter;

import com.yili.wormhole.common.JobStatus;
import com.yili.wormhole.common.WormholeException;
import com.yili.wormhole.common.interfaces.IParam;
import com.yili.wormhole.common.interfaces.ISourceCounter;
import com.yili.wormhole.common.interfaces.ITargetCounter;
import com.yili.wormhole.common.interfaces.IWriterPeriphery;
import com.yili.wormhole.plugins.common.DFSUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Shell.ShellCommandExecutor;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class HdfsWriterPeriphery implements IWriterPeriphery {

	private static final Logger logger = Logger
			.getLogger(HdfsWriterPeriphery.class.getName());

	private static final int HIVE_TABLE_ADD_PARTITION_PARAM_NUMBER = 2;
	private static final String HIDDEN_FILE_PREFIX = "_";
	private static final int MAX_LZO_CREATION_TRY_TIMES = 10;
	private static final long LZO_CREATION_TRY_INTERVAL_IN_MILLIS = 10000L;

	private String dir = "";
	private String prefixname = "";
	private int concurrency = 1;
	private String codecClass = "";
	private String fileType = "TXT";
	private String suffix = "";
	private boolean lzoCompressed = false;
	private String hiveTableAddPartitionOrNot = "false";
	private String hiveTableAddPartitionCondition = "";

	private final String ADD_PARTITION_SQL = "alter table {0} add if not exists partition({1}) location ''{2}'';";

	private FileSystem fs;

    private static final Configuration lzoIndexConfig;
    static {
        lzoIndexConfig = new Configuration();
        lzoIndexConfig.addResource("lzo-index-site.xml");
    }

	@Override
	public void rollback(IParam param) {
		deleteFilesOnHdfs(this.dir, this.prefixname);
	}

	@Override
	public void doPost(IParam param, ITargetCounter counter, int i) {
		deleteFilesOnHdfs(this.dir, this.prefixname);
		// rename temp files, make them visible to hdfs
		renameFiles();

		if (lzoCompressed
				&& "true".equalsIgnoreCase(param.getValue(
						ParamKey.createLzoIndexFile, "true").trim())) {
			createLzoIndex(dir);
		}

		/* add hive table partition if necessary */
		hiveTableAddPartitionOrNot = param.getValue(
				ParamKey.hiveTableAddPartitionSwitch, "false").trim();
		if (hiveTableAddPartitionOrNot.equalsIgnoreCase("true")) {
			hiveTableAddPartitionCondition = param.getValue(
					ParamKey.hiveTableAddPartitionCondition,
					this.hiveTableAddPartitionCondition);

			if (!StringUtils.isBlank(hiveTableAddPartitionCondition)) {
				String[] hqlParam = StringUtils.split(
                        hiveTableAddPartitionCondition, '@');
				if (HIVE_TABLE_ADD_PARTITION_PARAM_NUMBER == hqlParam.length) {
					String parCondition = hqlParam[0].trim().replace('"', '\'');
					String uri = hqlParam[1].trim();
					// split dbname and tablename
					String[] parts = StringUtils.split(uri, '.');

					if (StringUtils.isBlank(parCondition)
							|| StringUtils.isBlank(uri) || parts.length != 2
							|| StringUtils.isBlank(parts[0])
							|| StringUtils.isBlank(parts[1])) {
						logger.error(ParamKey.hiveTableAddPartitionSwitch
								+ " param can not be parsed correctly, please check it again");
						return;
					}

					try {
						addHiveTablePartition(parts[0].trim(), parts[1].trim(),
								parCondition, dir);
					} catch (IOException e) {
						throw new WormholeException(e.getMessage());
					}
				}
			}
		}
		return;
	}

	private void renameFiles() {
		try {
			String tempPrefixnamex = HIDDEN_FILE_PREFIX + prefixname;

			fs = DFSUtils.createFileSystem(new URI(dir),
					DFSUtils.getConf(dir, null));
			for (FileStatus status : fs.listStatus(new Path(dir))) {
				if (!status.isDir()) {
					String fileName = status.getPath().getName();
					if (fileName.startsWith(tempPrefixnamex)) {
						String parentPathName = status.getPath().getParent()
								.toString();
						Path absoluteSrcPath = status.getPath();
						// trim the ahead one '_' character
						fileName = fileName.substring(1);
						Path absoluteDstPath = new Path(parentPathName + "/"
								+ fileName);
						if (fs.rename(absoluteSrcPath, absoluteDstPath)) {
							logger.info("successfully rename file from "
									+ absoluteSrcPath.toString() + " to "
									+ absoluteDstPath.toString());
						} else {
							logger.error("failed to rename file from "
									+ absoluteSrcPath.toString() + " to "
									+ absoluteDstPath.toString());
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(String.format(
					"HdfsWriter rename temp files failed :%s,%s",
					e.getMessage(), e.getCause()));
			throw new WormholeException(
					"HdfsWriter rename temp files failed in do-post stage",
					JobStatus.POST_WRITE_FAILED.getStatus());
		} finally {
			closeAll();
		}
	}

	private void addHiveTablePartition(String dbName, String tableName,
			String partitionCondition, String location) throws IOException {
		StringBuffer addParitionCommand = new StringBuffer();
		addParitionCommand.append("hive -e \"");
		addParitionCommand.append("use " + dbName + ";");
		addParitionCommand.append(MessageFormat.format(ADD_PARTITION_SQL,
				tableName, partitionCondition, location));
		addParitionCommand.append("\"");

		logger.info(addParitionCommand.toString());

		int time = 0;
		do {
			String[] shellCmd = {"bash", "-c", addParitionCommand.toString()};
			ShellCommandExecutor shexec = new ShellCommandExecutor(shellCmd);
			shexec.execute();
			int exitCode = shexec.getExitCode();
			if (exitCode != 0) {
				time++;
                try {
                    Thread.sleep(30000L);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            } else {
				logger.info("hive table add partion hql executed correctly:"
						+ addParitionCommand.toString());
				break;
			}
		} while (time < MAX_LZO_CREATION_TRY_TIMES);

		if (time == MAX_LZO_CREATION_TRY_TIMES)
			throw new WormholeException("try"+ MAX_LZO_CREATION_TRY_TIMES +" add hdfs partition failed!");
	}

	// private void createHiveTable(IParam param){
	// String createTableOrNot = param.getValue(
	// ParamKey.hiveTableSwitch, "false");
	//
	// if (!createTableOrNot.equalsIgnoreCase("true"))
	// return;
	//
	// String tableFieldType = param.getValue(ParamKey.tableFieldType,
	// "").trim().toUpperCase();
	// if (StringUtils.isEmpty(tableFieldType))
	// return;
	//
	// String[] fieldTypes = tableFieldType.split(",");
	// for (int i = 0; i < fieldTypes.length; i++) {
	// String fieldType = fieldTypes[i];
	// if (fieldType.equalsIgnoreCase("bigint")){
	//
	// }else if(fieldType.equalsIgnoreCase("string")){
	//
	// }else{
	// logger.error(String.format("unnormal fieldType:%s in the param %s",
	// tableFieldType, ParamKey.tableFieldType));
	// return;
	// }
	// }
	// }

	private void createLzoIndex(final String directory) {
		int times = 1;
//		boolean idxCreated = false;
		do {
            logger.info("start to create lzo index file on " + directory
                    + " times: " + times);
            try {
                logger.info("lzo index directory => " + directory);
//				idxCreated = ToolRunner.run(lzoIndexConfig, new DistributedLzoIndexer(), new String[]{directory}) == 0;
                List<String> commands = new ArrayList<String>();
                commands.add("hadoop");
                commands.add("jar");
                commands.add(lzoIndexConfig.get("lzo.index.jar"));
                commands.add(lzoIndexConfig.get("lzo.index.jar.main"));
                commands.add(directory);

                ProcessBuilder processBuilder = new ProcessBuilder(commands);
                Process p = processBuilder.start();

                if (p.waitFor() != 0) {
                    throw new Exception();
                } else {
                    logger.info("create lzo index info is => " + org.apache.commons.io.IOUtils.toString(p.getErrorStream()));
                    break;
                }
            } catch (Throwable t) {
                logger.error(String
                        .format("HdfsWriter doPost stage create index %s failed, start to sleep %d millis sec, %s,%s",
								directory,
								LZO_CREATION_TRY_INTERVAL_IN_MILLIS,
								t.getMessage(), t.getCause()));
                try {
                    Thread.sleep(LZO_CREATION_TRY_INTERVAL_IN_MILLIS);
                } catch (InterruptedException ite) {
                    ite.printStackTrace(System.err);
                }
            }
        } while (times++ <= MAX_LZO_CREATION_TRY_TIMES);

		if (times > MAX_LZO_CREATION_TRY_TIMES) {
            throw new WormholeException(
                    "lzo index creation failed after try "
                            + MAX_LZO_CREATION_TRY_TIMES + " times",
                    JobStatus.POST_WRITE_FAILED.getStatus());
        }

		if (existIncompleteFile(dir)) {
            logger.error(String.format(
                    "HdfsWriter doPost stage create index %s failed:",
                    directory));
            throw new WormholeException(
                    "dfsWriter doPost stage create index failed",
                    JobStatus.POST_WRITE_FAILED.getStatus());
        }
	}

	@Override
	public void prepare(IParam param, ISourceCounter counter) {
		dir = param.getValue(ParamKey.dir);
		prefixname = param.getValue(ParamKey.prefixname, "prefix");
		concurrency = param.getIntValue(ParamKey.concurrency, 1);

		if (dir.endsWith("*")) {
			dir = dir.substring(0, dir.lastIndexOf("*"));
		}
		if (dir.endsWith("/")) {
			dir = dir.substring(0, dir.lastIndexOf("/"));
		}

		fileType = param.getValue(ParamKey.fileType, this.fileType);
		codecClass = param.getValue(ParamKey.codecClass, "");
		if (fileType.equalsIgnoreCase("TXT_COMP")) {
			suffix = DFSUtils.getCompressionSuffixMap().get(codecClass);
			if (StringUtils.isEmpty(suffix)) {
				suffix = "lzo";
			}
		}

		if (suffix.equalsIgnoreCase("lzo")) {
			lzoCompressed = true;
		}
		deleteFilesOnHdfs(this.dir, HIDDEN_FILE_PREFIX + this.prefixname);
	}

	private void closeAll() {
		try {
			IOUtils.closeStream(fs);
		} catch (Exception e) {
			logger.error(String.format(
					"HdfsWriter closing filesystem failed: %s,%s",
					e.getMessage(), e.getCause()));
		}
	}

	private boolean existIncompleteFile(String dir) {
		try {
			fs = DFSUtils.createFileSystem(new URI(dir),
					DFSUtils.getConf(dir, null));
			for (FileStatus status : fs.listStatus(new Path(dir))) {
				if (status.getPath().getName().endsWith(".tmp")) {
					return true;
				}
			}
		} catch (Exception e) {
			logger.error(String.format(
					"HdfsWriter Init file system failed:%s,%s", e.getMessage(),
					e.getCause()));
		} finally {
			closeAll();
		}
		return false;
	}

	private void deleteFilesOnHdfs(String dir, String prefix) {
		try {
			fs = DFSUtils.createFileSystem(new URI(dir),
					DFSUtils.getConf(dir, null));
			DFSUtils.deleteFiles(fs, new Path(dir + "/" + prefix + "*"),
						true, true);
		} catch (Exception e) {
			logger.error(String.format(
					"HdfsWriter Init file system failed:%s,%s", e.getMessage(),
					e.getCause()));
		} finally {
			closeAll();
		}
	}
}
