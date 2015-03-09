package com.yili.wormhole.plugins.writer.mongowriter;

import com.dp.nebula.wormhole.common.AbstractSplitter;
import com.dp.nebula.wormhole.common.interfaces.IParam;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MongoWriterSplitter extends AbstractSplitter {
	private final static Logger log = Logger.getLogger(MongoWriterSplitter.class);
	
	@Override
	public void init(IParam jobParams){
		param = jobParams;
	}
	
	@Override
	public List<IParam> split(){
		List<IParam> result = new ArrayList<IParam>();
		int concurrency = param.getIntValue(ParamKey.concurrency);
		for (int i = 0; i < concurrency; i++){
			IParam p = param.clone();
			result.add(p);
		}
		log.info("the number of split: " + result.size());
		return result;
	}
}
