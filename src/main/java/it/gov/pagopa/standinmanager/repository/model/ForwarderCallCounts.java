package it.gov.pagopa.standinmanager.repository.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwarderCallCounts {
  private String id;
  private String station;
  private Instant timestamp;
  private Boolean outcome;

  public String getPartitionKey() {
    return timestamp.toString().substring(0, 10);
  }
}
