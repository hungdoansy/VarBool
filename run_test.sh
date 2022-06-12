#!/bin/bash
times=500

cd test
for n in $(seq 1 1 8)
do
   javac test$n/Test.java

   for tool in "VFT" "VB"
   do
      mkdir -p ../temp_log/test$n
      touch "../temp_log/test$n/$tool.txt"

      for i in $(seq 1 1 $times)
      do
         echo "------------ Run: $tool - test$n - $i "
         rrrun -quiet -tool=$tool test$n/Test >> "../temp_log/test$n/$tool.txt"
         
         # sleep 1 second between each run
         sleep 1
      done

      grep -E '\[RR: Time = \d+\]' "../temp_log/test$n/$tool.txt" > "../temp_log/test$n/"$tool"_extracted.txt"

      # sleep 5 seconds between each tool
      sleep 5
   done

   # sleep 5 seconds between each test
   sleep 5
done
cd ..