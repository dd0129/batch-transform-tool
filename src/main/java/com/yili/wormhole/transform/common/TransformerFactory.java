package com.yili.wormhole.transform.common;

import com.yili.wormhole.common.interfaces.ITransformer;
import com.yili.wormhole.engine.utils.JarLoader;
import com.yili.wormhole.engine.utils.ReflectionUtil;


public class TransformerFactory {
	
	public static final String JAR_PATH = "transformers/";
	public static ITransformer create(String name){
		ITransformer result = ReflectionUtil.createInstanceByDefaultConstructor(
				name, 
				ITransformer.class,
				JarLoader.getInstance(JAR_PATH));
		return result;
	}
}
