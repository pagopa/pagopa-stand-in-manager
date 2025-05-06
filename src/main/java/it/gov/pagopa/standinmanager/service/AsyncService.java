package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncService {

    private final CosmosStationDataRepository cosmosStationDataRepository;
    private final ForwarderClient forwarderClient;

    @Async
    public void checkStation(ZonedDateTime now, Station station, StationCreditorInstitution creditorInstitution, CosmosStandInStation standInStation) {

        log.info("[async checkStation] [{}] [{}]", standInStation.getStation(), now);
        boolean b = false;
        try {
            b = forwarderClient.paVerifyPaymentNotice(station, creditorInstitution);
        } catch (Exception e) {
            log.error(String.format("error in paVerifyPaymentNotice [%s]", station), e);
        }
        log.info("checkStation [{}] done success:[{}]", station, b);
        CosmosForwarderCallCounts forwarderCallCounts =
                CosmosForwarderCallCounts.builder()
                        .id(UUID.randomUUID().toString())
                        .station(standInStation.getStation())
                        .timestamp(now.toInstant())
                        .outcome(b)
                        .build();
        cosmosStationDataRepository.save(forwarderCallCounts);
    }
}
