package it.gov.pagopa.standinmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodoCalcService {

    private final double slotThreshold;
    private final int slotMinutes;
    private final int rangeMinutes;
    private final double rangeThreshold;
    private final String env;
    private final Boolean saveDB;
    private final Boolean sendEvent;
    private final CosmosStationRepository cosmosStationRepository;
    private final CosmosNodeDataRepository cosmosNodeDataRepository;
    private final CosmosEventsRepository cosmosEventsRepository;
    private final MailService mailService;
    private final DatabaseStationsRepository dbStationsRepository;
    private final EventHubService eventHubService;
    private final DecimalFormat decimalFormat;

    public NodoCalcService(
            @Value("${adder.slot.fault.threshold}") double slotThreshold,
            @Value("${adder.slot.minutes}") int slotMinutes,
            @Value("${adder.range.minutes}") int rangeMinutes,
            @Value("${adder.range.fault.threshold}") double rangeThreshold,
            @Value("${info.properties.environment}") String env,
            @Value("${saveDB}") Boolean saveDB,
            @Value("${sendEvent}") Boolean sendEvent,
            CosmosStationRepository cosmosStationRepository,
            CosmosNodeDataRepository cosmosNodeDataRepository,
            CosmosEventsRepository cosmosEventsRepository,
            MailService mailService,
            DatabaseStationsRepository dbStationsRepository,
            EventHubService eventHubService
    ) {
        this.slotThreshold = slotThreshold;
        this.slotMinutes = slotMinutes;
        this.rangeMinutes = rangeMinutes;
        this.rangeThreshold = rangeThreshold;
        this.env = env;
        this.saveDB = saveDB;
        this.sendEvent = sendEvent;
        this.cosmosStationRepository = cosmosStationRepository;
        this.cosmosNodeDataRepository = cosmosNodeDataRepository;
        this.cosmosEventsRepository = cosmosEventsRepository;
        this.mailService = mailService;
        this.dbStationsRepository = dbStationsRepository;
        this.eventHubService = eventHubService;
        this.decimalFormat = new DecimalFormat("#.##");
    }

    public void runCalculations() {
        ZonedDateTime now = ZonedDateTime.now();
        int totalSlots = this.rangeMinutes / this.slotMinutes;
        log.info(
                "runCalculations [{}] on {} minutes range with {} minutes slots",
                now,
                this.rangeMinutes,
                this.slotMinutes);

        Set<String> standInStations =
                this.cosmosStationRepository.getStations().stream()
                        .map(CosmosStandInStation::getStation)
                        .collect(Collectors.toSet());

        List<CosmosNodeCallCounts> allCounts =
                this.cosmosNodeDataRepository.getStationCounts(now.minusMinutes(rangeMinutes));

        Map<String, List<CosmosNodeCallCounts>> allStationCounts =
                allCounts.stream().collect(Collectors.groupingBy(CosmosNodeCallCounts::getStation));

        allStationCounts.forEach(
                (station, stationCounts) -> {
                    if (standInStations.contains(station)) {
                        return;
                    }
                    Map<Instant, List<CosmosNodeCallCounts>> fiveMinutesIntervals =
                            stationCounts.stream()
                                    .collect(
                                            Collectors.groupingBy(item -> roundToNearest5Minutes(item.getTimestamp())));
                    List<CosmosNodeCallCounts> collect =
                            fiveMinutesIntervals.entrySet().stream()
                                    .map(
                                            entry -> entry.getValue().stream()
                                                    .reduce(
                                                            new CosmosNodeCallCounts(null, null, entry.getKey(), 0, 0),
                                                            (f, d) -> {
                                                                f.setTotal(f.getTotal() + d.getTotal());
                                                                f.setFaults(f.getFaults() + d.getFaults());
                                                                return f;
                                                            }))
                                    .collect(Collectors.toList());

                    long failedSlots = collect.stream().filter(c -> c.getPerc() > this.slotThreshold).count();
                    double failedSlotPerc = ((failedSlots / (double) totalSlots) * 100);
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "station [{}] data:\n{} of {} slots failed\n{}",
                                station,
                                failedSlots,
                                totalSlots,
                                collect.stream()
                                        .sorted(Comparator.comparingLong(s -> s.getTimestamp().getEpochSecond()))
                                        .map(s -> String.format(
                                                "%s : %s/%s: %s%%",
                                                s.getTimestamp(),
                                                s.getFaults(),
                                                s.getTotal(),
                                                this.decimalFormat.format(s.getPerc())))
                                        .collect(Collectors.joining("\n")));
                    }

                    if (failedSlotPerc > this.rangeThreshold) {
                        log.info(
                                "adding station [{}] to standIn stations because {} of {} slots failed in the last"
                                        + " {} minutes",
                                station,
                                failedSlots,
                                totalSlots,
                                this.rangeMinutes);
                        if (Boolean.TRUE.equals(this.sendEvent)) {
                            log.info("sending {} event for station {}", Constants.type_added, station);
                            try {
                                this.eventHubService.publishEvent(ZonedDateTime.now(), station, Constants.type_added);
                            } catch (JsonProcessingException e) {
                                log.error("could not publish {} for stations {}", Constants.type_added, station);
                                throw new RuntimeException(e);
                            }
                        }
                        if (Boolean.TRUE.equals(this.saveDB)) {
                            log.info("adding {} to standin database", station);
                            this.dbStationsRepository.save(new StandInStation(station));
                        }
                        this.cosmosStationRepository.save(
                                new CosmosStandInStation(UUID.randomUUID().toString(), station, Instant.now()));
                        this.cosmosEventsRepository.newEvent(
                                station,
                                Constants.EVENT_ADD_TO_STANDIN,
                                String.format(
                                        "adding station [%s] to standIn stations because [%s] of [%s] slots failed",
                                        station, failedSlots, totalSlots));
                        String sendResult =
                                this.mailService.sendEmail(
                                        String.format(
                                                "[StandInManager][%s] Station [%s] added to standin", this.env, station),
                                        String.format(
                                                "[StandInManager]Station [%s] has been added to standin"
                                                        + "\nbecause [%s] of [%s] slots failed",
                                                station, failedSlots, totalSlots));
                        log.info("email sender: {}", sendResult);
                    }
                });
    }

    private Instant roundToNearest5Minutes(Instant instant) {
        long epochSeconds = instant.getEpochSecond();
        long roundedSeconds = Math.floorDiv(epochSeconds, this.slotMinutes * 60) * this.slotMinutes * 60;
        return Instant.ofEpochSecond(roundedSeconds);
    }
}
