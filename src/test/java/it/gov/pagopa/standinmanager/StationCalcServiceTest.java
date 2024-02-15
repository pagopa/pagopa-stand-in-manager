package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationCalcServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosDatabase cosmosDatabase;
    @Mock private CosmosContainer cosmosContainer;
    @Mock private CosmosPagedIterable cosmosPagedIterable;
    @Mock private EventHubService eventHubService;
    @Mock private MailService awsSesClient;

    @Mock private DatabaseStationsRepository databaseStationsRepository = mock(DatabaseStationsRepository.class);

//    private CosmosNodeDataRepository cosmosNodeDataRepository = spy(new CosmosNodeDataRepository());
    private CosmosStationDataRepository cosmosStationDataRepository = spy(new CosmosStationDataRepository());
    private CosmosStationRepository cosmosStationRepository = spy(new CosmosStationRepository());
    private CosmosEventsRepository cosmosEventsRepository = spy(CosmosEventsRepository.class);

    @InjectMocks
    private StationCalcService stationCalcService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "rangeMinutes", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "rangeLimit", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "sendEvent", true);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "saveDB", true);

        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationDataRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosEventsRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "cosmosStationDataRepository", cosmosStationDataRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "cosmosStationRepository", cosmosStationRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(stationCalcService, "cosmosEventsRepository", cosmosEventsRepository);




        when(cosmosClient.getDatabase(any())).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(any())).thenReturn(cosmosContainer);
        when(cosmosContainer.queryItems(any(SqlQuerySpec.class),any(),any())).thenReturn(cosmosPagedIterable);
        when(cosmosPagedIterable.stream()).thenReturn(Arrays.asList(
                new CosmosStandInStation("","station1",Instant.now().minusSeconds(600)),
                new CosmosStandInStation("","station2",Instant.now().minusSeconds(600))
        ).stream(),
                Arrays.asList(
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
                ).stream(),
                Arrays.asList(
                        new CosmosStandInStation("","station1",Instant.now().minusSeconds(600))
                ).stream(),
                Arrays.asList(
                        new CosmosStandInStation("","station2",Instant.now().minusSeconds(600))
                        ).stream());

//    when(cosmosStationDataRepository.getStationCounts(any()))
//        .thenReturn(
//            List.of(
//                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station1", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station2", Instant.now(),true),
//                new CosmosForwarderCallCounts("", "station2", Instant.now(),true)
//            )
//        );
    }

    @Test
    void test1() throws Exception {
        stationCalcService.runCalculations();
        verify(eventHubService, times(2)).publishEvent(any(),any(),any());
        verify(databaseStationsRepository, times(2)).deleteById(any());
        verify(cosmosStationRepository, times(2)).removeStation(any());
        verify(cosmosEventsRepository, times(2)).newEvent(any(),any(),any());
        verify(awsSesClient, times(2)).sendEmail(any(),any());
    }
}
