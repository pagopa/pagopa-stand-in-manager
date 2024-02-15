package it.gov.pagopa.standinmanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClient;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.util.ArrayIterator;

@ExtendWith(MockitoExtension.class)
public class NodoMonitorServiceTest {



    @Mock private Client kustoClient;
    @Mock private CosmosClient cosmosClient;
    @Mock private CosmosNodeDataRepository cosmosNodeDataRepository = mock(CosmosNodeDataRepository.class);

    @InjectMocks
    private NodoMonitorService nodoMonitorService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
//        org.springframework.test.util.ReflectionTestUtils.setField(
//                cosmosNodeDataRepository, "cosmosClient", cosmosClient);
        when(kustoClient.execute(any(),any(),any())).thenReturn(new KustoOperationResult("{ \"Tables\":[ \"RE\"] }",""));
        when(cosmosNodeDataRepository.saveAll(any())).thenReturn(new ArrayIterator<>(null));
    }

    @Test
    void test1() throws Exception {
        nodoMonitorService.getAndSaveData();
        verify(cosmosNodeDataRepository, times(1)).saveAll(any());
    }
}
