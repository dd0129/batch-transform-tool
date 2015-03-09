package com.yili.wormhole.engine.core;

import com.yili.wormhole.common.interfaces.IParam;
import com.yili.wormhole.common.interfaces.ISourceCounter;
import com.yili.wormhole.common.interfaces.ITargetCounter;
import com.yili.wormhole.common.interfaces.IWriterPeriphery;

class DefaultWriterPeriphery implements IWriterPeriphery {

	@Override
	public void prepare(IParam param, ISourceCounter counter) {
		// do nothing
	}

	@Override
	public void doPost(IParam param, ITargetCounter counter, int i) {
		// do nothing
		
	}

	@Override
	public void rollback(IParam param) {
		// do nothing
	}

}
