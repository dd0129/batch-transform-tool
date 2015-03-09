package com.yili.wormhole.plugins.reader.salesforcereader;

import com.sforce.soap.partner.Field;

import java.util.Map;

public class DescribeRefObject {

	private String objectName;
	private Map<String, Field> fieldInfoMap;

	DescribeRefObject(String objectName, Map<String, Field> fieldInfoMap) {
		this.objectName = objectName;
		this.fieldInfoMap = fieldInfoMap;
	}

	public Map<String, Field> getFieldInfoMap() {
		return fieldInfoMap;
	}

	public String getObjectName() {
		return objectName;
	}
}
