package com.yili.wormhole.engine.storage;

import com.yili.wormhole.common.interfaces.ILine;

import java.util.concurrent.TimeUnit;

public abstract class StorageQueue implements java.io.Serializable{

	private static final long serialVersionUID = -7334864414523350826L;

	public abstract boolean push(ILine line, long timeout, TimeUnit unit) throws InterruptedException;

	public abstract boolean push(ILine[] lines, int size, long timeout, TimeUnit unit) throws InterruptedException;

	public abstract ILine pull(long timeout, TimeUnit unit) throws InterruptedException;

	public abstract int pull(ILine[] ea, long timeout, TimeUnit unit) throws InterruptedException ;

	public abstract void close();
	
	public abstract int size();
	
	public abstract int getLineLimit();
	
	public abstract String info();
}
