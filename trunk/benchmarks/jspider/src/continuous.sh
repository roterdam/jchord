#!/bin/sh
#
# Startup script for continuous integration that should be scheduled.
#
# $Id: continuous.sh,v 1.3 2003/02/02 14:11:09 vanrogu Exp $
#
ant -buildfile continuous.xml -logger org.apache.tools.ant.listener.MailLogger
ant -buildfile continuous.xml cleanFolders
