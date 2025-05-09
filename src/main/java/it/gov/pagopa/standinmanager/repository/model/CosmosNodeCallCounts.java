package it.gov.pagopa.standinmanager.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class CosmosNodeCallCounts {
    private String id;
    private String station;
    private Instant timestamp;
    private Integer total;
    private Integer faults;
    private Integer allStationCallInSlot;

    @JsonProperty("PartitionKey")
    public String getPartitionKey() {
        return timestamp.toString().substring(0, 10);
    }

    @JsonIgnore
    public double getFaultPercentage() {
        return ((getFaults() / (double) getTotal()) * 100);
    }

    @JsonIgnore
    public double getTotalTrafficPercentage() {
        return ((getTotal() / (double) getAllStationCallInSlot()) * 100);
    }
}
