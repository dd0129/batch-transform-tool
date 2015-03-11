package com.yili.wormhole.plugins.writer.rediswriter;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.JedisSentinelPool;

public class RedisDataNode {
	//随机数最大值
	private static final int RANDOM_SIZE = 10000;
	
	//Redis DataNode（逻辑节点）名称
	private String name;
	
	//redis物理节点
	private List<RedisInstance> redisInstanceList;
	
	//sentinel pool
	private JedisSentinelPool sentinelPool;
	
	//该datanode包含的所有redisInstance weight之和
	private int maxWeight;
	
	public RedisDataNode(List<RedisInstance> redisInstanceList,
			Set<String> sentinels, String name) {
		this.redisInstanceList = redisInstanceList;
		this.sentinelPool = new JedisSentinelPool(name, sentinels);
		
		for (int i = 0; i < redisInstanceList.size(); i++) {
			maxWeight += redisInstanceList.get(i).getWeight();
		}

	}
	
    public String getName() {
		return name;
	}

	public void addInstance(RedisInstance redisInstance){
		redisInstanceList.add(redisInstance);
		maxWeight += redisInstance.getWeight();
	}
	
	public int getInstanceCount(){
		return redisInstanceList.size();
	}
	
	public RedisInstance getInstance(int i){
		return  redisInstanceList.get(i);
	}

	public JedisSentinelPool getSentinelPool() {
		return sentinelPool;
	}
	
	public String toString(){
		String ret = "RedisDataNode name : ["  +  this.name + "], instance size: [" + redisInstanceList.size() + "]" + ", Instance: ";
		for (int i = 0; i < redisInstanceList.size(); i++) {
			ret += redisInstanceList.get(i).toString();
		}
		return ret;
	}
	
	/**
	 * 销毁链接
	 */
	public void destroy(){
		for (int i = 0; i < redisInstanceList.size(); i++) {
			redisInstanceList.get(i).destroy();
		}
		sentinelPool.destroy();
	}
	
	public int getWorkInstance() {
		if(maxWeight == 0){
			return (int) (Math.random() * RANDOM_SIZE % redisInstanceList.size());
		}
		
		int rand = (int) (Math.random() * RANDOM_SIZE % maxWeight);
		int sum = 0;
		for (int i = 0; i < redisInstanceList.size(); i++) {
			sum += redisInstanceList.get(i).getWeight();
			if (rand < sum) {
				return i;
			}
		}
		
		return 0;
	}
    
}
