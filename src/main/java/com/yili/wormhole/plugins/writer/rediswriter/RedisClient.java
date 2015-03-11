package com.yili.wormhole.plugins.writer.rediswriter;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public final class RedisClient {
	private final static Logger LOG = Logger.getLogger(RedisClient.class);

	private int batchSize;
	private String redisFamily;

	private static boolean initilized = false;

	private ArrayList<String> rkList;
	private ArrayList<String> vList;

	// Redis service
	private static RedisUse redisUse = null;

	private synchronized void init(final String sentinelList,
			final String redisList, final String name) {
		if (initilized)
			return;
		initilized = true;

		try {
			redisUse = new RedisUse(sentinelList, redisList, name);
			LOG.info("Get RedisUse success");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public RedisClient(final String sentinelList, final String redisList,
			final String name, int batchSize, String family) {
		init(sentinelList, redisList, name);
		this.batchSize = batchSize;
		redisFamily = family;

		rkList = new ArrayList<String>();
		vList = new ArrayList<String>();
	}

	public void setExpire(String key, String value, int expireTime) {
		try {
			redisUse.set(key + "_" + redisFamily, value);
			redisUse.expire(key, expireTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setBatch(String key, String value, int expireTime)
			throws IOException {
		rkList.add(key + "_" + redisFamily);
		vList.add(value);

		if (rkList.size() >= batchSize) {
			while (true) {
				try {
					redisUse.mset(rkList, vList, expireTime);
					rkList.clear();
					vList.clear();
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setBatch(String key, String value) throws IOException {
		rkList.add(key + "_" + redisFamily);
		vList.add(value);

		if (rkList.size() >= batchSize) {
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
		}
	}

	public void delete(String key) {
		try {
			redisUse.del(key + "_" + redisFamily);
		} catch (Exception ex) {
			LOG.error(ex);
		}
	}

	public void close() throws IOException {

	}

	public void flush() throws IOException {
		if (rkList.size() > 0) {
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
		}
	}
}