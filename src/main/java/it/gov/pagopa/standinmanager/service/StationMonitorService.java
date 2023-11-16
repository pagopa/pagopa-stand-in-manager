package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.StandInStationsRepository;
import it.gov.pagopa.standinmanager.repository.entity.StandInStation;
import it.gov.pagopa.standinmanager.repository.model.ForwarderCallCounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class StationMonitorService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private StandInStationsRepository standInStationsRepository;

    @Autowired
    private CosmosStationDataRepository cosmosStationDataRepository;

    @Autowired
    private ForwarderClient forwarderClient;

    public void checkStations(){
        ZonedDateTime now = ZonedDateTime.now();
        log.info("checkStations [{}]",now);

        ConfigDataV1 cache = configService.getCache();
        List<StandInStation> stations = standInStationsRepository.findAll();
        stations.forEach(s->checkStation(cache,now,s));
    }


    @Async
    private CompletableFuture<Boolean> checkStation(ConfigDataV1 cache, ZonedDateTime now,StandInStation standInStation){
        log.info("checkStation {}",standInStation.getStation());
        Station station = cache.getStations().get(standInStation.getStation());
        boolean b = true;//forwarderClient.verifyPaymentNotice(station);
        ForwarderCallCounts forwarderCallCounts = ForwarderCallCounts.builder()
                .id((standInStation.getStation() + now).hashCode() + "")
                .station(standInStation.getStation())
                .timestamp(now.toInstant())
                .outcome(b)
                .build();
        cosmosStationDataRepository.save(forwarderCallCounts);
        return CompletableFuture.completedFuture(b);
    }

}
