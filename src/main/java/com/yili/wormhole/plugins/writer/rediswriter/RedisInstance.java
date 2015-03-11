package com.yili.wormhole.plugins.writer.rediswriter;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.JedisPool;

public class RedisInstance {
	private static final String LOCALHOST_STR = "localhost";
	private static final int DEFAULT_TIMEOUT = 0;
	
	private String host;
	private int port;
	private JedisPool jp;
	private int weight;

	public int getWeight() {
		return weight;
	}

	public RedisInstance(String host, int port, int timeout,
			GenericObjectPoolConfig conf) {
		this(host, port, timeout, conf, DEFAULT_TIMEOUT);
	}

	public RedisInstance(String host, int port, int timeout,
			GenericObjectPoolConfig conf, int weight) {
		this.host = host;
		this.port = port;
		this.jp = new JedisPool(conf, host, port, timeout);
		this.weight = weight;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RedisInstance) {
			RedisInstance hp = (RedisInstance) obj;
			String thisHost = convertHost(host);
			String hpHost = convertHost(hp.host);
			return port == hp.port && thisHost.equals(hpHost);
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + host + ":" + port + ":" + weight + "]";
	}

	private String convertHost(String host) {
		if (host.equals("127.0.0.1"))
			return LOCALHOST_STR;
		else if (host.equals("::1"))
			return LOCALHOST_STR;

		return host;
	}

	public JedisPool getJp() {
		return jp;
	}

	public void destroy() {
		jp.destroy();
	}

}
