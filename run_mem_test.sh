#!/bin/bash
times=3
file_suffix=$(date +'%Y_%m_%d_%H_%M_%S')

cd test
for n in $(seq 1 1 8)
do
    javac test$n/Test.java

    for tool in "VFT" "VB"
    do
        mkdir -p ../temp_log/test$n

        for i in $(seq 1 1 $times)
        do
            echo "------------ Run: $tool - test$n - $i "
            rrrun -quiet -tool=$tool test$n/Test
            
            # sleep 1 second between each run
            sleep 1

            # move snapshot file
            mv jp_snapshot.0.jps ../temp_log/test$n/$tool\_$file_suffix\_$i.jps
            mv jp_snapshot.0.hprof ../temp_log/test$n/$tool\_$file_suffix\_$i.hprof
        done

        # sleep 2 seconds between each tool
        sleep 2
    done

    # sleep 2 seconds between each test
    sleep 2
done
cd ..