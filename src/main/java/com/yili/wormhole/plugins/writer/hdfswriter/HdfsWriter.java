package com.yili.wormhole.plugins.writer.hdfswriter;

import com.yili.wormhole.common.AbstractPlugin;
import com.yili.wormhole.common.interfaces.ILine;
import com.yili.wormhole.common.interfaces.ILineReceiver;
import com.yili.wormhole.common.interfaces.ITransformer;
import com.yili.wormhole.common.interfaces.IWriter;
import com.yili.wormhole.plugins.common.DFSUtils;
import com.yili.wormhole.transform.common.TransformerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HdfsWriter extends AbstractPlugin implements IWriter {
	private static final Logger logger = Logger.getLogger(HdfsWriter.class);
	private static volatile boolean compressionTypePrintVirgin = true;

	private static final int WRITE_TRY_TIMES = 5;
	private static final long WRITE_SLEEP_TIME = 5000L;
	
	private FileSystem fs;
	private Path p = null;
	private char fieldSplit = '\u0001';
	private char lineSplit = '\n';
	private int bufferSize = 8 * 1024;
	private String encoding = "UTF-8";
	private String dir = "";
	private String replaceChar = "";
	private Map<Character, Character> replaceCharMap = null;
	private char[] nullChars = null;
	
	private String dataTransform;
	private String dataTransformParams;

	
	private DfsWriterStrategy dfsWriterStrategy = null;
	
	static {
		Thread.currentThread().setContextClassLoader(HdfsWriter.class.getClassLoader());
	}
	
	@Override
	public void init() {
		fieldSplit = getParam().getCharValue(ParamKey.fieldSplit,
				fieldSplit);
		encoding = getParam().getValue(ParamKey.encoding, encoding);
		lineSplit = getParam().getCharValue(ParamKey.lineSplit,
				lineSplit);
		bufferSize = getParam().getIntValue(ParamKey.bufferSize,
				bufferSize);
		nullChars = getParam().getValue(ParamKey.nullChar, "")
				.toCharArray();
		replaceChar = getParam().getValue(ParamKey.replaceChar, "");
		
		replaceCharMap = parseReplaceChar(replaceChar);
		
		dir = getParam().getValue(ParamKey.dir, this.dir);
		dataTransform = getParam().getValue(ParamKey.dataTransformClass, "");
		dataTransformParams = getParam().getValue(ParamKey.dataTransformParams, "");

		try {
            logger.info("dir :=" +dir);
            fs = DFSUtils.createFileSystem(new URI(dir),
					DFSUtils.getConf(dir, null));
		} catch (Exception e) {
			logger.error(String.format(
					"HdfsWriter Initialize file system failed:%s,%s",
					e.getMessage(), e.getCause()));
            logger.error(e.getMessage(),e);
			closeAll();
		}
	
		if (!StringUtils.isBlank(dir)) {
			p = new Path(dir);
		} else {
			closeAll();
			logger.error("Can't find the param ["
					+ ParamKey.dir + "] in hdfs-writer-param.");
			return;
		}

		String filetype = getParam().getValue(ParamKey.fileType, "TXT");
		if ("TXT_COMP".equalsIgnoreCase(filetype))
			dfsWriterStrategy = new DfsWriterTextFileStrategy(true);
		else if ("TXT".equalsIgnoreCase(filetype))
			dfsWriterStrategy = new DfsWriterTextFileStrategy(false);
		else {
			closeAll();
			logger.error(
					"HdfsWriter cannot recognize filetype: " + filetype);
		}
	}

	private Map<Character, Character> parseReplaceChar(String replaceChar) {
		replaceCharMap = new HashMap<Character, Character>();
		if (!StringUtils.isBlank(replaceChar)){
			String[] items = StringUtils.split(replaceChar, ":");
			if (2 == items.length){
				char[] srcChars = items[0].toCharArray();
				char[] destChars = items[1].toCharArray();
				if (destChars.length != 1 || srcChars.length == 0){
					throw new IllegalArgumentException(String.format(
							"paramKey replaceChar '%s' are not properly set, pleace check it again", replaceChar));
				}else{
					for (char srcC : srcChars)
						replaceCharMap.put(srcC, destChars[0]);
				}
			}
		}
		return replaceCharMap;
	}

	@Override
	public void connection() {
		if (p == null) {
			closeAll();
			logger.error("HdfsWriter Can't initialize file system .");
		}
		try {
			dfsWriterStrategy.open();
		} catch (Exception ex) {
			closeAll();
			logger.error(ex.toString());
		}
	}

	@Override
	public void finish() {
		closeAll();
	}

	@Override
	public void commit() {
	}

	@Override
	public void write(ILineReceiver lineReceiver) {
		ITransformer transformer = null;
		if(!dataTransform.isEmpty()) {
			transformer = TransformerFactory.create(dataTransform);
		}
		try {
			dfsWriterStrategy.write(lineReceiver,transformer,dataTransformParams);
		} catch (Exception ex) {
			logger.error(String.format(
					"Some errors occurs on starting writing: %s,%s",
					ex.getMessage(), ex.getCause()));
		} finally {
			dfsWriterStrategy.close();
			closeAll();
		}

	}

	public interface DfsWriterStrategy {
		void open();

		void write(ILineReceiver receiver, ITransformer transformer, String transformerParams);

		void close();
	}
	
	class DfsWriterTextFileStrategy implements DfsWriterStrategy {
		private FSDataOutputStream out = null;

		private BufferedWriter bw = null;

		private CompressionOutputStream co = null;

		private boolean compressed = false;

		public DfsWriterTextFileStrategy(boolean compressed) {
			this.compressed = compressed;
		}

		@Override
		public void close() {
			IOUtils.cleanup(null, bw, out, co);
		}

		@Override
		public void open() {
			try {
				if (compressed) {
					logger.info("creating compressed file " + p.toString());
					
					//using LzopCodec as default option
					String codecClassName = getParam().getValue(
							ParamKey.codecClass,
							"com.hadoop.compression.lzo.LzopCodec");

					Class<?> codecClass = Class.forName(codecClassName);
					Configuration conf = DFSUtils.getConf(dir, null);
					CompressionCodec codec = (CompressionCodec) ReflectionUtils
							.newInstance(codecClass, conf);
					
					if (compressionTypePrintVirgin) {
						logger.info("reflection using compression codec class: " + codec.getClass().getName());
						compressionTypePrintVirgin = false;
					}
					
					out = fs.create(p, false, bufferSize);
					co = codec.createOutputStream(out);
					bw = new BufferedWriter(
							new OutputStreamWriter(co, encoding), bufferSize);
				} else {
					out = fs.create(p, false, bufferSize);
					bw = new BufferedWriter(new OutputStreamWriter(out,
							encoding), bufferSize);
				}
			} catch (Exception e) {
				logger.error(e.toString(),e);
			}
		}

		// retry
		private void safeWrite(char[] c) throws Exception {
			int times = 0;
			do {
				try {
					bw.write(c);
					break;
				} catch (Exception e) {
					if (times == WRITE_TRY_TIMES)
						throw new Exception("hdfs write retry " + times + " times, fail!");
					times++;
					logger.warn("safe write retry " + times, e);
					try {
						Thread.sleep(WRITE_SLEEP_TIME);
					} catch (InterruptedException ite) {
						ite.printStackTrace(System.err);
					}
				}
			} while(times < WRITE_TRY_TIMES);
		}

		// retry
		private void safeWrite(char c) throws Exception {
			int times = 0;
			do {
				try {
					bw.write(c);
					break;
				} catch (Exception e) {
					if (times == WRITE_TRY_TIMES)
						throw new Exception("hdfs write retry " + times + " times, fail!");
					times++;
					logger.warn("safe write retry "+ times, e);
					try {
						Thread.sleep(WRITE_SLEEP_TIME);
					} catch (InterruptedException ite) {
						ite.printStackTrace(System.err);
					}
				}
			} while(times < WRITE_TRY_TIMES);
		}

		@Override
		public void write(ILineReceiver receiver,ITransformer transformer, String transformerParams) {
			ILine line;
			try {
				while ((line = receiver.receive()) != null) {
					if(transformer != null ) {
						if(transformerParams != null && !transformerParams.equals("")) {
							line = transformer.transform(line,transformerParams);
						} else {
							line = transformer.transform(line);
						}
					}
					int len = line.getFieldNum();
					
					for (int i = 0; i < len; i++) {
//						bw.write(replaceChars(line.getField(i), replaceCharMap));
						safeWrite(replaceChars(line.getField(i), replaceCharMap));
						if (i < len - 1)
							//bw.write(fieldSplit);
							safeWrite(fieldSplit);
					}
					//bw.write(lineSplit);
					safeWrite(lineSplit);
					
					getMonitor().increaseSuccessLines();
				}
				bw.flush();
			} catch (Exception e) {
				logger.error(e.toString(),e);
				System.exit(-1);
			}
		}
	}
	
	/**
	 * Replace field string with space character when it contains field split character, \r or \n
	 * 
	 * @param str
	 *            source string
	 * 
	 * @param   replaceCharMap
	 *   		  fieldSplit
	 * @return replaced character array.
	 * */
	private char[] replaceChars(String str, Map<Character, Character> replaceCharMap) {
		if (null == str) {
			return this.nullChars;
		}
		
		char[] newchars = str.toCharArray();
		int strLength = newchars.length;
		
		//when user doesn't fill the replaceChar parameter, we will replace \r \n and fieldSplit with ' ' as default
		if (replaceCharMap == null || replaceCharMap.isEmpty()){
			for (int i = 0; i < strLength; i++) {
				if (fieldSplit == newchars[i] || 13 == newchars[i]
						|| 10 == newchars[i]) {
					newchars[i] = ' ';
				}	
			}
		}
		// else we will replace char as user specify 
		else{
			for (int i = 0; i < strLength; i++) {
				if (replaceCharMap.containsKey(newchars[i])){
					newchars[i] = replaceCharMap.get(newchars[i]);
				}
			}
		}
		
		return newchars;
	}
	
	private void closeAll() {
		try {
			fs.close();
		} catch (Exception e) {
			logger.error(String.format(
					"HdfsWriter closing filesystem failed: %s,%s",
					e.getMessage(), e.getCause()));
		}
	}
}
