package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.client.ForwarderClient;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.util.ArrayIterator;

@ExtendWith(MockitoExtension.class)
public class StationMonitorServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosNodeDataRepository cosmosNodeDataRepository = mock(CosmosNodeDataRepository.class);
    @Mock private CosmosStationDataRepository cosmosStationDataRepository = mock(CosmosStationDataRepository.class);
    @Mock private CosmosStationRepository cosmosStationRepository = mock(CosmosStationRepository.class);
    @Mock private ForwarderClient forwarderClient = mock(ForwarderClient.class);
    @Mock private ConfigService configService = mock(ConfigService.class);


    @InjectMocks
    private StationMonitorService stationMonitorService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        Map<String, Station> stations = new HashMap<>();
        Station station1 = new Station();
        Station station2 = new Station();
        stations.put("station1",station1);
        stations.put("station2",station2);
        configDataV1.setStations(stations);
//        org.springframework.test.util.ReflectionTestUtils.setField(
//                cosmosNodeDataRepository, "cosmosClient", cosmosClient);
    when(configService.getCache()).thenReturn(configDataV1);

        when(cosmosStationRepository.getStations()).thenReturn(List.of(new CosmosStandInStation("","station1", Instant.now().minusSeconds(600)),new CosmosStandInStation("","station2", Instant.now().minusSeconds(600))));
        when(forwarderClient.verifyPaymentNotice(any())).thenReturn(true);

    }

    @Test
    void test1() throws Exception {
        stationMonitorService.checkStations();
        Thread.sleep(2000);
        verify(cosmosStationDataRepository, times(2)).save(any());
    }
}
