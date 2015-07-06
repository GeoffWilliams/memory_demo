#!/bin/bash
rm -rf results
mkdir results
cd results

function run_with_args() {
  TEST_NAME=$1
  TEST_ARGS=$2
  TEST_LENGTH=$3

  echo "Running tests ${TEST_NAME} for ${TEST_LENGTH}"

  mkdir $TEST_NAME
  cd $TEST_NAME 
 
  for GC in ' ' '-XX:+UseSerialGC' '-XX:+UseParallelGC' '-XX:+UseConcMarkSweepGC' '-XX:+UseG1GC' ; do
    java -cp ../../build/classes $GC -Xmx7g memory_demo.MemoryDemo $TEST_ARGS > /dev/null &
    echo $! >> pids.txt
  done
  sleep $TEST_LENGTH
  while read pid
  do
    echo killing $pid
    kill $pid
  done < pids.txt
  rm pids.txt

  for FILE in *memlog.txt ; do
    OUT_FILE=$(basename -s .txt $FILE)
    csv2svg_graph.py --input_file $FILE --output_file $OUT_FILE.svg 
  done

  cd ..
  echo "---------- completed -----------"
}

run_with_args "normal" " " "3h"
run_with_args "gc_once" "--gc_once" "2h"
run_with_args "manual_gc" "--manual_gc" "1h"
run_with_args "object_activity" "--object_activity" "2h"
run_with_args "manual_gc_and_activity" "--manual_gc --:object_activity" "1h"
run_with_args "gc_once_and_activity" "--gc_once --object_activity" "1h"
