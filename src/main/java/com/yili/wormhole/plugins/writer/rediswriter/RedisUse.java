package com.yili.wormhole.plugins.writer.rediswriter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Pipeline;

public class RedisUse {
	private static Logger log = Logger.getLogger(RedisUse.class);

	private static boolean initilized = false;
	private static final String SUCCESS_RESPONSE = "OK";
	private static RedisDataNode redisNode = null;

	
	public RedisUse(final String sentinelList, final String redisList,
			final String name) {
		init(sentinelList, redisList, name);
	}
	
	private synchronized void init(final String sentinelList, final String redisList,
			final String name) {
		if (initilized)
			return;
		initilized = true;

		RedisConf redisConf = new RedisConf();
		redisNode = redisConf.getDataNode(sentinelList, redisList, name);
	}

	public String set(final String key, final String value) throws Exception {
		JedisSentinelPool jp = redisNode.getSentinelPool();

		Jedis je = null;

		boolean success = true;
		try {
			je = jp.getResource();
			return je.set(key, value);
		} catch (Exception e) {
			success = false;
			if (je != null)
				jp.returnBrokenResource(je);
			String error = getLogInfo(jp.getCurrentHostMaster(), key, "");
			throw new Exception(error, e);
		} finally {
			if (success && je != null) {
				jp.returnResource(je);
			}
		}
	}
	

	public String mset(final List<String> redisKeys, final List<String> values) throws Exception {

		List<String> kvPairList = new ArrayList<String>();
		for (int i = 0; i < redisKeys.size(); i++) {
			kvPairList.add(redisKeys.get(i));
			kvPairList.add(values.get(i));
		}

		boolean success = true;
		Jedis je = null;
		JedisSentinelPool jp = null;
		try {
			jp = redisNode.getSentinelPool();
			je = jp.getResource();
			Pipeline p = je.pipelined();
			String[] kvPairs = new String[kvPairList.size()];
			kvPairs = kvPairList.toArray(kvPairs);
			p.mset(kvPairs);
			p.sync();
		} catch (Exception e) {
			success = false;
			if (je != null)
				jp.returnBrokenResource(je);
			String error = getLogInfo(jp.getCurrentHostMaster(),
					kvPairList.toString(), "");
			throw new Exception(error, e);
		} finally {
			if (success && je != null) {
				jp.returnResource(je);
			}
		}

		return SUCCESS_RESPONSE;
	}

	public String mset(final List<String> redisKeys, final List<String> values,
			final int expireTime) throws Exception {

		List<String> kvPairList = new ArrayList<String>();
		for (int i = 0; i < redisKeys.size(); i++) {
			kvPairList.add(redisKeys.get(i));
			kvPairList.add(values.get(i));
		}

		boolean success = true;
		Jedis je = null;
		JedisSentinelPool jp = null;
		try {
			jp = redisNode.getSentinelPool();
			je = jp.getResource();
			Pipeline p = je.pipelined();
			String[] kvPairs = new String[kvPairList.size()];
			kvPairs = kvPairList.toArray(kvPairs);
			p.mset(kvPairs);
			for (int i = 0; i < kvPairList.size();) {
				p.expire(kvPairList.get(i), expireTime);
				i = i + 2;
			}
			p.sync();
		} catch (Exception e) {
			success = false;
			if (je != null)
				jp.returnBrokenResource(je);
			String error = getLogInfo(jp.getCurrentHostMaster(),
					kvPairList.toString(), "");
			throw new Exception(error, e);
		} finally {
			if (success && je != null) {
				jp.returnResource(je);
			}
		}

		return SUCCESS_RESPONSE;
	}

	public long del(final String key) throws Exception {
		JedisSentinelPool jp = redisNode.getSentinelPool();

		Jedis je = null;

		boolean success = true;
		try {
			je = jp.getResource();
			return je.del(key);
		} catch (Exception e) {
			success = false;
			if (je != null)
				jp.returnBrokenResource(je);
			String error = getLogInfo(jp.getCurrentHostMaster(), key, "");
			throw new Exception(error, e);
		} finally {
			if (success && je != null) {
				jp.returnResource(je);
			}
		}
	}

	public Long del(final String[] keys) throws Exception {
		long successCount = 0;

		JedisSentinelPool jp = redisNode.getSentinelPool();

		Jedis je = null;
		boolean success = true;

		try {
			je = jp.getResource();
			successCount += je.del(keys);
		} catch (Exception e) {
			success = false;
			if (je != null)
				jp.returnBrokenResource(je);
			String error = getLogInfo(jp.getCurrentHostMaster(), keys[0], "");
			throw new Exception(error, e);
		} finally {
			if (success && je != null) {
				jp.returnResource(je);
			}
		}
		return successCount;
	}
	
	public long expire(final String key, int seconds) throws Exception {
		JedisSentinelPool jp = redisNode.getSentinelPool();

		Jedis je = null;

		boolean success = true;
		try {
			je = jp.getResource();
			return je.expire(key, seconds);
		} catch (Exception e) {
			success = false;
			if (je != null)
				jp.returnBrokenResource(je);
            String error = getLogInfo(jp.getCurrentHostMaster(), key, "");
            throw new Exception(error, e);
		} finally {
			if (success && je != null) {
				jp.returnResource(je);
			}
		}
	}

	private String getLogInfo(HostAndPort hostAndPort, String key, String desc) {
		StringBuilder builder = new StringBuilder(desc);
		builder.append(" instance = ");
		builder.append(hostAndPort.toString());
		builder.append(", Key = ");
		builder.append(key);
		return builder.toString();
	}

}
