package com.yili.wormhole.plugins.common;

import com.yili.wormhole.common.interfaces.ILineReceiver;
import com.yili.wormhole.common.interfaces.IPluginMonitor;

import java.sql.SQLException;

public class DBResultSetReceiver {

	//private static final Log s_logger = LogFactory.getLog(DBResultSetReceiver.class);
	
	private ILineReceiver receiver;

	private int columnCount;
	
	private IPluginMonitor monitor;

	public static DBResultSetReceiver newReceiver(ILineReceiver receiver) {
		return new DBResultSetReceiver(receiver);
	}
	
	public DBResultSetReceiver(ILineReceiver receiver) {
		this.receiver = receiver;
	}

	public ILineReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(ILineReceiver receiver) {
		this.receiver = receiver;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	public IPluginMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(IPluginMonitor monitor) {
		this.monitor = monitor;
	}
	
	public void getFromReader() throws SQLException{
		
	}
	
	
}
