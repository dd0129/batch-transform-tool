package com.yili.wormhole.plugins.writer.memcachewriter;

public final class ParamKey {
	/*
     * @name: table name
     * @description: redis table name
     * @range: 
     * @mandatory: true
     * @default:
     */
	public final static String table = "table";
	
	/*
     * @name: Faimly
     * @description: redis family name
     * @range: 
     * @mandatory: true
     * @default:
     */
	public final static String family = "family";

	/*
     * @name: rowKeyIndex
     * @description: specify the rowkey index number
     * @range: 
     * @mandatory: true
     * @default:
     */
	public final static String keyIndex = "keyIndex";
	/*
     * @name: columnsName
     * @description: split by comma, e.g."col1,col2"
     * @range: 
     * @mandatory: true
     * @default:
     */
	public final static String columnsName = "columnsName";
	
	/*
     * @name: serialize
     * @description: serialize type: 0(json) 1(string)
     * @range: 
     * @mandatory: true
     * @default:
     */
	public final static String serialize = "serialize";
	
	/*
     * @name: serialize separator
     * @description: valid only serialize is 1
     * @range: 
     * @mandatory: true
     * @default:
     */
	public final static String separator = "separator";
	
	 /*
	  * @name: concurrency
	  * @description: concurrency of the job 
	  * @range:1-10
	  * @mandatory: false
	  * @default:1
	  */
	public final static String concurrency = "concurrency";
	
	 /*
	  * @name: batch
	  * @description: set batch size
	  * @range:1-10
	  * @mandatory: false
	  * @default:1
	  */
	public final static String batchSize = "batchSize";
	
	/*
	  * @name: write_sleep
	  * @description: slow the speed of write 
	  * @range:
	  * @mandatory: false
	  * @default: false
	  */
	public final static String write_sleep = "write_sleep";
	/*
	  * @name: wait_time
	  * @description: wait_time of the write 
	  * @range:
	  * @mandatory: 
	  * @default:1000
	  */
	public final static String wait_time = "wait_time"; 
	/*
	  * @name: num_to_wait
	  * @description: num_to_wait of the write 
	  * @range:
	  * @mandatory: 
	  * @default:1000
	  */
	public final static String num_to_wait = "num_to_wait"; 
}
