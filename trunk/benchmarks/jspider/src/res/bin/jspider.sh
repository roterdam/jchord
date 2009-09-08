#!/bin/sh
# $Id: jspider.sh,v 1.9 2003/04/22 16:43:33 vanrogu Exp $
echo ------------------------------------------------------------
echo JSpider startup script

if [ ! -d "$JSPIDER_HOME" ]; then
  echo JSPIDER_HOME does not exist as a valid directory : $JSPIDER_HOME
  echo Defaulting to current directory
  JSPIDER_HOME=..
fi

echo JSPIDER_HOME=$JSPIDER_HOME
echo ------------------------------------------------------------

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

java -cp $JSPIDER_CLASSPATH:$CLASSPATH $JSPIDER_OPTS net.javacoding.jspider.JSpider $1 $2
