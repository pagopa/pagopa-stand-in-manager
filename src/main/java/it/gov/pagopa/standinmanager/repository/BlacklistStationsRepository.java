package it.gov.pagopa.standinmanager.repository;

import it.gov.pagopa.standinmanager.repository.entity.BlacklistStation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistStationsRepository extends JpaRepository<BlacklistStation, String> {

  @Query("select station from BlacklistStation")
  public List<String> findAllStations();
}
