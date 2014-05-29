#!/bin/bash
# Packs all dependencies into one jar and uploads it to HDFS
# Prerequisite: sbt assembly plugin
# TODO: turn this into an sbt plugin some day

tmpdir=target/dependencies-$RANDOM
project_name=$( grep "^ *name *:=" build.sbt | sed 's/^[^"]*"\([^"]*\)".*$/\1/' )
assembly_jar=$( readlink -m target/${project_name}-assembly.jar )
output_jar=target/${project_name}-dependencies.jar
hdfs_jar=jars/${project_name}-dependencies.jar

function do_assemble {
  echo "Building dependency jar at $output_jar..."
  mkdir -vp $tmpdir
  echo "Packaging scala library"
  sbt assembly-package-scala
  ( cd $tmpdir && jar -xf $assembly_jar )
  echo "Packaging dependencies"
  sbt assembly-package-dependency
  ( cd $tmpdir && jar -xf $assembly_jar )
  echo "Repacking jar"
  jar -cf $output_jar -C $tmpdir .
  rm -rf $tmpdir
  echo "Created $output_jar"
}

function upload { hadoop fs -put $output_jar $hdfs_jar && echo "$output_jar -> $hdfs_jar"; }
function do_upload {
  echo "Uploading dependency jar to HDFS..."
  upload || hadoop fs -rm $hdfs_jar && upload
}


function do_all {
  do_assemble
  do_upload
}

case "$1" in
  pack)
    do_assemble
    ;;
  upload)
    do_upload
    ;;
  nothing)
    ;;
  *)
    do_all
    ;;
esac

