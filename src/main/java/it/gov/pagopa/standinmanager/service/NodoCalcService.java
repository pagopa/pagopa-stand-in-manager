package it.gov.pagopa.standinmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.exception.AppError;
import it.gov.pagopa.standinmanager.exception.AppException;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalTime;
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
    private final double totalTrafficDayThreshold;
    private final double totalTrafficNightThreshold;
    private final String env;
    private final Boolean saveDB;
    private final Boolean sendEvent;
    private final int nighttimeStartHours;
    private final int nighttimeStartMinutes;
    private final int nighttimeEndHours;
    private final int nighttimeEndMinutes;
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
            @Value("${adder.total.daytime.traffic.threshold}") double totalDaytimeTrafficThreshold,
            @Value("${adder.total.nighttime.traffic.threshold}") double totalNighttimeTrafficThreshold,
            @Value("${info.properties.environment}") String env,
            @Value("${saveDB}") Boolean saveDB,
            @Value("${sendEvent}") Boolean sendEvent,
            @Value("${nighttime.start.hours}") int nighttimeStartHours,
            @Value("${nighttime.start.minutes}") int nighttimeStartMinutes,
            @Value("${nighttime.end.hours}") int nighttimeEndHours,
            @Value("${nighttime.end.minutes}") int nighttimeEndMinutes,
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
        this.totalTrafficDayThreshold = totalDaytimeTrafficThreshold;
        this.totalTrafficNightThreshold = totalNighttimeTrafficThreshold;
        this.env = env;
        this.saveDB = saveDB;
        this.sendEvent = sendEvent;
        this.nighttimeStartHours = nighttimeStartHours;
        this.nighttimeStartMinutes = nighttimeStartMinutes;
        this.nighttimeEndHours = nighttimeEndHours;
        this.nighttimeEndMinutes = nighttimeEndMinutes;
        this.cosmosStationRepository = cosmosStationRepository;
        this.cosmosNodeDataRepository = cosmosNodeDataRepository;
        this.cosmosEventsRepository = cosmosEventsRepository;
        this.mailService = mailService;
        this.dbStationsRepository = dbStationsRepository;
        this.eventHubService = eventHubService;
        this.decimalFormat = new DecimalFormat("#.##");
    }

    /**
     * Elaborate the monitored stations traffic in the configured range time {@link NodoCalcService#rangeMinutes}
     * aggregated by the configured slot time {@link NodoCalcService#slotMinutes} and activates the Stand-In mode if all
     * the following condition occur:
     * <ul>
     *     <li> During nighttime hours, from {@link NodoCalcService#nighttimeStartHours}:{@link NodoCalcService#nighttimeStartMinutes}
     *     to {@link NodoCalcService#nighttimeEndHours}:{@link NodoCalcService#nighttimeEndMinutes}
     *     <ul>
     *         <li> Stations have an availability below the configured threshold {@link NodoCalcService#rangeThreshold}
     *         <li> The total payment traffic managed by the station exceeds the configured threshold {@link NodoCalcService#totalTrafficNightThreshold}
     *         of the total traffic of NodoSPC
     *     </ul>
     *     <li> During daytime hours, from {@link NodoCalcService#nighttimeEndHours}:{@link NodoCalcService#nighttimeEndMinutes}
     *     to {@link NodoCalcService#nighttimeStartHours}:{@link NodoCalcService#nighttimeStartMinutes}
     *     <ul>
     *         <li> Stations have an availability below the configured threshold {@link NodoCalcService#rangeThreshold}
     *         <li> The total payment traffic managed by the station exceeds the configured threshold {@link NodoCalcService#totalTrafficDayThreshold}
     *         of the total traffic of NodoSPC
     *     </ul>
     */
    public void runCalculations() {
        ZonedDateTime now = ZonedDateTime.now();
        double totalTrafficThreshold = getTotalTrafficThreshold(now);
        int totalSlots = this.rangeMinutes / this.slotMinutes;
        log.info("runCalculations [{}] on {} minutes range with {} minutes slots", now, this.rangeMinutes, this.slotMinutes);

        Set<String> standInStations = this.cosmosStationRepository.getStations().stream()
                .map(CosmosStandInStation::getStation).collect(Collectors.toSet());

        List<CosmosNodeCallCounts> allCounts = this.cosmosNodeDataRepository.getStationCounts(now.minusMinutes(rangeMinutes));

        Map<String, List<CosmosNodeCallCounts>> allStationCounts = allCounts.stream()
                .collect(Collectors.groupingBy(CosmosNodeCallCounts::getStation));

        allStationCounts.forEach((station, stationCounts) -> {
            if (standInStations.contains(station)) {
                return;
            }

            // TODO could be useless
            List<CosmosNodeCallCounts> groupedCallCounts = groupCosmosNodeCallCountsToSlotInterval(stationCounts);

            long numOfHighTrafficSlots = groupedCallCounts.stream().filter(c -> c.getTotalTrafficPercentage() > totalTrafficThreshold).count();
            long numOfFailedSlots = groupedCallCounts.stream().filter(c -> c.getFaultPercentage() > this.slotThreshold).count();
            double failedSlotPerc = ((numOfFailedSlots / (double) totalSlots) * 100);
            double highTrafficSlotPerc = ((numOfHighTrafficSlots / (double) totalSlots) * 100);

            if (log.isDebugEnabled()) {
                log.debug(
                        "station [{}] data:\n{} of {} slots failed\n{}",
                        station,
                        numOfFailedSlots,
                        totalSlots,
                        groupedCallCounts.stream()
                                .sorted(Comparator.comparingLong(s -> s.getTimestamp().getEpochSecond()))
                                .map(s -> String.format(
                                        "%s : %s/%s: %s%%",
                                        s.getTimestamp(),
                                        s.getFaults(),
                                        s.getTotal(),
                                        this.decimalFormat.format(s.getFaultPercentage())))
                                .collect(Collectors.joining("\n")));
            }

            if (failedSlotPerc > this.rangeThreshold && highTrafficSlotPerc > totalTrafficThreshold) {
                log.info("adding station [{}] to standIn stations because {} of {} slots failed in the last" + " {} minutes",
                        station, numOfFailedSlots, totalSlots, this.rangeMinutes);
                if (Boolean.TRUE.equals(this.sendEvent)) {
                    log.info("sending {} event for station {}", Constants.TYPE_ADDED, station);
                    publishStandInAddedEvent(station);
                }
                if (Boolean.TRUE.equals(this.saveDB)) {
                    log.info("adding {} to standin database", station);
                    this.dbStationsRepository.save(new StandInStation(station));
                }
                this.cosmosStationRepository.save(new CosmosStandInStation(UUID.randomUUID().toString(), station, Instant.now()));
                this.cosmosEventsRepository.newEvent(
                        station,
                        Constants.EVENT_ADD_TO_STANDIN,
                        String.format(
                                "adding station [%s] to standIn stations because [%s] of [%s] slots failed",
                                station, numOfFailedSlots, totalSlots)
                );

                String sendResult = this.mailService.sendEmail(
                        String.format("[StandInManager][%s] Station [%s] added to standin", this.env, station),
                        String.format(
                                "[StandInManager]Station [%s] has been added to standin %nbecause [%s] of [%s] slots failed",
                                station, numOfFailedSlots, totalSlots));
                log.info("email sender: {}", sendResult);
            }
        });
    }

    private void publishStandInAddedEvent(String station) {
        try {
            this.eventHubService.publishEvent(ZonedDateTime.now(), station, Constants.TYPE_ADDED);
        } catch (JsonProcessingException e) {
            log.error("could not publish {} for stations {}", Constants.TYPE_ADDED, station, e);
            throw new AppException(AppError.EVENT_HUB_PUBLISH_ERROR, e);
        }
    }

    private @NotNull List<CosmosNodeCallCounts> groupCosmosNodeCallCountsToSlotInterval(List<CosmosNodeCallCounts> stationCounts) {
        Map<Instant, List<CosmosNodeCallCounts>> fiveMinutesIntervals = stationCounts.stream()
                .collect(Collectors.groupingBy(item -> roundToNearest5Minutes(item.getTimestamp())));

        return fiveMinutesIntervals.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .reduce(
                                new CosmosNodeCallCounts(null, null, entry.getKey(), 0, 0, 0),
                                (f, d) -> {
                                    f.setTotal(f.getTotal() + d.getTotal());
                                    f.setFaults(f.getFaults() + d.getFaults());
                                    return f;
                                }))
                .toList();
    }

    private double getTotalTrafficThreshold(ZonedDateTime now) {
        LocalTime time = now.toLocalTime();
        LocalTime nightStart = LocalTime.of(this.nighttimeStartHours, this.nighttimeStartMinutes);
        LocalTime nightEnd = LocalTime.of(this.nighttimeEndHours, this.nighttimeEndMinutes);

        if (time.isAfter(nightStart) || time.isBefore(nightEnd)) {
            return this.totalTrafficNightThreshold;
        }
        return this.totalTrafficDayThreshold;
    }

    private Instant roundToNearest5Minutes(Instant instant) {
        long epochSeconds = instant.getEpochSecond();
        long roundedSeconds = Math.floorDiv(epochSeconds, this.slotMinutes * 60) * this.slotMinutes * 60;
        return Instant.ofEpochSecond(roundedSeconds);
    }
}
