package com.yili.wormhole.plugins.reader.hivereader;

import com.yili.wormhole.common.JobStatus;
import com.yili.wormhole.common.WormholeException;
import com.yili.wormhole.common.interfaces.IParam;
import com.yili.wormhole.common.interfaces.IReaderPeriphery;
import com.yili.wormhole.common.interfaces.ISourceCounter;
import com.yili.wormhole.common.interfaces.ITargetCounter;
import com.yili.wormhole.plugins.common.DFSUtils;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class HiveReaderPeriphery implements IReaderPeriphery {
	private static final Logger LOG = Logger
			.getLogger(HiveReaderPeriphery.class);

	private static final String INSERT_SQL_PATTERN = "INSERT OVERWRITE DIRECTORY '%s' %s";

	private static final int EXEC_TIME_THREDHOLD = 60 * 50 * 1000; // 设置任务最多执行时间
	//private static final long THREAD_SLEEP_TIME = 30000L; // 设置guard线程睡眠时间

	private String path = "jdbc:hive://10.1.1.161:10000/default";
	private String username = "";
	private String password = "";
	private String sql = "";
	private String dataDir = "";
	private int reduceNumber = -1;
	public Path absolutePath;
	private String mode = HiveReaderMode.READ_FROM_HIVESERVER.getMode();
	private HiveJdbcClient client;
	private Configuration conf;
	private FileSystem fs;

	@Override
	public void prepare(IParam param, ISourceCounter counter) {
		mode = param.getValue(ParamKey.mode, mode);
		Preconditions
				.checkArgument(
						mode.equals(HiveReaderMode.READ_FROM_HDFS.getMode())
								|| mode.equals(HiveReaderMode.READ_FROM_HIVESERVER
										.getMode()) || mode.equals(HiveReaderMode.READ_FROM_LOCAL.getMode()),
						"hive reader mode should be READ_FROM_HDFS or READ_FROM_HIVESERVER or READ_FROM_LOCAL)");
		if (mode.equals(HiveReaderMode.READ_FROM_HDFS.getMode())) {
			path = param.getValue(ParamKey.path, path);
			username = param.getValue(ParamKey.username, username);
			password = param.getValue(ParamKey.password, password);
			reduceNumber = param.getIntValue(ParamKey.reduceNumber,
					reduceNumber);
			sql = param.getValue(ParamKey.sql, sql).trim();
			if (sql.endsWith(";")) {
				sql = StringUtils.substring(sql, 0, sql.length() - 1);
			}
			dataDir = param.getValue(ParamKey.dataDir, dataDir);
			try {
				createTempDir();
				param.putValue(ParamKey.dataDir, absolutePath.toString());
				sql = String.format(INSERT_SQL_PATTERN,
						absolutePath.toString(), sql);
				client = new HiveJdbcClient.Builder(path).username(username)
						.password(password).sql(sql).build();
				client.initialize();
				client.processInsertQuery(reduceNumber);
			} catch (Exception e) {
				throw new WormholeException(e,
						JobStatus.READ_FAILED.getStatus());
			} finally {
				if (client != null) {
					client.close();
				}
			}
		} else if (mode.equals(HiveReaderMode.READ_FROM_LOCAL.getMode())) {
			sql = param.getValue(ParamKey.sql, sql).trim();
			if (sql.endsWith(";")) {
				sql = StringUtils.substring(sql, 0, sql.length() - 1);
			}
			dataDir = param.getValue(ParamKey.dataDir, dataDir);
			try {
				createTempDir();
				param.putValue(ParamKey.dataDir, absolutePath.toString());
				sql = String.format(INSERT_SQL_PATTERN,
						absolutePath.toString(), sql);
				sql = sql.replaceAll("`", "");
				runHiveWithTimeout();
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			} catch (URISyntaxException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	private static class ProcessUtil {
		public static int executeCommand (final List<String> command, final long timeout) throws IOException {
			ProcessBuilder hiveProcessBuilder = new ProcessBuilder(command);
			hiveProcessBuilder.redirectErrorStream(true); // combine the stream
			Process p = hiveProcessBuilder.start();
			ProcessWorker processWorker = new ProcessWorker(p);
			processWorker.start();
			try {
				processWorker.join(timeout);
				if (processWorker.exit != null) {
					if (processWorker.exit == 0) {
						LOG.info("hive -e executed => SUCCESS");
						return processWorker.exit;
					} else {
						throw new WormholeException("hive execute => FAILED");
					}
				} else {
					throw new WormholeException("hive execute hung => FAILED");
				}
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(), e);
				throw new WormholeException("Process Thread interrupted => FAILED");
			}
		}
	}

	static class ProcessWorker extends Thread {
		private final Process process;
		private Integer exit;

		public ProcessWorker(Process process) {
			this.process = process;
		}

		@Override
		public void run() {
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					LOG.info(line);
				}
				exit = process.waitFor();
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(), e);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	private int runHiveWithTimeout () throws IOException {
		List<String> command = new ArrayList<String>();
		command.add("hive");
		command.add("-e");
		command.add(sql);
		LOG.info("start hive -e => " + sql);

		return ProcessUtil.executeCommand(command, EXEC_TIME_THREDHOLD);
	}

//	private void runHiveCommandWithRetry () throws IOException, InterruptedException {
//		final Process proc = getHiveCommandRunningProcess();
//		LOG.info("hive -e executing...");
//		final DateTime startTime = new DateTime();
//		Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler(){
//			@Override
//			public void uncaughtException(Thread t, Throwable e) {
//				WormholeException wormholeException = (WormholeException) e;
//				JobStatus jobStatus = JobStatus.fromStatus(wormholeException.getStatusCode());
//				switch(jobStatus) {
//					case READ_FROM_LOCAL_FAILED:
//					case READ_FROM_LOCAL_HUNG:
//						LOG.error(wormholeException.getMessage() + "--returncode: " + wormholeException.getStatusCode());
//						break;
//				}
//				System.exit(-1);
//			}
//		};
//		Thread t = new Thread() {
//			@Override
//			public void run() {
//				while(true) {
//					try {
//						if (proc.exitValue() == 0) {
//							LOG.info("hive -e executed => SUCCESS");
//							break;
//						} else {
//							throw new WormholeException("hive execute => FAILED", JobStatus.READ_FROM_LOCAL_FAILED.getStatus());
//						}
//					} catch (IllegalThreadStateException e) {
//						if (new DateTime().minusMinutes(EXEC_TIME_THREDHOLD).isAfter(startTime)) {
//							proc.destroy();
//							throw new WormholeException("hive execute => FAILED", JobStatus.READ_FROM_LOCAL_HUNG.getStatus());
//						}
//						try {
//							Thread.sleep(THREAD_SLEEP_TIME);
//						} catch (InterruptedException e1) {
//							LOG.error(e1.getMessage(), e1);
//						}
//					}
//				}
//			}
//		};
//		t.setUncaughtExceptionHandler(exceptionHandler);
//		t.start();
//
//		consumeCommandPipeBuffers(proc);
//		proc.waitFor();
//	}
//
//	private Process getHiveCommandRunningProcess() throws IOException {
//		List<String> command = new ArrayList<String>();
//		command.add("hive");
//		command.add("-e");
//		command.add(sql);
//		LOG.info("start hive -e => " + sql);
//		ProcessBuilder hiveProcessBuilder = new ProcessBuilder(command);
//		hiveProcessBuilder.redirectErrorStream(true); // combine the stream
//		return hiveProcessBuilder.start();
//	}
//
//	private void consumeCommandPipeBuffers(Process p) throws IOException {
//		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//		String line;
//		while ((line = bufferedReader.readLine()) != null) {
//			LOG.info(line);
//		}
//	}

	private String createTempDir() throws IOException, URISyntaxException {
		conf = DFSUtils.getConf(dataDir, null);
		fs = DFSUtils.createFileSystem(new URI(dataDir), conf);
		absolutePath = new Path(dataDir, createFilename(sql));
		fs = absolutePath.getFileSystem(conf);
		if (fs.mkdirs(absolutePath)) {
			LOG.info("create data temp directory successfully "
					+ absolutePath.toString());
		} else {
			LOG.error("Failed to mkdir " + absolutePath.toString());
			throw new WormholeException(JobStatus.READ_FAILED.getStatus());
		}
		return absolutePath.toString();
	}

	private String createFilename(String sql) {
		return DigestUtils.md5Hex(sql + System.currentTimeMillis());
	}

	@Override
	public void doPost(IParam param, ITargetCounter counter, int i) {
		if ((mode.equals(HiveReaderMode.READ_FROM_HDFS.getMode()) || mode.equals(HiveReaderMode.READ_FROM_LOCAL.getMode()))
				&& absolutePath != null) {
			try {
				if (fs.exists(absolutePath)) {
					fs.delete(absolutePath, true);
					LOG.info(absolutePath.toString()
							+ " has been deleted at dopost stage");
				}
			} catch (IOException e) {
				LOG.info("Failed to delete " + absolutePath.toString());
			}
		}
	}
}
