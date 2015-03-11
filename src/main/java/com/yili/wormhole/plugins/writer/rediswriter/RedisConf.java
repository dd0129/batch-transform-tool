package com.yili.wormhole.plugins.writer.rediswriter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/*
 * 解析配置，获取RedisNode
 * */
public class RedisConf {		
	private int timeout = 50;
	
	public RedisConf() {
	}
	
	public RedisDataNode getDataNode(String sentinelList, String redisList, String name){
	    Set<String> sentinels = getSentinels(sentinelList);
	    RedisDataNode dataNode = processSingleDataNode(redisList, sentinels, name);
	    return dataNode;
	}
	
	private Set<String> getSentinels(String sentinelList) {
		Set<String> sentinels = new HashSet<String>();
		String[] sentinelString = sentinelList.split(";");
		for (String key : sentinelString) {
			String[] hostAndPort = key.split(":");
			sentinels.add(hostAndPort[0] + ":" + hostAndPort[1]);
		}
		return sentinels;
	}

    private RedisDataNode processSingleDataNode(String redisList, Set<String> sentinels, String name) {
        String[] instances = redisList.split(",");
        List<RedisInstance> instanceList = new CopyOnWriteArrayList<RedisInstance>();
        for(String instance : instances){
            String[] hostAndPort = instance.split(":");
            String host = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);
            int weight = hostAndPort.length == 3 ? Integer.parseInt(hostAndPort[2]) : 0;
            RedisInstance redisInstance = 
                    new RedisInstance(host, port, timeout,  getDefaultConf(), weight);
            instanceList.add(redisInstance);
        }
        RedisDataNode dataNode = new RedisDataNode(instanceList, sentinels, name);
        return dataNode;
    }

	private GenericObjectPoolConfig getDefaultConf() {
		GenericObjectPoolConfig conf = new GenericObjectPoolConfig();
		conf.setMaxWaitMillis(1000);
		conf.setMaxTotal(1000);
		conf.setMaxIdle(10);
		conf.setTestOnBorrow(false);
		conf.setTestOnReturn(false);
		conf.setTestWhileIdle(true);
		conf.setMinEvictableIdleTimeMillis(60000);
		conf.setTimeBetweenEvictionRunsMillis(30000);
		conf.setNumTestsPerEvictionRun(-1);
		return conf;
	}

}
