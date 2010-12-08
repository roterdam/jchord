#!/bin/bash

#
#  Modify the line below to point to your HADOOP_HOME.
#
HADOOP_HOME=/Users/asrabkin/workspace/hadoop-0.20.2

ant run -Dchord.work.dir=`pwd`/examples/hadoop20 -DhadoopHome=$HADOOP_HOME -Ddictionary.name=hdfs.dict -Dchord.main.class=org.apache.hadoop.hdfs.server.datanode.DataNode -Dentrypoints.file=examples/hadoop20/entrypoints-20-hdfs.txt

ant run -Dchord.work.dir=`pwd`/examples/hadoop20 -DhadoopHome=$HADOOP_HOME -Ddictionary.name=mapred.dict -Dchord.main.class=org.apache.hadoop.mapred.TaskTracker -Dentrypoints.file=examples/hadoop20/entrypoints-20-mapred.txt
