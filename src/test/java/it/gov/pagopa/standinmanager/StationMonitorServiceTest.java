package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Service;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.service.ConfigService;
import it.gov.pagopa.standinmanager.service.StationMonitorService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class StationMonitorServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosDatabase cosmosDatabase;
    @Mock private CosmosContainer cosmosContainer;
    @Mock private CosmosPagedIterable cosmosPagedIterable;
    @Mock private ConfigService configService = mock(ConfigService.class);
    @Mock private RestTemplate restTemplate;

    private CosmosNodeDataRepository cosmosNodeDataRepository = spy(new CosmosNodeDataRepository());
    private CosmosStationDataRepository cosmosStationDataRepository = spy(new CosmosStationDataRepository());
    private CosmosStationRepository cosmosStationRepository = spy(new CosmosStationRepository());
    private CosmosEventsRepository cosmosEventsRepository = spy(CosmosEventsRepository.class);

    private ForwarderClient forwarderClient = new ForwarderClient();


    @InjectMocks
    private StationMonitorService stationMonitorService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        Map<String, Station> stations = new HashMap<>();
        Station station1 = new Station();
        station1.setBrokerCode("broker1");
        station1.setStationCode("station1");
        Service service = new Service();
        service.setTargetHost("http://test.it");
        service.setTargetPath("/test");
        service.setTargetPort(8080l);
        station1.setServicePof(service);
        Station station2 = new Station();
        station2.setBrokerCode("broker2");
        station2.setStationCode("station2");
        station2.setServicePof(service);
        stations.put("station1",station1);
        stations.put("station2",station2);
        configDataV1.setStations(stations);



        when(configService.getCache()).thenReturn(configDataV1);
//        when(cosmosStationRepository.getStations()).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now().minusSeconds(600)),new CosmosStandInStation("","station2", Instant.now().minusSeconds(600))));
        when(restTemplate.exchange(any(), any(Class.class))).thenReturn(new ResponseEntity<String>("", HttpStatus.OK));
        when(cosmosClient.getDatabase(any())).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(any())).thenReturn(cosmosContainer);
//        when(cosmosContainer.executeBulkOperations(any())).thenReturn(null);
        when(cosmosContainer.queryItems(any(SqlQuerySpec.class),any(),any())).thenReturn(cosmosPagedIterable);
        when(cosmosPagedIterable.stream()).thenReturn(Arrays.asList(
                new CosmosStandInStation("","station1",Instant.now()),
                new CosmosStandInStation("","station2",Instant.now())
        ).stream());

        org.springframework.test.util.ReflectionTestUtils.setField(cosmosNodeDataRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationDataRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosEventsRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "cosmosEventsRepository", cosmosEventsRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "url", "http://forwarder.it");
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "key", "key");
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "restTemplate", restTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "forwarderClient", forwarderClient);
//        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "cosmosNodeDataRepository", cosmosNodeDataRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "cosmosStationDataRepository", cosmosStationDataRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "cosmosStationRepository", cosmosStationRepository);
//        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "cosmosEventsRepository", cosmosEventsRepository);
    }

    @Test
    void test1() throws Exception {
        stationMonitorService.checkStations();
        Thread.sleep(2000);
        verify(cosmosStationDataRepository, times(2)).save(any());
    }
}
