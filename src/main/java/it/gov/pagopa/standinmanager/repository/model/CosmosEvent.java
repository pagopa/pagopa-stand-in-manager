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
public class CosmosEvent {
  private Instant timestamp;
  private String type;
  private String info;

  public String getPartitionKey() {
    return timestamp.toString().substring(0, 10);
  }
}
