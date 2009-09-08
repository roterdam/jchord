#!/bin/sh
# $Id: jspider-tool.sh,v 1.6 2003/04/22 16:43:33 vanrogu Exp $

if [ ! -d "$JSPIDER_HOME" ]; then
  JSPIDER_HOME=..
fi

export JSPIDER_OPTS=
export JSPIDER_OPTS="$JSPIDER_OPTS -Djspider.home=$JSPIDER_HOME"
export JSPIDER_OPTS="$JSPIDER_OPTS -Djava.util.logging.config.file=$JSPIDER_HOME/common/conf/logging/logging.properties"
export JSPIDER_OPTS="$JSPIDER_OPTS -Dlog4j.configuration=conf/logging/log4j.xml"

export JSPIDER_CLASSPATH=
export JSPIDER_CLASSPATH="$JSPIDER_HOME/lib/jspider.jar"
export JSPIDER_CLASSPATH="$JSPIDER_CLASSPATH:$JSPIDER_HOME/lib/velocity-dep-1.3.1.jar"
export JSPIDER_CLASSPATH="$JSPIDER_CLASSPATH:$JSPIDER_HOME/lib/commons-logging.jar"
export JSPIDER_CLASSPATH="$JSPIDER_CLASSPATH:$JSPIDER_HOME/lib/log4j-1.2.8.jar"
export JSPIDER_CLASSPATH="$JSPIDER_CLASSPATH:$JSPIDER_HOME/common"
export JSPIDER_CLASSPATH="$JSPIDER_CLASSPATH:$CLASSPATH"

java -cp $JSPIDER_CLASSPATH:$CLASSPATH $JSPIDER_OPTS net.javacoding.jspider.JSpiderTool $1 $2 $3 $4 $5 $6 $7 $8 $9
