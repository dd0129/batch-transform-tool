package com.yili.wormhole.plugins.reader.hivereader;

public enum HiveReaderMode {
	READ_FROM_HIVESERVER ("READ_FROM_HIVESERVER"),
	READ_FROM_HDFS ("READ_FROM_HDFS"),

	READ_FROM_LOCAL("READ_FROM_LOCAL"); // 新加READ模式 hive -e 方式
	
	private String mode = null;
	private HiveReaderMode(String mode) {
		this.mode = mode;
	}
	String getMode(){
		return mode;
	}
}
