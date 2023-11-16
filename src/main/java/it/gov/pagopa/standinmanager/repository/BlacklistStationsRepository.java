package it.gov.pagopa.standinmanager.repository;

import it.gov.pagopa.standinmanager.repository.entity.BlacklistStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistStationsRepository extends JpaRepository<BlacklistStation, String> {
}
