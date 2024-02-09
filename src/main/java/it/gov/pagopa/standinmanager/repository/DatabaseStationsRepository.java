package it.gov.pagopa.standinmanager.repository;

import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseStationsRepository extends JpaRepository<StandInStation, String> {}
