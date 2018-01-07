#!/bin/bash

N=${1:-1}

docker rm -f hadoop-master &> /dev/null

echo "start hadoop-master container..."
docker run -itd --net=hadoop \
                -p 50070:50070 \
                -p 8088:8088 \
                -p 8080:8080 \
                --name hadoop-master \
                --hostname hadoop-master \
                elysium11/concurrency-lab5


i=0
while [ $i -lt $N ]
do
  docker rm -f hadoop-slave$i &> /dev/null
  echo "start hadoop-slave$i container..."
  docker run -itd --net=hadoop \
                  --name hadoop-slave$i \
                  --hostname hadoop-slave$i \
                  elysium11/concurrency-lab5
  i=$(( $i + 1 ))
done

docker exec -it hadoop-master bash