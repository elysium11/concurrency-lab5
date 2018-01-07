hdfs dfs -mkdir /nasa_logs

hdfs dfs -put NASA_access_log_Aug95 /nasa_logs
hdfs dfs -put NASA_access_log_Jul95 /nasa_logs

echo "access_log_Aug95 and access_log_Jul95 were put under '/nasa_logs' hdfs dir"
