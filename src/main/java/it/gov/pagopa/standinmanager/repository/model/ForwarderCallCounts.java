package it.gov.pagopa.standinmanager.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwarderCallCounts {
  private String id;
  private String station;
  private Instant timestamp;
  private Boolean outcome;
}
