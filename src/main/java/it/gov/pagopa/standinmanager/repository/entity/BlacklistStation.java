package it.gov.pagopa.standinmanager.repository.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "STAND_IN_STATIONS_BLACKLIST")
@Data
public class BlacklistStation {
    @Id
    @Column(name = "STATION_CODE")
    private String station;
}
