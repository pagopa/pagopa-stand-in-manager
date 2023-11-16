package it.gov.pagopa.standinmanager.repository.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "STAND_IN_STATIONS")
@Data
public class StandInStation {
    @Id
    @Column(name = "STATION_CODE")
    private String station;
}
