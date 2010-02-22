#!/bin/sh

echo "Building WebLech..."

echo "Building Spider..."
javac -classpath lib/log4j-1.1.3.jar:classes -d classes src/weblech/spider/*.java src/weblech/ui/*.java src/weblech/util/*.java

echo "Spider built."

