package it.gov.pagopa.standinmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.exception.AppException;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StationCalcService {

    @Value("${remover.range.minutes}")
    private int rangeMinutes;

    @Value("${remover.range.fault.limit}")
    private double rangeLimit;

    @Value("${info.properties.environment}")
    private String env;

    @Value("${saveDB}")
    private Boolean saveDB;

    @Value("${sendEvent}")
    private Boolean sendEvent;

    @Autowired
    private CosmosStationRepository cosmosStationRepository;
    @Autowired
    private CosmosStationDataRepository cosmosStationDataRepository;
    @Autowired
    private CosmosEventsRepository cosmosEventsRepository;
    @Autowired
    private MailService awsSesClient;
    @Autowired
    private DatabaseStationsRepository dbStationsRepository;
    @Autowired
    private EventHubService eventHubService;

    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public void runCalculations() {
        ZonedDateTime now = ZonedDateTime.now();
        log.info(
                "runCalculations [{}] on {} minutes range with a fault limit of {}",
                now,
                rangeMinutes,
                rangeLimit);

        Map<String, Instant> standInStations =
                cosmosStationRepository.getStations().stream()
                        .collect(Collectors.toMap(d -> d.getStation(), d -> d.getTimestamp()));

        List<CosmosForwarderCallCounts> allCounts =
                cosmosStationDataRepository.getStationCounts(now.minusMinutes(rangeMinutes));
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

                    if (insertTime.plus(rangeMinutes, ChronoUnit.MINUTES).isBefore(Instant.now()) && successfulCalls > rangeLimit) {
                        removeStationAutomatically(station, successfulCalls);
                    }
                });
    }

    public void removeStationFromStandIn(String station) {
        List<CosmosStandInStation> stations = cosmosStationRepository.getStation(station);

        if (stations.isEmpty()) {
            throw new IllegalStateException("Station not found: " + station);
        }

        removeStationManually(stations);
    }

    /**
     * This method is called manually to remove a station from stand-in through internal API
     * @param stations list of station
     */
    private void removeStationManually(List<CosmosStandInStation> stations) {
        String eventInfo = String.format(
                "removing station [%s] from standIn stations manually", stations.get(0).getStation());

        String emailBody = String.format(
                "[StandInManager] Station [%s] has been removed from stand-in manually", stations.get(0).getStation());

        removeStation(stations, eventInfo, emailBody);
    }

    /**
     * This method is called during stationMonitorService.checkStations job execution
     * @param station station code
     * @param successfulCalls number of success
     */
    private void removeStationAutomatically(String station, long successfulCalls) {
        String eventInfo = String.format(
                "removing station [%s] from standIn stations because %s calls were successful"
                        + " in the last %s minutes", station, successfulCalls, rangeMinutes);

        String emailBody = String.format(
                "[StandInManager] Station [%s] has been removed from stand-in"
                        + "\nbecause [%s] calls were successful in the last %s minutes",
                station, successfulCalls, rangeMinutes);
        List<CosmosStandInStation> stations = cosmosStationRepository.getStation(station);

        removeStation(stations, eventInfo, emailBody);
    }

    private void removeStation(List<CosmosStandInStation> stations, String eventInfo, String emailBody) {
        log.info(eventInfo);

        stations.forEach(
                s -> {
                    cosmosStationRepository.removeStation(s);
                });
        String station = stations.get(0).getStation();

        if (sendEvent) {
            log.info("sending {} event for station {}", Constants.type_removed, station);
            try {
                eventHubService.publishEvent(ZonedDateTime.now(), station, Constants.type_removed);
            } catch (JsonProcessingException e) {
                log.error("could not publish {} for stations {}", Constants.type_removed, station);
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Json processing error", e.getMessage());
            }
        }
        if (saveDB) {
            log.info("removing {} from standin database", station);
            dbStationsRepository.deleteById(station);
        }

        cosmosEventsRepository.newEvent(
                station,
                Constants.EVENT_REMOVE_FROM_STANDIN,
                eventInfo
        );

        String sendResult =
                awsSesClient.sendEmail(
                        String.format(
                                "[StandInManager][%s] Station [%s] removed from stand-in", env, station),
                        emailBody
                );
        log.info("email sender: {}", sendResult);
    }

}
