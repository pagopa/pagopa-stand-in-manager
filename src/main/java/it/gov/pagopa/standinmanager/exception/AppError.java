package it.gov.pagopa.standinmanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppError {
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),
  NO_RESULT_FROM_DATA_EXPLORER_QUERY(
      HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected result from Data Explorer query", "Data Explorer query result was empty"),
  EVENT_HUB_PUBLISH_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "Failed to publish event to Event Hub", "Json processing error: %s");

  public final HttpStatus httpStatus;
  public final String title;
  public final String details;

  AppError(HttpStatus httpStatus, String title, String details) {
    this.httpStatus = httpStatus;
    this.title = title;
    this.details = details;
  }
}
