package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import it.gov.pagopa.standinmanager.service.EventHubService;
import it.gov.pagopa.standinmanager.service.NodoCalcService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.util.ArrayIterator;

@ExtendWith(MockitoExtension.class)
class NodoCalcServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosNodeDataRepository cosmosNodeDataRepository = mock(CosmosNodeDataRepository.class);
    @Mock private CosmosStationRepository cosmosStationsRepository = mock(CosmosStationRepository.class);
    @Mock private DatabaseStationsRepository databaseStationsRepository = mock(DatabaseStationsRepository.class);
    @Mock private CosmosEventsRepository cosmosEventsRepository = mock(CosmosEventsRepository.class);
    @Mock private EventHubService eventHubService;
    @Mock private MailService awsSesClient;

    @InjectMocks
    private NodoCalcService nodoCalcService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "slotMinutes", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "rangeMinutes", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "sendEvent", true);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "saveDB", true);
//        when(kustoClient.execute(any(),any(),any())).thenReturn(new KustoOperationResult("{ \"Tables\":[ \"RE\"] }",""));
//        when(cosmosNodeDataRepository.saveAll(any())).thenReturn(new ArrayIterator<>(null));
//    when(standInStationsRepository.getStations()).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now()),new CosmosStandInStation("","station2", Instant.now())));
    when(cosmosNodeDataRepository.getStationCounts(any()))
        .thenReturn(
            List.of(
                new CosmosNodeCallCounts("", "station1", Instant.now(),10,10),
                    new CosmosNodeCallCounts("", "station2", Instant.now(),10,10)
            )
        );
    }

    @Test
    void test1() throws Exception {
        nodoCalcService.runCalculations();
        verify(eventHubService, times(2)).publishEvent(any(),any(),any());
        verify(databaseStationsRepository, times(2)).save(any());
        verify(cosmosStationsRepository, times(2)).save(any());
        verify(cosmosEventsRepository, times(2)).newEvent(any(),any(),any());
        verify(awsSesClient, times(2)).sendEmail(any(),any());
    }
}
