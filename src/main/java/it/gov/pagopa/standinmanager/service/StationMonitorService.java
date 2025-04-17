package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StationMonitorService {

  @Autowired private ConfigService configService;
  @Autowired private CosmosStationRepository cosmosStationRepository;
  @Autowired private CosmosStationDataRepository cosmosStationDataRepository;
  @Autowired private ForwarderClient forwarderClient;

  public void checkStations() {
    ZonedDateTime now = ZonedDateTime.now();
    log.info("checkStations [{}]", now);
    ConfigDataV1 cache = configService.getCache();
    if (cache != null) {
      List<CosmosStandInStation> stations = cosmosStationRepository.getStations();
      stations.stream()
          .map(s -> Pair.of(s, cache.getStations().get(s.getStation())))
          .filter(s -> s.getRight() != null)
          .map(s -> {
                Station station = s.getRight();
                StationCreditorInstitution creditorInstitution = cache.getCreditorInstitutionStations().entrySet().stream()
                    .filter(e -> e.getKey().startsWith(station.getStationCode()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow();

                return checkStation(now, station, creditorInstitution, s.getLeft());
            }
          )
          .collect(Collectors.toList());
    } else {
      log.warn("Can not run, cache is null");
    }
  }

  @Async
  protected CompletableFuture<Boolean> checkStation(
          ZonedDateTime now, Station station, StationCreditorInstitution creditorInstitution, CosmosStandInStation standInStation) {
    return CompletableFuture.supplyAsync(
        () -> {
          log.info("checkStation [{}] [{}]", now, standInStation.getStation());
          boolean b = false;
          try {
            b = forwarderClient.paVerifyPaymentNotice(station, creditorInstitution);
          } catch (Exception e) {
            log.error("error in verify", e);
          }
          log.info("checkStation done success:[{}]", b);
          CosmosForwarderCallCounts forwarderCallCounts =
              CosmosForwarderCallCounts.builder()
                  .id(UUID.randomUUID().toString())
                  .station(standInStation.getStation())
                  .timestamp(now.toInstant())
                  .outcome(b)
                  .build();
          cosmosStationDataRepository.save(forwarderCallCounts);
          return b;
        });
  }
}
