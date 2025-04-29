package it.gov.pagopa.standinmanager;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.microsoft.azure.kusto.data.Client;
import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Service;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.service.AsyncService;
import it.gov.pagopa.standinmanager.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncServiceTest {

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
    private AsyncService asyncService;

    @BeforeEach
    void setUp() {
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

        Map<String, StationCreditorInstitution> stationCreditorInstitutions = new HashMap<>();
        StationCreditorInstitution stationCreditorInstitution1 = new StationCreditorInstitution();
        stationCreditorInstitution1.setStationCode(station1.getStationCode());
        stationCreditorInstitution1.setCreditorInstitutionCode("creditorInstitution");
        stationCreditorInstitutions.put(station1.getStationCode() + "_creditorInstitution",stationCreditorInstitution1);
        configDataV1.setCreditorInstitutionStations(stationCreditorInstitutions);

        StationCreditorInstitution stationCreditorInstitution2 = new StationCreditorInstitution();
        stationCreditorInstitution2.setStationCode(station1.getStationCode());
        stationCreditorInstitution2.setCreditorInstitutionCode("creditorInstitution");
        stationCreditorInstitutions.put(station2.getStationCode() + "_creditorInstitution",stationCreditorInstitution1);
        configDataV1.setCreditorInstitutionStations(stationCreditorInstitutions);

        when(cosmosClient.getDatabase(any())).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(any())).thenReturn(cosmosContainer);

        org.springframework.test.util.ReflectionTestUtils.setField(cosmosNodeDataRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationDataRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosEventsRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "cosmosEventsRepository", cosmosEventsRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "url", "http://forwarder.it");
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "key", "key");
        org.springframework.test.util.ReflectionTestUtils.setField(forwarderClient, "restTemplate", restTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(asyncService, "forwarderClient", forwarderClient);
    }

    @Test
    void test1() {
        Station station  = new Station();
        station.setStationCode("station1");

        StationCreditorInstitution stationCreditorInstitution = new StationCreditorInstitution();

        asyncService.checkStation(ZonedDateTime.now(), station, stationCreditorInstitution, CosmosStandInStation.builder().build());
        verify(cosmosStationDataRepository, times( 1)).save(any());
    }
}
