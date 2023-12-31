package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.client.AwsSesClient;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
//import it.gov.pagopa.standinmanager.repository.StandInStationsRepository;
import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.util.Constants;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NodoCalcService {

  @Value("${adder.slot.fault.threshold}")
  private double slotThreshold;

  @Value("${adder.slot.minutes}")
  private int slotMinutes;

  @Value("${adder.range.minutes}")
  private int rangeMinutes;

  @Value("${adder.range.fault.threshold}")
  private double rangeThreshold;

  @Value("${aws.mailto}")
  private String mailto;

  @Autowired private CosmosStationRepository cosmosStationRepository;
//  @Autowired private StandInStationsRepository standInStationsRepository;
  @Autowired private CosmosNodeDataRepository cosmosRepository;
  @Autowired private CosmosEventsRepository cosmosEventsRepository;
  @Autowired private AwsSesClient awsSesClient;

  private DecimalFormat decimalFormat = new DecimalFormat("#.##");

  private Instant roundToNearest5Minutes(Instant instant) {
    long epochSeconds = instant.getEpochSecond();
    long roundedSeconds = Math.floorDiv(epochSeconds, slotMinutes * 60) * slotMinutes * 60;
    return Instant.ofEpochSecond(roundedSeconds);
  }

  public void runCalculations()
      throws URISyntaxException, DataServiceException, DataClientException {
    ZonedDateTime now = ZonedDateTime.now();
    int totalSlots = rangeMinutes / slotMinutes;
    log.info(
        "runCalculations [{}] on {} minutes range with {} minutes slots",
        now,
        rangeMinutes,
        slotMinutes);

    List<CosmosNodeCallCounts> allCounts =
        cosmosRepository.getStationCounts(now.minusMinutes(rangeMinutes));
    Map<String, List<CosmosNodeCallCounts>> allStationCounts =
        allCounts.stream().collect(Collectors.groupingBy(CosmosNodeCallCounts::getStation));

    allStationCounts.forEach(
        (station, stationCounts) -> {
          Map<Instant, List<CosmosNodeCallCounts>> fiveMinutesIntervals =
              stationCounts.stream()
                  .collect(
                      Collectors.groupingBy(item -> roundToNearest5Minutes(item.getTimestamp())));
          List<CosmosNodeCallCounts> collect =
              fiveMinutesIntervals.entrySet().stream()
                  .map(
                      entry -> {
                        CosmosNodeCallCounts reduced =
                            entry.getValue().stream()
                                .reduce(
                                    new CosmosNodeCallCounts(null, null, entry.getKey(), 0, 0),
                                    (f, d) -> {
                                      f.setTotal(f.getTotal() + d.getTotal());
                                      f.setFaults(f.getFaults() + d.getFaults());
                                      return f;
                                    });
                        return reduced;
                      })
                  .collect(Collectors.toList());

          long failedSlots = collect.stream().filter(c -> c.getPerc() > slotThreshold).count();
          double failedSlotPerc = ((failedSlots / (double) totalSlots) * 100);
          if (log.isDebugEnabled()) {
            log.debug(
                "station [{}] data:\n{} of {} slots failed\n{}",
                station,
                failedSlots,
                totalSlots,
                String.join(
                    "\n",
                    collect.stream()
                        .sorted(Comparator.comparingLong(s -> s.getTimestamp().getEpochSecond()))
                        .map(
                            s ->
                                s.getTimestamp()
                                    + " : "
                                    + s.getFaults()
                                    + "/"
                                    + s.getTotal()
                                    + ": "
                                    + decimalFormat.format(s.getPerc())
                                    + "%")
                        .collect(Collectors.toList())));
          }

          if (failedSlotPerc > rangeThreshold) {
            log.info(
                "adding station [{}] to standIn stations because {} of {} slots failed in the last"
                    + " {} minutes",
                station,
                failedSlots,
                totalSlots,
                rangeMinutes);
//            standInStationsRepository.save(new StandInStation(station));
              cosmosStationRepository.save(new CosmosStandInStation(station,Instant.now()));
            cosmosEventsRepository.newEvent(
                station,
                Constants.EVENT_ADD_TO_STANDIN,
                String.format(
                    "adding station [%s] to standIn stations because [%s] of [%s] slots failed",
                    station, failedSlots, totalSlots));
            awsSesClient.sendEmail(
                String.format("[StandInManager]Station [%s] added to standin"),
                String.format(
                    "[StandInManager]Station [%s] has been added to standin"
                        + "\nbecause [%s] of [%s] slots failed",
                    station, failedSlots, totalSlots),
                mailto);
          }
        });
  }
}
