#!/bin/sh
echo "Running Spider..."
java -classpath classes:lib/log4j-1.1.3.jar weblech.ui.TextSpider config/Spider.properties
