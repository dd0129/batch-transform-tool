package com.yili.wormhole.common;

import com.yili.wormhole.common.interfaces.IParam;
import com.yili.wormhole.common.interfaces.ISplitter;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSplitter implements ISplitter{
	
	protected IParam param;
	
	@Override
	public void init(IParam jobParams){
		param = jobParams;
	}
	
	@Override
	public List<IParam> split(){
		List<IParam> result = new ArrayList<IParam>();
		result.add(param);
		return result;
	}


}
