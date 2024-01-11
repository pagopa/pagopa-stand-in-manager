package it.gov.pagopa.standinmanager.repository.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmosStandInStation {
  private String id;
  private String station;
  private Instant timestamp;
  @JsonProperty("PartitionKey")
  public String getPartitionKey() {
    return timestamp.toString().substring(0, 10);
  }
}
