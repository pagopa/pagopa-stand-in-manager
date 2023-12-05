package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.StandInStationsRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import it.gov.pagopa.standinmanager.util.Constants;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
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

  @Autowired private StandInStationsRepository standInStationsRepository;
  @Autowired private CosmosStationDataRepository cosmosRepository;
  @Autowired private CosmosEventsRepository cosmosEventsRepository;

  private DecimalFormat decimalFormat = new DecimalFormat("#.##");

  public void runCalculations()
      throws URISyntaxException, DataServiceException, DataClientException {
    ZonedDateTime now = ZonedDateTime.now();
    log.info(
        "runCalculations [{}] on {} minutes range with a fault limit of {}",
        now,
        rangeMinutes,
        rangeLimit);

    List<CosmosForwarderCallCounts> allCounts =
        cosmosRepository.getStationCounts(now.minusMinutes(rangeMinutes));
    Map<String, List<CosmosForwarderCallCounts>> allStationCounts =
        allCounts.stream().collect(Collectors.groupingBy(CosmosForwarderCallCounts::getStation));

    allStationCounts.forEach(
        (station, stationCounts) -> {
          long successfulCalls = stationCounts.stream().filter(d -> d.getOutcome()).count();
          if (log.isDebugEnabled()) {
            log.debug(
                "station [{}] data:\n{} of {} calls were successful",
                station,
                successfulCalls,
                stationCounts.size());
          }

          if (successfulCalls > rangeLimit) {
            log.info(
                "removing station [{}] from standIn stations because {} calls were successful in"
                    + " the last {} minutes",
                station,
                successfulCalls,
                rangeMinutes);
            standInStationsRepository.deleteById(station);

            cosmosEventsRepository.newEvent(
                station,
                Constants.EVENT_REMOVE_FROM_STANDIN,
                String.format(
                    "removing station [%s] from standIn stations because %s calls were successful"
                        + " in the last %s minutes",
                    station, successfulCalls, rangeMinutes));
          }
        });
  }
}
