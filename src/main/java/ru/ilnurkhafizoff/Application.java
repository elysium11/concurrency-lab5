package ru.ilnurkhafizoff;

import static java.time.ZoneOffset.UTC;
import static org.apache.spark.api.java.JavaSparkContext.toSparkContext;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.window;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serializable;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import scala.Tuple3;

public class Application implements Serializable {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss");
  private static final SimpleDateFormat SIMPLE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  private static final int HOST_INDEX = 0;
  private static final int DATE_INDEX = 3;
  private static final int HTTP_METHOD_INDEX = 5;
  private static final int ENDPOINT_INDEX = 6;

  private static Function<String, Record> toRecord = s -> {
    String[] splitted = s.split(" ");

    try {
      String host = splitted[HOST_INDEX];
      LocalDateTime date = LocalDateTime
          .parse(splitted[DATE_INDEX].substring(1), DATE_TIME_FORMATTER);
      String method = splitted[HTTP_METHOD_INDEX].substring(1);
      String endpoint = removeLastQuoteIfExists(splitted[ENDPOINT_INDEX]).orElse("");
      String responseStatus = splitted[responseCodeIndex(splitted)];

      return new Record(
          host,
          new Date(date.atZone(UTC).toInstant().toEpochMilli()),
          method,
          endpoint,
          Integer.valueOf(responseStatus)
      );
    } catch (Exception e) {
      System.err.println("Bad record: " + s);
    }

    return null;
  };

  private static int responseCodeIndex(String[] splitted) {
    return splitted.length - 2;
  }

  private static Optional<String> removeLastQuoteIfExists(String string) {
    if (string != null && string.length() > 0) {
      if (string.charAt(string.length() - 1) == '"') {
        return Optional.of(string.substring(0, string.length() - 1));
      } else {
        return Optional.of(string);
      }
    }
    return Optional.empty();
  }

  public static void main(String[] args) throws JsonProcessingException {
    if (args.length < 2) {
      throw new IllegalArgumentException("Expected hdfs host name and logs dir path");
    }

    new Application().run(args[0], args[1]);
  }

  private void run(String hdfsHost, String nasaLogsDir) {
    SparkConf sparkConf = new SparkConf();

    JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);

    JavaRDD<Record> records = javaSparkContext
        .textFile("hdfs://" + hdfsHost + ":9000" + nasaLogsDir + "/NASA_*")
        .map(toRecord)
        .filter(Objects::nonNull);

    // 1. 5xx calls

    records
        .filter(r -> r.getResponseStatus() >= 500 && r.getResponseStatus() < 600)
        .groupBy(r -> r.getHost() + r.getEndpoint())
        .mapValues(i -> i.spliterator().getExactSizeIfKnown())
        .saveAsTextFile("hdfs://" + hdfsHost + ":9000/task1");

    // 2. Time series

    records
        .groupBy(r -> r.getDate().toLocalDate().format(DateTimeFormatter.ISO_DATE))
        .mapValues(this::countHttpMethodAndStatusPairs)
        .mapValues(m -> removeWhereCountLessThan(m, 10))
        .saveAsTextFile("hdfs://" + hdfsHost + ":9000/task2");

    // 3. 4xx and 5xx with week window

    SparkSession session = SparkSession.builder().
        sparkContext(toSparkContext(javaSparkContext))
        .getOrCreate();

    Dataset<Row> recordsDataSet = session.createDataFrame(records, Record.class);

    recordsDataSet
        .filter("responseStatus >= 400")
        .filter("responseStatus < 600")
        .groupBy(window(recordsDataSet.col("date"), "1 week"))
        .agg(count("responseStatus").as("status_count"))
        .select("window.start", "window.end", "status_count")
        .javaRDD()
        .map(r -> new Tuple3<>(
            SIMPLE_FORMATTER.format(r.getTimestamp(0)),
            SIMPLE_FORMATTER.format(r.getTimestamp(1)),
            r.getLong(2)))
        .saveAsTextFile("hdfs://" + hdfsHost + ":9000/task3");
  }


  private Map<Tuple2<String, Integer>, Long> countHttpMethodAndStatusPairs(Iterable<Record> records) {

    Map<Tuple2<String, Integer>, Long> result = new HashMap<>();

    for (Record r : records) {
      Tuple2<String, Integer> methodAndStatus = new Tuple2<>(r.getMethod(), r.getResponseStatus());
      result.putIfAbsent(methodAndStatus, 0L);
      result.compute(methodAndStatus, (t, c) -> c + 1);
    }

    return result;
  }

  private <K, V extends Long> Map<K, V> removeWhereCountLessThan(Map<K, V> map, long count) {
    HashMap<K, V> copyMap = new HashMap<>(map);
    copyMap.entrySet().removeIf(e -> e.getValue().longValue() < count);
    return copyMap;
  }
}
