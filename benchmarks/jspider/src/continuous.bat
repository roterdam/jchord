@ECHO OFF
REM
REM Startup script for continuous integration that should be scheduled.
REM
REM $Id: continuous.bat,v 1.1 2003/02/02 13:25:59 vanrogu Exp $
REM
ant -buildfile continuous.xml -logger org.apache.tools.ant.listener.MailLogger
ant -buildfile continuous.xml cleanFolders
