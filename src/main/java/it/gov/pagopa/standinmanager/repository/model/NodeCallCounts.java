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
public class NodeCallCounts {
  private String id;
  private String station;
  private Instant timestamp;
  private Integer total;
  private Integer faults;

  public double getPerc() {
    return ((getFaults() / (double) getTotal()) * 100);
  }
}
