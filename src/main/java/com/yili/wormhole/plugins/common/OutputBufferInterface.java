package com.yili.wormhole.plugins.common;

public interface OutputBufferInterface {

	public void put(String key, String value);
	
	public String toString();
	
	public void clear();
}
