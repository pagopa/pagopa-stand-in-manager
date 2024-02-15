package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.repository.*;
import it.gov.pagopa.standinmanager.repository.model.CosmosForwarderCallCounts;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.service.EventHubService;
import it.gov.pagopa.standinmanager.service.StationCalcService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StationCalcServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosNodeDataRepository cosmosNodeDataRepository = mock(CosmosNodeDataRepository.class);
    @Mock private CosmosStationRepository cosmosStationsRepository = mock(CosmosStationRepository.class);
    @Mock private CosmosStationDataRepository cosmosStationDataRepository = mock(CosmosStationDataRepository.class);
    @Mock private DatabaseStationsRepository databaseStationsRepository = mock(DatabaseStationsRepository.class);
    @Mock private CosmosEventsRepository cosmosEventsRepository = mock(CosmosEventsRepository.class);
    @Mock private EventHubService eventHubService;
    @Mock private MailService awsSesClient;

    @InjectMocks
    private StationCalcService stationCalcService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "rangeMinutes", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "rangeLimit", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "sendEvent", true);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "saveDB", true);

//        when(kustoClient.execute(any(),any(),any())).thenReturn(new KustoOperationResult("{ \"Tables\":[ \"RE\"] }",""));
//        when(cosmosNodeDataRepository.saveAll(any())).thenReturn(new ArrayIterator<>(null));
    when(cosmosStationsRepository.getStations()).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now().minusSeconds(600)),new CosmosStandInStation("","station2", Instant.now().minusSeconds(600))));
    when(cosmosStationsRepository.getStation("station1")).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now().minusSeconds(600))));
    when(cosmosStationsRepository.getStation("station2")).thenReturn(List.of(new CosmosStandInStation("","station2", Instant.now().minusSeconds(600))));

    when(cosmosStationDataRepository.getStationCounts(any()))
        .thenReturn(
            List.of(
                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
                new CosmosForwarderCallCounts("", "station2", Instant.now(),true)
            )
        );
    }

    @Test
    void test1() throws Exception {
        stationCalcService.runCalculations();
        verify(eventHubService, times(2)).publishEvent(any(),any(),any());
        verify(databaseStationsRepository, times(2)).deleteById(any());
        verify(cosmosStationsRepository, times(2)).removeStation(any());
        verify(cosmosEventsRepository, times(2)).newEvent(any(),any(),any());
        verify(awsSesClient, times(2)).sendEmail(any(),any());
    }
}
