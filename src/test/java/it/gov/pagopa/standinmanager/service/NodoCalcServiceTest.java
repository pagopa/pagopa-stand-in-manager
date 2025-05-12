package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = NodoCalcService.class)
class NodoCalcServiceTest {

    private static final int ALL_STATION_CALL = 120;
    private static final String STATION_1 = "station1";
    private static final String STATION_2 = "station2";
    private static final String STATION_3 = "station3";

    @MockBean
    private CosmosNodeDataRepository cosmosNodeDataRepository;
    @MockBean
    private CosmosStationRepository cosmosStationRepository;
    @MockBean
    private CosmosEventsRepository cosmosEventsRepository;
    @MockBean
    private MailService mailService;
    @MockBean
    private DatabaseStationsRepository databaseStationsRepository;
    @MockBean
    private EventHubService eventHubService;

    @Autowired
    private NodoCalcService sut;

    @Test
    void runCalculationsSuccess() throws Exception {
        ReflectionTestUtils.setField(sut, "sendEvent", true);
        ReflectionTestUtils.setField(sut, "saveDB", true);

        doReturn(buildCosmosStandInStationList()).when(cosmosStationRepository).getStations();
        doReturn(buildCosmosNodeCallCountsList()).when(cosmosNodeDataRepository).getStationCounts(any());


        Assertions.assertDoesNotThrow(() -> sut.runCalculations());

        verify(eventHubService).publishEvent(any(), eq(STATION_2), any());
        verify(databaseStationsRepository).save(any());
        verify(cosmosStationRepository).save(any());
        verify(cosmosEventsRepository).newEvent(any(), any(), any());
        verify(mailService).sendEmail(anyString(), anyString());
    }

    @Test
    void runCalculationsSuccessWithSendEventAndSaveDBFalse() throws Exception {
        ReflectionTestUtils.setField(sut, "sendEvent", false);
        ReflectionTestUtils.setField(sut, "saveDB", false);

        doReturn(buildCosmosStandInStationList()).when(cosmosStationRepository).getStations();
        doReturn(buildCosmosNodeCallCountsList()).when(cosmosNodeDataRepository).getStationCounts(any());


        Assertions.assertDoesNotThrow(() -> sut.runCalculations());

        verify(eventHubService, never()).publishEvent(any(), eq(STATION_2), any());
        verify(databaseStationsRepository, never()).save(any());
        verify(cosmosStationRepository).save(any());
        verify(cosmosEventsRepository).newEvent(any(), any(), any());
        verify(mailService).sendEmail(anyString(), anyString());
    }

    private List<CosmosStandInStation> buildCosmosStandInStationList() {
        return List.of(CosmosStandInStation.builder().station(STATION_3).build());
    }

    private List<CosmosNodeCallCounts> buildCosmosNodeCallCountsList() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        return List.of(
                buildCosmosNodeCallCounts(STATION_1, now, 5, 2),
                buildCosmosNodeCallCounts(STATION_1, now, 10, 0),
                buildCosmosNodeCallCounts(STATION_1, now, 15, 1),
                buildCosmosNodeCallCounts(STATION_1, now, 20, 0),
                buildCosmosNodeCallCounts(STATION_1, now, 25, 10), // duplicated slot, should be removed because older
                buildCosmosNodeCallCounts(STATION_1, now, 27, 7),  // duplicated slot, should be keep because most recent
                buildCosmosNodeCallCounts(STATION_1, now, 30, 4),
                buildCosmosNodeCallCounts(STATION_2, now, 5, 0),
                buildCosmosNodeCallCounts(STATION_2, now, 10, 5),
                buildCosmosNodeCallCounts(STATION_2, now, 15, 8),
                buildCosmosNodeCallCounts(STATION_2, now, 20, 9),
                buildCosmosNodeCallCounts(STATION_2, now, 25, 7),
                buildCosmosNodeCallCounts(STATION_2, now, 30, 6),
                buildCosmosNodeCallCounts(STATION_3, now, 5, 0),
                buildCosmosNodeCallCounts(STATION_3, now, 10, 2),
                buildCosmosNodeCallCounts(STATION_3, now, 15, 8),
                buildCosmosNodeCallCounts(STATION_3, now, 20, 0),
                buildCosmosNodeCallCounts(STATION_3, now, 25, 1),
                buildCosmosNodeCallCounts(STATION_3, now, 30, 1)
        );
    }

    private CosmosNodeCallCounts buildCosmosNodeCallCounts(
            String station,
            Instant now,
            int minutesToAdd,
            int faults
    ) {
        return CosmosNodeCallCounts.builder()
                .id(UUID.randomUUID().toString())
                .station(station)
                .timestamp(now.plus(minutesToAdd, ChronoUnit.MINUTES))
                .total(10)
                .faults(faults)
                .allStationCallInSlot(ALL_STATION_CALL)
                .build();
    }
}
