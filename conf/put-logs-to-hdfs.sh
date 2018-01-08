hdfs dfs -mkdir /nasa_logs

hdfs dfs -put NASA_access_log_Aug95 /nasa_logs
hdfs dfs -put NASA_access_log_Jul95 /nasa_logs

echo "NASA_access_log_Aug95 and NASA_access_log_Jul95 were put under '/nasa_logs' hdfs dir"
