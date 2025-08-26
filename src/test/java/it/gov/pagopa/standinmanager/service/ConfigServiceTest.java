package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.api.CacheApi;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private CacheApi cacheApi;

    @InjectMocks
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        Map<String, Station> stations = new HashMap<>();
        Station station1 = new Station();
        Station station2 = new Station();
        stations.put("station1",station1);
        stations.put("station2",station2);
        configDataV1.setStations(stations);
        when(cacheApi.cache()).thenReturn(configDataV1);
    }

    @Test
    void test1() throws Exception {
        configService.loadCache();
        ConfigDataV1 cache = configService.getCache();
        verify(cacheApi, times(1)).cache();
        assertEquals(2, cache.getStations().size());
    }
}
