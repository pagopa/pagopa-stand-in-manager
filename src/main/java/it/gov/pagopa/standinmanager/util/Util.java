package it.gov.pagopa.standinmanager.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Util {

  public static String format(LocalDate d) {
    return d.format(DateTimeFormatter.ISO_DATE);
  }

  /**
   * @param value value to deNullify.
   * @return return empty string if value is null
   */
  public static String deNull(String value) {
    return Optional.ofNullable(value).orElse("");
  }

  /**
   * @param value value to deNullify.
   * @return return empty string if value is null
   */
  public static String deNull(Object value) {
    return Optional.ofNullable(value).orElse("").toString();
  }

  /**
   * @param value value to deNullify.
   * @return return false if value is null
   */
  public static Boolean deNull(Boolean value) {
    return Optional.ofNullable(value).orElse(false);
  }

}
