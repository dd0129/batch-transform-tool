package com.yili.wormhole.engine.core;

import com.yili.wormhole.common.interfaces.IParam;
import com.yili.wormhole.common.interfaces.IReaderPeriphery;
import com.yili.wormhole.common.interfaces.ISourceCounter;
import com.yili.wormhole.common.interfaces.ITargetCounter;

class DefaultReaderPeriphery implements IReaderPeriphery{

	@Override
	public void prepare(IParam param, ISourceCounter counter) {
		//do nothing
	}

	@Override
	public void doPost(IParam param, ITargetCounter counter) {
		//do nothing
	}

}
