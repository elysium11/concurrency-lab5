package ru.ilnurkhafizoff;

import java.io.Serializable;
import java.sql.Date;

public class Record implements Serializable {

  private String host;
  private Date date;
  private String method;
  private String endpoint;
  private int responseStatus;

  public Record() {
  }

  public Record(String host, Date date, String method, String endpoint, int responseStatus) {
    this.host = host;
    this.date = date;
    this.method = method;
    this.endpoint = endpoint;
    this.responseStatus = responseStatus;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public int getResponseStatus() {
    return responseStatus;
  }

  public void setResponseStatus(int responseStatus) {
    this.responseStatus = responseStatus;
  }

  @Override
  public String toString() {
    return "Record{" +
        "host='" + host + '\'' +
        ", date=" + date +
        ", method=" + method +
        ", endpoint='" + endpoint + '\'' +
        ", reponseStatus='" + responseStatus + '\'' +
        '}';
  }
}
