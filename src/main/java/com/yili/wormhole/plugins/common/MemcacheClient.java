package com.yili.wormhole.plugins.common;

import com.dianping.avatar.cache.CacheKey;
import com.dianping.avatar.cache.CacheService;
import com.dp.nebula.wormhole.common.utils.Environment;
import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public final class MemcacheClient {
	private final static Logger LOG = Logger.getLogger(MemcacheClient.class);

	private String category;

	// memcache service
	private static CacheService cacheService = null;
	
	static {
		// get avatar bean
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(Environment.SERVICE_CONF);
		cacheService = (CacheService) context
				.getBean("cacheService");
				
		LOG.info(Environment.SERVICE_CONF);
	}
	
	public MemcacheClient(String category) {
		this.category = new String(category.replace('.', '#').replace('_', '-'));
	}

	public void set(String key, String value) throws IOException {
		// write memcache
		CacheKey cacheKey = new CacheKey(category, key);
		cacheService.add(cacheKey, value);
	}

	public void close() throws IOException {
	}

	public void flush() throws IOException {
	}
}