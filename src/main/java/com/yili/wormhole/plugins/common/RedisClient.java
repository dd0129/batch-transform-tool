package com.yili.wormhole.plugins.common;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;

public final class RedisClient {
	private final static Logger LOG = Logger.getLogger(RedisClient.class);

	private int batchSize;
	private String redisFamily;
	
//	private ArrayList<RedisKey> rkList;
//	private ArrayList<String> vList;
	
	// Redis service
//	private static RedisUseInterface redisUse = null;
	
	static {
		try {
//			redisUse = new RedisUse();
///			LOG.info("Get RedisUse success");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public RedisClient(int batchSize, String redisFamily) {
		this.batchSize = batchSize; 
		this.redisFamily = new String(redisFamily);
		
//		rkList = new ArrayList<RedisKey>();
//		vList = new ArrayList<String>();
	}

	public void setExpire(String key, String value, int expireTime) {
        try {
//            RedisKey rKey = new RedisKey(redisFamily, key);
//            redisUse.set(rKey, value);
//            redisUse.expire(rKey, expireTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public void setBatch(String key, String value, int expireTime) throws IOException {
 /*       rkList.add(new RedisKey(redisFamily, key));
        vList.add(value);
        
        if (rkList.size() >= batchSize) {
            while(true){
                try {
                    redisUse.mset(rkList, vList, expireTime);
                    rkList.clear();
                    vList.clear();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }       
        }*/
    }

	public void setBatch(String key, String value) throws IOException {
/*		rkList.add(new RedisKey(redisFamily, key));
		vList.add(value);
		
		if (rkList.size() >= batchSize) {
			while(true){
				try {
					redisUse.mset(rkList,vList);
					rkList.clear();
					vList.clear();
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}		
		}*/
	}

    public void delete(String key) {
 /*       try {
            redisUse.del(new RedisKey(redisFamily, key));
        } catch(Exception ex) {
            LOG.error(ex);
        }*/
    }

	public void close() throws IOException {
		
	}

	public void flush() throws IOException {
	/*	if (rkList.size() > 0) {
			while (true) {
				try {
					redisUse.mset(rkList, vList);
					rkList.clear();
					vList.clear();
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}*/
	}
}