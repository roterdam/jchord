@echo off

echo Building WebLech...

echo Building Spider...
javac -classpath lib/log4j-1.1.3.jar -d classes src/weblech/spider/*.java src/weblech/util/*.java src/weblech/ui/*

echo Spider built.

