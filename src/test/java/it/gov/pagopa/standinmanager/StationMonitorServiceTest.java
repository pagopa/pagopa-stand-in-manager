package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
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
    @Mock private CosmosNodeDataRepository cosmosNodeDataRepository = mock(CosmosNodeDataRepository.class);
    @Mock private CosmosStationDataRepository cosmosStationDataRepository = mock(CosmosStationDataRepository.class);
    @Mock private CosmosStationRepository cosmosStationRepository = mock(CosmosStationRepository.class);
    @Mock private CosmosEventsRepository cosmosEventsRepository = mock(CosmosEventsRepository.class);


    private ForwarderClient forwarderClient = new ForwarderClient();
    @Mock private ConfigService configService = mock(ConfigService.class);
    @Mock private RestTemplate restTemplate;


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

        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "cosmosEventsRepository", cosmosEventsRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "url", "http://forwarder.it");
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "key", "key");
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "restTemplate", restTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "forwarderClient", forwarderClient);

        when(configService.getCache()).thenReturn(configDataV1);
        when(cosmosStationRepository.getStations()).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now().minusSeconds(600)),new CosmosStandInStation("","station2", Instant.now().minusSeconds(600))));
        when(restTemplate.exchange(any(), any(Class.class))).thenReturn(new ResponseEntity<String>("", HttpStatus.OK));
    }

    @Test
    void test1() throws Exception {
        stationMonitorService.checkStations();
        Thread.sleep(2000);
        verify(cosmosStationDataRepository, times(2)).save(any());
    }
}
