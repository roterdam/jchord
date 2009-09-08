@echo off
rem  $Id: jspider-tool.bat,v 1.5 2003/04/22 16:43:33 vanrogu Exp $

if exist "%JSPIDER_HOME%" goto JSPIDER_HOME_DIR_OK
  set JSPIDER_HOME=..
:JSPIDER_HOME_DIR_OK

set JSPIDER_OPTS=
set JSPIDER_OPTS=%JSPIDER_OPTS% -Djspider.home=%JSPIDER_HOME%
set JSPIDER_OPTS=%JSPIDER_OPTS% -Djava.util.logging.config.file=%JSPIDER_HOME%/common/conf/logging/logging.properties
set JSPIDER_OPTS=%JSPIDER_OPTS% -Dlog4j.configuration=conf/logging/log4j.xml

set JSPIDER_CLASSPATH=
set JSPIDER_CLASSPATH=%JSPIDER_CLASSPATH%;%JSPIDER_HOME%/lib/jspider.jar
set JSPIDER_CLASSPATH=%JSPIDER_CLASSPATH%;%JSPIDER_HOME%/lib/velocity-dep-1.3.1.jar
set JSPIDER_CLASSPATH=%JSPIDER_CLASSPATH%;%JSPIDER_HOME%/lib/commons-logging.jar
set JSPIDER_CLASSPATH=%JSPIDER_CLASSPATH%;%JSPIDER_HOME%/lib/log4j-1.2.8.jar
set JSPIDER_CLASSPATH=%JSPIDER_CLASSPATH%;%JSPIDER_HOME%/common
set JSPIDER_CLASSPATH=%JSPIDER_CLASSPATH%;%CLASSPATH%

java -cp "%JSPIDER_CLASSPATH%;%classpath%" %JSPIDER_OPTS% net.javacoding.jspider.JSpiderTool %1 %2 %3 %4 %5 %6 %7 %8 %9
