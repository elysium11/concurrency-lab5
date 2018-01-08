# Содержание

1. [Код][1]
2. [Конфигурационные файлы][2] для хадупа и спарка
3. [Результаты выполнения][3]

# Порядок запуска для проверки
## Общая часть
* Создать сеть с именем hadoop: `docker network create --driver bridge hadoop`
* Можно собрать docker image локально c помощью скрипта: `./docker-build.sh`, но перед этим нужно будет собрать jar с программой: `mvn clean package`
* Поднять контейнеры с помощью скрипта `./start-cluster.sh`. Скрипт поднимет 2 контейнера `hadoop-master` и `hadoop-slave0`. В конце скрипта выполняется подключение к контейнеру `hadoop-master`, т.е. все последующие команды будут исполняться внутри контейнера `hadoop-master`.
## Запуск поверх Yarn'a
* Поднять демоны Hadoop HDFS и Hadoop YARN: `start-dfs.sh && start-yarn.sh`. 
* Положить логи на hdfs: `./put-logs-to-hdfs.sh`. Скрипт положит логи на hdfs по следующему пути: `/nasa_logs`.
* Выполнить запуск программы `concurrency-lab5-1.0.jar`, передав в качестве аргументов имя хоста, на котором запущен демон Namenode и путь до директории с логами на hdfs:
```shell
spark-submit --class ru.ilnurkhafizoff.Application\
             --master yarn \
             --deploy-mode client \
             --executor-memory 1G \
             --num-executors 3 \
             concurrency-lab5-1.0.jar hadoop-master /nasa_logs
```
* После выполнения, результаты можно получить выполнив следующие команды: 
```shell
hdfs dfs -getmerge /task1 task1.txt && \
hdfs dfs -getmerge /task2 task2.txt && \
hdfs dfs -getmerge /task3 task3.txt
```
## Запуск в режиме Standalone
* Если перед этим выполнялся запуск поверх Yarn'a, нужно удалить результаты выполнения программы с hdfs: `hdfs dfs -rm -r /task*`. Также можно остановить демоны Yarn'a: `stop-yarn.sh`
* Поднять демоны Hadoop HDFS: `start-dfs.sh`
* Если требуется, положить логи на hdfs: `./put-logs-to-hdfs.sh`. Скрипт положит логи на hdfs по следующему пути: `/nasa_logs`. 
* Поднять демоны Spark'а: `start-master.sh && start-slaves.sh`
* Выполнить запуск программы передав в качестве аргументов имя хоста, на котором запущен Namenode и путь до директории с логами на hdfs:
```shell
spark-submit --total-executor-cores 8 \
             --class ru.ilnurkhafizoff.Application \
             --master spark://hadoop-master:7077 \
             concurrency-lab5-1.0.jar hadoop-master /nasa_logs
```
* После выполнения, результаты можно получить выполнив следующие команды: 
```shell
hdfs dfs -getmerge /task1 task1.txt && \
hdfs dfs -getmerge /task2 task2.txt && \
hdfs dfs -getmerge /task3 task3.txt
```

[1]: https://github.com/elysium11/concurrency-lab5/tree/master/src/main/java/ru/ilnurkhafizoff
[2]: https://github.com/elysium11/concurrency-lab5/tree/master/conf
[3]: https://github.com/elysium11/concurrency-lab5/tree/master/results