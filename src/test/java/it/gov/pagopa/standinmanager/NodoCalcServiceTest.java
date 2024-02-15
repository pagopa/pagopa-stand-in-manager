package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.microsoft.azure.kusto.data.Client;
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
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class NodoCalcServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosNodeDataRepository cosmosNodeDataRepository = mock(CosmosNodeDataRepository.class);
    @Mock private CosmosStationRepository cosmosStationsRepository = mock(CosmosStationRepository.class);
    @Mock private DatabaseStationsRepository databaseStationsRepository = mock(DatabaseStationsRepository.class);

    private CosmosEventsRepository cosmosEventsRepository = spy(new CosmosEventsRepository());
    private EventHubService eventHubService = new EventHubService();
    private MailService mailService = new MailService();

    @Mock private EventHubProducerClient producer;
    @Mock private SesClient sesClient;
    EventDataBatch testBatch = mock(EventDataBatch.class);
    @Mock private SendEmailResponse testResponse;
    @Mock private CosmosDatabase cosmosDatabase;
    @Mock private CosmosContainer cosmosContainer;

    @InjectMocks
    private NodoCalcService nodoCalcService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosEventsRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(mailService, "sesClient", sesClient);
        org.springframework.test.util.ReflectionTestUtils.setField(mailService, "from", "test@test.it");
        org.springframework.test.util.ReflectionTestUtils.setField(mailService, "mailto", "test@test.it");
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "mailService", mailService);
        org.springframework.test.util.ReflectionTestUtils.setField(eventHubService, "producer", producer);
    org.springframework.test.util.ReflectionTestUtils.setField(
        eventHubService, "om", new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "eventHubService", eventHubService);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "slotMinutes", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "rangeMinutes", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "sendEvent", true);
        org.springframework.test.util.ReflectionTestUtils.setField(nodoCalcService, "saveDB", true);
//        when(kustoClient.execute(any(),any(),any())).thenReturn(new KustoOperationResult("{ \"Tables\":[ \"RE\"] }",""));
//        when(cosmosNodeDataRepository.saveAll(any())).thenReturn(new ArrayIterator<>(null));
//    when(standInStationsRepository.getStations()).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now()),new CosmosStandInStation("","station2", Instant.now())));

        when(cosmosClient.getDatabase(any())).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(any())).thenReturn(cosmosContainer);
        when(testBatch.tryAdd(any())).thenReturn(true);
    when(testBatch.getCount()).thenReturn(2);
    when(producer.createBatch()).thenReturn(testBatch);
    when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(testResponse);
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
        verify(producer, times(2)).send(any(EventDataBatch.class));
        verify(databaseStationsRepository, times(2)).save(any());
        verify(cosmosStationsRepository, times(2)).save(any());
        verify(cosmosEventsRepository, times(2)).newEvent(any(),any(),any());
        verify(sesClient, times(2)).sendEmail(any(SendEmailRequest.class));
    }
}
