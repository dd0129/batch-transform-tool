package com.yili.wormhole.common;


import com.yili.wormhole.common.interfaces.ILineReceiver;
import com.yili.wormhole.common.interfaces.ILineSender;
import com.yili.wormhole.engine.storage.IStorage;

import java.util.List;

public class LineExchangerFactory {
	
	public static ILineSender createNewLineSender(IStorage storageForRead, List<IStorage> storageForWrite){
	
		return new BufferedLineExchanger(storageForRead, storageForWrite);
	}
	
	public static ILineReceiver createNewLineReceiver(IStorage storageForRead, List<IStorage> storageForWrite){

		return new BufferedLineExchanger(storageForRead, storageForWrite);
	}

}
