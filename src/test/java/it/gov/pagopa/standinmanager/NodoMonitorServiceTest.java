package it.gov.pagopa.standinmanager;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.service.NodoMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodoMonitorServiceTest {


    private final CosmosNodeDataRepository cosmosNodeDataRepository = spy(new CosmosNodeDataRepository());
    @Mock
    private Client kustoClient;
    @Mock
    private CosmosClient cosmosClient;
    @Mock
    private CosmosDatabase cosmosDatabase;
    @Mock
    private CosmosContainer cosmosContainer;
//    @Autowired
//    private CosmosNodeDataRepository cosmosNodeDataRepository = spy(CosmosNodeDataRepository.class);
    @InjectMocks
    private NodoMonitorService nodoMonitorService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException, IOException {

        String s = new String(getClass().getClassLoader().getResourceAsStream("kustoResponse.json").readAllBytes());
        org.springframework.test.util.ReflectionTestUtils.setField(
                cosmosNodeDataRepository, "cosmosClient", cosmosClient);
        when(cosmosClient.getDatabase(any())).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(any())).thenReturn(cosmosContainer);
        when(cosmosContainer.executeBulkOperations(any())).thenReturn(null);
        when(kustoClient.execute(any(), any(), any()))
                .thenReturn(
                        new KustoOperationResult(s, "v2"));
//        when(cosmosNodeDataRepository.saveAll(any())).thenReturn(new ArrayIterator<>(null));
    }

    @Test
    void test1() throws Exception {
        nodoMonitorService.getAndSaveData();
//        verify(cosmosNodeDataRepository, times(1)).saveAll(any());
    }

    @Test
    void test2() throws Exception {

        org.springframework.test.util.ReflectionTestUtils.setField(nodoMonitorService, "excludedStations", "00000000000_01");
        nodoMonitorService.getAndSaveData();
//        verify(cosmosNodeDataRepository, times(1)).saveAll(any());
    }
}
