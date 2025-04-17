package it.gov.pagopa.standinmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.data.exceptions.KustoServiceQueryError;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.service.ConfigService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.api.CacheApi;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private CacheApi cacheApi;

    @InjectMocks
    private ConfigService configService;

    @BeforeEach
    void setUp() throws KustoServiceQueryError, DataServiceException, DataClientException {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        Map<String, Station> stations = new HashMap<>();
        Station station1 = new Station();
        Station station2 = new Station();
        stations.put("station1",station1);
        stations.put("station2",station2);
        configDataV1.setStations(stations);
        when(cacheApi.cache(false)).thenReturn(configDataV1);

    }

    @Test
    void test1() throws Exception {
        configService.loadCache();
        ConfigDataV1 cache = configService.getCache();
        verify(cacheApi, times(1)).cache(false);
        assertEquals(cache.getStations().size(),2);
    }
}
