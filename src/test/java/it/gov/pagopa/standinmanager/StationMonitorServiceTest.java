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
import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Service;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import it.gov.pagopa.standinmanager.service.AsyncService;
import it.gov.pagopa.standinmanager.service.ConfigService;
import it.gov.pagopa.standinmanager.service.StationMonitorService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StationMonitorServiceTest {

    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosDatabase cosmosDatabase;
    @Mock private CosmosContainer cosmosContainer;
    @Mock private CosmosPagedIterable cosmosPagedIterable;
    @Mock private ConfigService configService = mock(ConfigService.class);
    @Mock private RestTemplate restTemplate;
    @Mock private ForwarderClient forwarderClient;

    private CosmosNodeDataRepository cosmosNodeDataRepository = spy(new CosmosNodeDataRepository());
    private CosmosStationRepository cosmosStationRepository = spy(new CosmosStationRepository());
    private CosmosEventsRepository cosmosEventsRepository = spy(CosmosEventsRepository.class);

    @Mock
    AsyncService asyncService;

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

        when(configService.getCache()).thenReturn(configDataV1);
        when(cosmosClient.getDatabase(any())).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(any())).thenReturn(cosmosContainer);
        when(cosmosContainer.queryItems(any(SqlQuerySpec.class),any(),any())).thenReturn(cosmosPagedIterable);
        when(cosmosPagedIterable.stream()).thenReturn(Arrays.asList(
                new CosmosStandInStation("","station1",Instant.now()),
                new CosmosStandInStation("","station2",Instant.now())
        ).stream());

        org.springframework.test.util.ReflectionTestUtils.setField(cosmosNodeDataRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosStationRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(cosmosEventsRepository, "cosmosClient", cosmosClient);
        org.springframework.test.util.ReflectionTestUtils.setField(stationMonitorService, "cosmosStationRepository", cosmosStationRepository);
    }

    @Test
    void test1() throws Exception {
        stationMonitorService.checkStations();
        Thread.sleep(2000);
        verify(asyncService, times(2)).checkStation(any(), any(), any(), any());
    }

    @Test
    void test2() {
        stationMonitorService.testStation("station1");
        verify(forwarderClient, times(1)).testPaVerifyPaymentNotice(any(), any());
    }

    @Test
    void test3() throws Exception {
        when(cosmosPagedIterable.stream()).thenReturn(Arrays.asList(
                new CosmosStandInStation("","station1",Instant.now())
        ).stream());
        stationMonitorService.checkStations();
        Thread.sleep(2000);
        verify(asyncService, times(1)).checkStation(any(), any(), any(), any());
    }
}
