package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.StandInStationsRepository;
import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StationMonitorService {

  @Autowired private ConfigService configService;

  @Autowired private StandInStationsRepository standInStationsRepository;

  @Autowired private CosmosStationDataRepository cosmosStationDataRepository;

  @Autowired private ForwarderClient forwarderClient;

  public void checkStations() {
    ZonedDateTime now = ZonedDateTime.now();
    log.info("checkStations [{}]", now);
    ConfigDataV1 cache = configService.getCache();
    List<StandInStation> stations = standInStationsRepository.findAll();
    stations.stream()
        .map(s -> checkStation(now, cache.getStations().get(s.getStation()), s))
        .collect(Collectors.toList());
  }

  @Async
  private CompletableFuture<Boolean> checkStation(
      ZonedDateTime now, Station station, StandInStation standInStation) {
    return CompletableFuture.supplyAsync(
        () -> {
          log.info("checkStation [{}] [{}]", now, standInStation.getStation());
          boolean b = forwarderClient.verifyPaymentNotice(station);
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
