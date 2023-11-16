package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.repository.BlacklistStationsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.StandInStationsRepository;
import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import it.gov.pagopa.standinmanager.repository.model.NodeCallCounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private StandInStationsRepository standInStationsRepository;
    @Autowired
    private BlacklistStationsRepository blacklistStationsRepository;
    @Autowired
    private CosmosNodeDataRepository cosmosRepository;

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private Instant roundToNearest5Minutes(Instant instant) {
        long epochSeconds = instant.getEpochSecond();
        long roundedSeconds = Math.floorDiv(epochSeconds, slotMinutes * 60) * slotMinutes * 60;
        return Instant.ofEpochSecond(roundedSeconds);
    }


    public void runCalculations() throws URISyntaxException, DataServiceException, DataClientException {
        ZonedDateTime now = ZonedDateTime.now();
        int totalSlots = rangeMinutes/slotMinutes;
        log.info("runCalculations [{}] on {} minutes range with {} minutes slots",now,rangeMinutes,slotMinutes);

        List<NodeCallCounts> allCounts = cosmosRepository.getStationCounts(now.minusMinutes(rangeMinutes));
        Map<String, List<NodeCallCounts>> allStationCounts = allCounts.stream().collect(Collectors.groupingBy(NodeCallCounts::getStation));

        allStationCounts.forEach((station,stationCounts)->{
            Map<Instant, List<NodeCallCounts>> fiveMinutesIntervals = stationCounts.stream().collect(Collectors.groupingBy(item -> roundToNearest5Minutes(item.getTimestamp())));
            List<NodeCallCounts> collect = fiveMinutesIntervals.entrySet().stream().map(entry -> {
                NodeCallCounts reduced = entry.getValue().stream().reduce(new NodeCallCounts(null,null,entry.getKey(),0,0), (f, d) -> {
                    f.setTotal(f.getTotal() + d.getTotal());
                    f.setFaults(f.getFaults() + d.getFaults());
                    return f;
                });
                return reduced;
            }).collect(Collectors.toList());

            long failedSlots = collect.stream().filter(c -> c.getPerc() > slotThreshold).count();
            double failedSlotPerc = ((failedSlots / (double) totalSlots) * 100);
            if(log.isDebugEnabled()){
                log.debug(
                        "station [{}] data:\n{} of {} slots failed\n{}",
                        station,
                        failedSlots,totalSlots,
                        String.join("\n",collect.stream()
                                .sorted(Comparator.comparingLong(s->s.getTimestamp().getEpochSecond()))
                                .map(s->s.getTimestamp() + " : "+s.getFaults()+"/"+s.getTotal()+": "+decimalFormat.format(s.getPerc())+"%")
                                .collect(Collectors.toList()))
                );
            }

            if(failedSlotPerc>rangeThreshold){
                log.info("adding station [{}] to standIn stations because {} of {} slots failed",station,failedSlots,totalSlots);
                standInStationsRepository.save(new StandInStation(station));
            }
        });

    }

}
