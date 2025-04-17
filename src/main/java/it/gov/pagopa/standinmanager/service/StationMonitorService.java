package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationMonitorService {

  @Autowired private ConfigService configService;
  @Autowired private CosmosStationRepository cosmosStationRepository;
  private AsyncService asyncService;

  public void checkStations() {
    ZonedDateTime now = ZonedDateTime.now();
    log.info("checkStations [{}]", now);
    ConfigDataV1 cache = configService.getCache();
    if (cache != null) {
      List<CosmosStandInStation> stations = cosmosStationRepository.getStations();
      stations.stream()
          .map(s -> Pair.of(s, cache.getStations().get(s.getStation())))
          .filter(s -> s.getRight() != null)
          .forEach(s -> {
                Station station = s.getRight();
                StationCreditorInstitution creditorInstitution = cache.getCreditorInstitutionStations().entrySet().stream()
                    .filter(e -> e.getKey().startsWith(station.getStationCode()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("CreditorInstitution not found for station: " + station.getStationCode()));

              asyncService.checkStation(now, station, creditorInstitution, s.getLeft());
            }
          );
    } else {
      log.warn("Can not run, cache is null");
    }
  }
}
