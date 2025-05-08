package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import it.gov.pagopa.standinmanager.util.TestUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = NodoMonitorService.class)
class NodoMonitorServiceTest {

    @MockBean
    private CosmosNodeDataRepository cosmosNodeDataRepositoryMock;
    @Captor
    ArgumentCaptor<List<CosmosNodeCallCounts>> cosmosNodeCallCountsCaptor;
    @MockBean
    private Client kustoClientMock;
    @MockBean
    private ConfigService configServiceMock;

    @Autowired
    private NodoMonitorService sut;

    @Test
    void getAndSaveDataSuccess() throws Exception {
        doReturn(buildConfigDataV1()).when(configServiceMock).getCache();
        doReturn(getKustoResult("response/kustoTotalResponse.json")).when(kustoClientMock).execute(any(), any(), any());
        doReturn(getKustoResult("response/kustoFaultResponse.json")).when(kustoClientMock).execute(any(), any(), any());

        assertDoesNotThrow(() -> sut.getAndSaveData());

        verify(cosmosNodeDataRepositoryMock).saveAll(cosmosNodeCallCountsCaptor.capture());
        List<CosmosNodeCallCounts> savedCounts = cosmosNodeCallCountsCaptor.getValue();
        assertNotNull(savedCounts);
        assertFalse(savedCounts.isEmpty());
        assertEquals(2, savedCounts.size());
    }

    @Test
    void getAndSaveDataWithExcludedStationsSuccess() throws Exception {
        ReflectionTestUtils.setField(sut, "excludedStations", "00000000000_01");

        doReturn(buildConfigDataV1()).when(configServiceMock).getCache();
        doReturn(getKustoResult("response/kustoTotalResponse.json")).when(kustoClientMock).execute(any(), any(), any());
        doReturn(getKustoResult("response/kustoFaultResponse.json")).when(kustoClientMock).execute(any(), any(), any());

        assertDoesNotThrow(() -> sut.getAndSaveData());

        verify(cosmosNodeDataRepositoryMock).saveAll(cosmosNodeCallCountsCaptor.capture());
        List<CosmosNodeCallCounts> savedCounts = cosmosNodeCallCountsCaptor.getValue();
        assertNotNull(savedCounts);
        assertFalse(savedCounts.isEmpty());
        assertEquals(2, savedCounts.size());
    }

    private ConfigDataV1 buildConfigDataV1() {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        configDataV1.setStations(Map.of("Stazione3", new Station()));
        return configDataV1;
    }

    private KustoOperationResult getKustoResult(String relativePath) throws KustoServiceQueryError, IOException {
        return new KustoOperationResult(TestUtil.readJsonFromFile(relativePath), "v2");
    }
}
