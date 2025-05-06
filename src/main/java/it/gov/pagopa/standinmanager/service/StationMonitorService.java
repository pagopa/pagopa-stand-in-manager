package it.gov.pagopa.standinmanager.service;

import com.azure.cosmos.implementation.guava25.base.Joiner;
import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationMonitorService {

  private final ConfigService configService;
  private final CosmosStationRepository cosmosStationRepository;
  private final ForwarderClient forwarderClient;
  private final AsyncService asyncService;

  public void checkStations() {
    ZonedDateTime now = ZonedDateTime.now();
    log.info("[checkStations] [{}]", now);
    ConfigDataV1 cache = configService.getCache();
    if (cache != null) {
      List<CosmosStandInStation> stations = cosmosStationRepository.getStations();
      if (log.isDebugEnabled()) {
          List<String> stationCodes = stations.stream().map(CosmosStandInStation::getStation).collect(Collectors.toList());
          log.debug("[checkStations] stations to check: " + String.join(", ", stationCodes));
      }
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
      log.warn("[checkStations] Can not run, cache is null");
    }
  }

  public String testStation(String stationCode) {
      ZonedDateTime now = ZonedDateTime.now();
      ConfigDataV1 cache = configService.getCache();
      Station station = cache.getStations().get(stationCode);
      StationCreditorInstitution creditorInstitution = cache.getCreditorInstitutionStations().entrySet().stream()
              .filter(e -> e.getKey().startsWith(stationCode))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("CreditorInstitution not found for station: " + stationCode));
      return callForwarder(now, station, creditorInstitution);
  }

  public String callForwarder(ZonedDateTime now, Station station, StationCreditorInstitution creditorInstitution) {
      log.info("testStation [{}] [{}]", now, station.getStationCode());
      String response = forwarderClient.testPaVerifyPaymentNotice(station, creditorInstitution);
      log.info("testStation done success:[{}]", station.getStationCode());
      return response;
  }
}
