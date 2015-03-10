#!/bin/sh

source /etc/profile
#export LD_LIBRARY_PATH=/usr/local/hadoop/hadoop-release/lib/native/Linux-amd64-64:/usr/local/hadoop/lzo/lib
export LD_LIBRARY_PATH=/usr/local/hadoop/hadoop-release/lib/native:/usr/local/hadoop/lzo/lib
CURR_DIR=`pwd`
cd `dirname "$0"`/..
WORMHOLE_HOME=`pwd`
export WORMHOLE_CONNECT_FILE="${WORMHOLE_HOME}/conf/connect.props"

#set JAVA_OPTS
JAVA_OPTS=" -Xms1024m -Xmx4048m -Xmn256m -Xss2048k"

#performance Options
#JAVA_OPTS="$JAVA_OPTS -XX:+AggressiveOpts"
#JAVA_OPTS="$JAVA_OPTS -XX:+UseBiasedLocking"
#JAVA_OPTS="$JAVA_OPTS -XX:+UseFastAccessorMethods"
#JAVA_OPTS="$JAVA_OPTS -XX:+DisableExplicitGC"
#JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC"
#JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC"
#JAVA_OPTS="$JAVA_OPTS -XX:+CMSParallelRemarkEnabled"
#JAVA_OPTS="$JAVA_OPTS -XX:+UseCMSCompactAtFullCollection"
#JAVA_OPTS="$JAVA_OPTS -XX:+UseCMSInitiatingOccupancyOnly"
#JAVA_OPTS="$JAVA_OPTS -XX:CMSInitiatingOccupancyFraction=75"
#JAVA_OPTS="$JAVA_OPTS -XX:LargePageSizeInBytes=128m"

#log print Options
#JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCApplicationStoppedTime"
#JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCTimeStamps"
#JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails"
#==========================================================================

#start
RUN_CMD="/usr/local/jdk/bin/java -classpath \"${WORMHOLE_HOME}/conf/:${WORMHOLE_HOME}/lib/*\""
RUN_CMD="$RUN_CMD $JAVA_OPTS"
RUN_CMD="$RUN_CMD com.yili.wormhole.engine.core.Engine $@"
echo $RUN_CMD
eval $RUN_CMD
#==========================================================================
