package com.yili.wormhole.plugins.writer.rediswriter;

import com.yili.wormhole.common.AbstractPlugin;
import com.yili.wormhole.common.JobStatus;
import com.yili.wormhole.common.WormholeException;
import com.yili.wormhole.common.interfaces.ILine;
import com.yili.wormhole.common.interfaces.ILineReceiver;
import com.yili.wormhole.common.interfaces.IWriter;
import com.yili.wormhole.plugins.common.BufferJson;
import com.yili.wormhole.plugins.common.BufferString;
import com.yili.wormhole.plugins.common.OutputBufferInterface;
import com.google.common.base.Preconditions;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class RedisWriter extends AbstractPlugin implements IWriter {

    private final static Logger LOG = Logger.getLogger(RedisWriter.class);

    private final static int MININUM_FIELD_NUM = 2;
    private final static int JSON_SERIAL = 0;
    private final static int STRING_SERIAL = 1;

    private final static int TWENTY_YEARS = 20 * 365 * 24 * 60 * 60;

    private int keyIndex;
    private String columnsName;
    private String table;
    private String family;
    private int serialize;
    private String separator;
    private int num_to_wait;
    private int wait_time;
    private boolean write_sleep;
    private int batchSize;
    private boolean clearKey;
    private int expireTime;
    private String nodes;
    private String sentinels;
    private String name;

    private String[] columns;

    private RedisClient redisClient;

    private OutputBufferInterface redisBuffer;

    @Override
    public void init() {
    	nodes =  getParam().getValue(ParamKey.nodes);
    	sentinels =  getParam().getValue(ParamKey.sentinels);
    	name =  getParam().getValue(ParamKey.name);
        keyIndex = getParam().getIntValue(ParamKey.keyIndex, 0);
        columnsName = getParam().getValue(ParamKey.columnsName);
        num_to_wait = getParam().getIntValue(ParamKey.num_to_wait, 10000);
        wait_time = getParam().getIntValue(ParamKey.wait_time, 1000);
        write_sleep = getParam().getBooleanValue(ParamKey.write_sleep, true);
        table = getParam().getValue(ParamKey.table);
        family = getParam().getValue(ParamKey.family);
        serialize = getParam().getIntValue(ParamKey.serialize, 1);
        separator = getParam().getValue(ParamKey.separator, ",");
        batchSize = getParam().getIntValue(ParamKey.batchSize);
        clearKey = getParam().getBooleanValue(ParamKey.clear_key, false);
        expireTime = getParam().getIntValue(ParamKey.expire_time, TWENTY_YEARS);

        columns = StringUtils.split(columnsName, ',');

        if (serialize == JSON_SERIAL) {
            redisBuffer = new BufferJson();
        } else if (serialize == STRING_SERIAL) {
            redisBuffer = new BufferString(separator);
        } else {
            redisBuffer = new BufferString(separator);
        }
    }

    @Override
    public void connection() {
        redisClient = new RedisClient(sentinels, nodes, name, batchSize, table + "." + family);
        Preconditions.checkNotNull(redisClient);
    }

    @Override
    public void write(ILineReceiver lineReceiver) {
        ILine line;
        int count = 0;
        try {
            while ((line = lineReceiver.receive()) != null) {
                int fieldNum = line.getFieldNum();
                if (fieldNum < MININUM_FIELD_NUM) {
                    LOG.warn("field number is less than " + MININUM_FIELD_NUM
                            + ", consider it as an empty line: " + line.toString(','));
                    continue;
                }

                String key = line.getField(keyIndex);

                if (StringUtils.isEmpty(key)) {
                    LOG.warn("row key is null, ignore it: " + line.toString(','));
                    continue;
                }

                if (!clearKey) {
                    // value不应包含key
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
                        redisBuffer.put(columns[i - 1], line.getField(i));
                    }
                    LOG.debug("origin key: " + key + ", value: " + redisBuffer.toString());
                    if (redisBuffer == null || redisBuffer.toString().isEmpty()) {
                        redisClient.delete(key);
                    } else {
                        redisClient.setBatch(key, redisBuffer.toString(), expireTime);
                    }
                    redisBuffer.clear();
                } else {
                    redisClient.delete(key);
                }

                count++;
                getMonitor().increaseSuccessLines();

                // slow the write speed
                if (write_sleep) {
                    if (count >= num_to_wait) {
                        Thread.sleep(wait_time);
                        count = 0;
                    }
                }
            }
        } catch (Exception e) {
            throw new WormholeException(e, JobStatus.WRITE_DATA_EXCEPTION.getStatus());
        }
    }

    @Override
    public void commit() {
        try {
            redisClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {}
}
