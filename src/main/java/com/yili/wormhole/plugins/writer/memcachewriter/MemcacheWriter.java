package com.yili.wormhole.plugins.writer.memcachewriter;

import com.dp.nebula.wormhole.common.AbstractPlugin;
import com.dp.nebula.wormhole.common.JobStatus;
import com.dp.nebula.wormhole.common.WormholeException;
import com.dp.nebula.wormhole.common.interfaces.ILine;
import com.dp.nebula.wormhole.common.interfaces.ILineReceiver;
import com.dp.nebula.wormhole.common.interfaces.IWriter;
import com.dp.nebula.wormhole.plugins.common.BufferJson;
import com.dp.nebula.wormhole.plugins.common.BufferString;
import com.dp.nebula.wormhole.plugins.common.MemcacheClient;
import com.dp.nebula.wormhole.plugins.common.OutputBufferInterface;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class MemcacheWriter extends AbstractPlugin implements IWriter {
	private final static Logger LOG = Logger.getLogger(MemcacheWriter.class);

	private final static int MININUM_FIELD_NUM = 2;
	private final static int JSONSERIAL = 0;
	private final static int STRINGSERIAL = 1;
	
	private int keyIndex;
	private String columnsName;
	private String[] columns;
	private String table;
	private String family;
	private int serialize; 
	private String separator;
	private MemcacheClient memcacheClinet;
	private int num_to_wait;
	private int wait_time;
	private boolean write_sleep;
	
	private OutputBufferInterface redisBuffer;

	@Override
	public void init() {
		keyIndex = getParam().getIntValue(ParamKey.keyIndex, 0);
		columnsName = getParam().getValue(ParamKey.columnsName);
		num_to_wait = getParam().getIntValue(ParamKey.num_to_wait, 10000);
		wait_time   = getParam().getIntValue(ParamKey.wait_time, 1000);
		write_sleep = getParam().getBooleanValue(ParamKey.write_sleep, true);
		table = getParam().getValue(ParamKey.table);
		family = getParam().getValue(ParamKey.family);
		serialize = getParam().getIntValue(ParamKey.serialize, 1);
		separator = getParam().getValue(ParamKey.separator, ",");
		
		columns =  StringUtils.split(columnsName, ',');
		
		if(serialize == JSONSERIAL){
			redisBuffer = new BufferJson();
		}else if(serialize == STRINGSERIAL){
			redisBuffer = new BufferString(separator);
		}else{
			redisBuffer = new BufferString(separator);
		}
	}

	@Override
	public void connection() {
		memcacheClinet = new MemcacheClient(table+"."+family);
		Preconditions.checkNotNull(memcacheClinet);
	}

	@Override
	public void write(ILineReceiver lineReceiver) {
		ILine line;
		int count = 0;
		try {
			while ((line = lineReceiver.receive()) != null) {
				int fieldNum = line.getFieldNum();
				if (fieldNum < MININUM_FIELD_NUM) {
					LOG.warn("field number is less than " + MININUM_FIELD_NUM + " consider it as an empty line:" + line.toString(','));
					continue;
				}
				String key = line.getField(keyIndex);
				
				if (StringUtils.isEmpty(key)) {
					LOG.warn("row key is null, ignore it");
					getMonitor().increaseSuccessLines();
					continue;
				}
				
				for (int i = 0; i < keyIndex; i++) {
					if (line.getField(i) == null) {
						continue;
					}
					redisBuffer.put(columns[i], line.getField(i));   
				}
				for (int i = keyIndex + 1; i < fieldNum; i++) {
					if (line.getField(i) == null) {
						continue;
					} 
					redisBuffer.put(columns[i-1], line.getField(i));   
				}
				LOG.debug("origin key:" + key +  "value:" + redisBuffer.toString());
				memcacheClinet.set(key, redisBuffer.toString());
				redisBuffer.clear();
				
				count++;
				
				// for slow the write speed 
				if ( write_sleep ){
					if ( count >= num_to_wait ){
						Thread.sleep(wait_time);
						count = 0;
					}
				}
				getMonitor().increaseSuccessLines();
			}
		} catch (Exception e) {
			throw new WormholeException(e, JobStatus.WRITE_DATA_EXCEPTION.getStatus());
		}
	}

	@Override
	public void commit() {
		try {
			memcacheClinet.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void finish() {
	}
}
