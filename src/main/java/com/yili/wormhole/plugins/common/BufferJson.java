package com.yili.wormhole.plugins.common;

import org.json.JSONException;
import org.json.JSONObject;

public class BufferJson implements OutputBufferInterface {
	private JSONObject jsonObject;
	
	public BufferJson(){
		jsonObject = new JSONObject();
	}
	
	@Override
	public void put(String key, String value) {
		try {
			JSONObject valueJson = new JSONObject(value); 
			jsonObject.put(key, valueJson);
		} catch (Exception e) {
			try {
				jsonObject.put(key, value);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public String toString(){
		return jsonObject.toString();
	}

	@Override
	public void clear() {
		jsonObject = new JSONObject();
	}

}
