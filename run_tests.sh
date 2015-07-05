#!/bin/bash
cd build/classes
for GC in '-XX:+UseSerialGC' '-XX:+UseParallelGC' '-XX:+UseConcMarkSweepGC' '-XX:+UseG1GC' ; do
  java $GC -Xmx10g memory_demo.MemoryDemo &
done
