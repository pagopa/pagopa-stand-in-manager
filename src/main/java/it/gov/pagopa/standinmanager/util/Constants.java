package it.gov.pagopa.standinmanager.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String HEADER_REQUEST_ID = "X-Request-Id";

  public static final String EVENT_FORWARDER_CALL = "FORWARDER_CALL";
  public static final String EVENT_FORWARDER_CALL_RESP_SUCCCESS = "FORWARDER_CALL_RESP_SUCCESS";
  public static final String EVENT_FORWARDER_CALL_RESP_ERROR = "FORWARDER_CALL_RESP_ERROR";
  public static final String EVENT_ADD_TO_STANDIN = "ADD_TO_STANDIN";
  public static final String EVENT_REMOVE_FROM_STANDIN = "REMOVE_FROM_STANDIN";
}
