package it.gov.pagopa.standinmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.util.Constants;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StationCalcService {

  @Value("${remover.range.minutes}")
  private int rangeMinutes;

  @Value("${remover.range.fault.limit}")
  private double rangeLimit;

  @Value("${info.properties.environment}")
  private String env;

  @Value("#{'${saveDB}'=='true'}")
  private Boolean saveDB;

  @Value("#{'${sendEvent}'=='true'}")
  private Boolean sendEvent;

  @Autowired private CosmosStationRepository standInStationsRepository;
  @Autowired private CosmosStationRepository cosmosStationRepository;
  @Autowired private CosmosStationDataRepository cosmosRepository;
  @Autowired private CosmosEventsRepository cosmosEventsRepository;
  @Autowired private MailService awsSesClient;
  @Autowired private DatabaseStationsRepository dbStationsRepository;
  @Autowired private EventHubService eventHubService;

  private DecimalFormat decimalFormat = new DecimalFormat("#.##");

  public void runCalculations()
      throws URISyntaxException, DataServiceException, DataClientException {
    ZonedDateTime now = ZonedDateTime.now();
    log.info(
        "runCalculations [{}] on {} minutes range with a fault limit of {}",
        now,
        rangeMinutes,
        rangeLimit);

    Map<String, Instant> standInStations =
        standInStationsRepository.getStations().stream()
            .collect(Collectors.toMap(d -> d.getStation(), d -> d.getTimestamp()));

    List<CosmosForwarderCallCounts> allCounts =
        cosmosRepository.getStationCounts(now.minusMinutes(rangeMinutes));
    Map<String, List<CosmosForwarderCallCounts>> allStationCounts =
        allCounts.stream().collect(Collectors.groupingBy(CosmosForwarderCallCounts::getStation));

    allStationCounts.forEach(
        (station, stationCounts) -> {
          if (!standInStations.containsKey(station)) {
            return;
          }
          Instant insertTime = standInStations.get(station);
          long successfulCalls = stationCounts.stream().filter(d -> d.getOutcome()).count();
          if (log.isDebugEnabled()) {
            log.debug(
                "station [{}] data:\n{} of {} calls were successful",
                station,
                successfulCalls,
                stationCounts.size());
          }

          if (insertTime.plus(rangeMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())
              && successfulCalls > rangeLimit) {
            log.info(
                "removing station [{}] from standIn stations because {} calls were successful in"
                    + " the last {} minutes",
                station,
                successfulCalls,
                rangeMinutes);
            //            standInStationsRepository.deleteById(station);
            List<CosmosStandInStation> stations = cosmosStationRepository.getStation(station);
            stations.forEach(
                s -> {
                  cosmosStationRepository.removeStation(s);
                });

            if (sendEvent) {
              log.info("sending {} event for station {}",Constants.type_removed,station);
              try {
                eventHubService.publishEvent(ZonedDateTime.now(), station, Constants.type_removed);
              } catch (JsonProcessingException e) {
                log.error("could not publish {} for stations {}", Constants.type_removed, station);
                throw new RuntimeException(e);
              }
            }
            if (saveDB) {
              log.info("removing {} from standin database",station);
              dbStationsRepository.deleteById(station);
            }

            cosmosEventsRepository.newEvent(
                station,
                Constants.EVENT_REMOVE_FROM_STANDIN,
                String.format(
                    "removing station [%s] from standIn stations because %s calls were successful"
                        + " in the last %s minutes",
                    station, successfulCalls, rangeMinutes));

            String sendResult =
                awsSesClient.sendEmail(
                    String.format(
                        "[StandInManager][%s] Station [%s] removed from standin", env, station),
                    String.format(
                        "[StandInManager]Station [%s] has been removed from standin"
                            + "\nbecause [%s] calls were successful in the last %s minutes",
                        station, successfulCalls, rangeMinutes));
            log.info("email sender: {}", sendResult);
          }
        });
  }
}
