#!/bin/bash
# Runs a subclass of com.twitter.scalding.Job locally or on the cluster
# Arguments: <fully-qualified job class> [--local or --hdfs] [any arguments expected by the Job's Args]
source ./upload_dependencies.sh nothing

USER=${USER:-$( whoami )}
DEPJAR=target/${project_name}-dependencies.jar
HDFS_DEPJAR=hdfs:///user/$USER/jars/${project_name}-dependencies.jar
JOBJAR=target/scala-2.10/${project_name}-package.jar

[[ -f "$DEPJAR" ]] || do_assemble

if [[ "$1" == "skip-package" ]]; then
  shift
else
  sbt package
fi

HADOOP_CLASSPATH="$HADOOP_CLASSPATH:$DEPJAR" hadoop jar $JOBJAR -libjars $HDFS_DEPJAR -- $@
