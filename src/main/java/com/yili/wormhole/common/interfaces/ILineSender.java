package com.yili.wormhole.common.interfaces;

public interface ILineSender {
	
	ILine createNewLine();
	
	Boolean send(ILine line);
	
	void flush();

}
