@echo off
rem  $Id: jspider.bat,v 1.8 2003/04/22 16:43:33 vanrogu Exp $
echo ------------------------------------------------------------
echo JSpider startup script

if exist "%JSPIDER_HOME%" goto JSPIDER_HOME_DIR_OK
  echo JSPIDER_HOME does not exist as a valid directory : %JSPIDER_HOME%
  echo Defaulting to current directory
  set JSPIDER_HOME=..

:JSPIDER_HOME_DIR_OK

echo JSPIDER_HOME=%JSPIDER_HOME%
echo ------------------------------------------------------------

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

java -cp "%JSPIDER_CLASSPATH%;%classpath%" %JSPIDER_OPTS% net.javacoding.jspider.JSpider %1 %2
