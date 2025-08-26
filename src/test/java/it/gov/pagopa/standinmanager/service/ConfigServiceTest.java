package it.gov.pagopa.standinmanager.service;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.azure.messaging.eventhubs.models.PartitionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.model.CacheEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.api.CacheApi;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private CacheApi cacheApi;

    @InjectMocks
    private ConfigService configService;

    @Mock
    private EventHubConsumerAsyncClient consumer;

    @Mock
    private EventHubClientBuilder mockBuilder;

    @Mock
    private Disposable subscription;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(configService, "connectionString", "mock-connection-string");
        ReflectionTestUtils.setField(configService, "consumerGroup", "mock-consumer-group");
        ReflectionTestUtils.setField(configService, "eventHubName", "mock-eventhub-name");
    }

    @Test
    void test1() throws Exception {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        Map<String, Station> stations = new HashMap<>();
        Station station1 = new Station();
        Station station2 = new Station();
        stations.put("station1",station1);
        stations.put("station2",station2);
        configDataV1.setStations(stations);
        when(cacheApi.cache()).thenReturn(configDataV1);

        configService.loadCache();
        ConfigDataV1 cache = configService.getCache();
        verify(cacheApi, times(1)).cache();
        assertEquals(2, cache.getStations().size());
    }

    @Test
    void getCache_should_load_cache_on_first_call() {
        ConfigDataV1 mockData = new ConfigDataV1();
        mockData.setVersion("1.0.0");
        when(cacheApi.cache()).thenReturn(mockData);

        ConfigDataV1 result = configService.getCache();

        assertNotNull(result);
        assertEquals("1.0.0", result.getVersion());
        verify(cacheApi, times(1)).cache();
    }

    @Test
    void getCache_should_return_cached_data_if_version_matches() throws JsonProcessingException {
        String version = "1.0.0";
        ConfigDataV1 mockData = new ConfigDataV1();
        mockData.setVersion(version);
        CacheEvent mockEvent = new CacheEvent("cacheVersion", version, "timestamp");
        ReflectionTestUtils.setField(configService, "configData", mockData);
        ReflectionTestUtils.setField(configService, "cacheEvent", mockEvent);

        ConfigDataV1 result = configService.getCache();

        assertNotNull(result);
        assertEquals(version, result.getVersion());

        verify(cacheApi, never()).cache();
    }

    @Test
    void getCache_should_reload_cache_if_version_is_different() throws JsonProcessingException {
        String version = "1.0.0";
        ConfigDataV1 oldData = new ConfigDataV1();
        oldData.setVersion(version);
        ConfigDataV1 newData = new ConfigDataV1();
        newData.setVersion("2.0.0");
        CacheEvent mockEvent = new CacheEvent("cacheVersion","2.0.0", "timestamp");
        ReflectionTestUtils.setField(configService, "configData", oldData);
        ReflectionTestUtils.setField(configService, "cacheEvent", mockEvent);
        when(cacheApi.cache()).thenReturn(newData);

        ConfigDataV1 result = configService.getCache();

        assertNotNull(result);
        assertEquals("2.0.0", result.getVersion());
        verify(cacheApi, times(1)).cache();
    }

    @Test
    void loadCache_should_call_cacheApi_and_set_configData() {
        String version = "1.0.0";
        ConfigDataV1 mockData = new ConfigDataV1();
        mockData.setVersion(version);
        when(cacheApi.cache()).thenReturn(mockData);

        configService.loadCache();

        ConfigDataV1 configData = (ConfigDataV1) ReflectionTestUtils.getField(configService, "configData");
        assertNotNull(configData);
        assertEquals(version, configData.getVersion());
        verify(cacheApi, times(1)).cache();
    }

    @Test
    void postConstruct_should_subscribe_to_event_hub() throws Exception {
        when(consumer.getPartitionIds()).thenReturn(Flux.just("0", "1"));
        when(consumer.receiveFromPartition(anyString(), any(EventPosition.class)))
                .thenReturn(Flux.empty());

        ReflectionTestUtils.setField(configService, "consumer", consumer);

        configService.postConstruct();

        verify(consumer, times(1)).getPartitionIds();
        verify(consumer, times(2)).receiveFromPartition(anyString(), any(EventPosition.class));
    }

    @Test
    void postConstruct_should_process_new_event() throws Exception {
        String partitionId = "0";
        String eventBody = "{\"version\": \"1.0.0\"}";
        EventData mockEventData = new EventData(ByteBuffer.wrap(eventBody.getBytes(StandardCharsets.UTF_8)));

        PartitionEvent mockPartitionEvent = mock(PartitionEvent.class);
        when(mockPartitionEvent.getData()).thenReturn(mockEventData);
        when(consumer.getPartitionIds()).thenReturn(Flux.just(partitionId));
        when(consumer.receiveFromPartition(eq(partitionId), any(EventPosition.class)))
                .thenReturn(Flux.just(mockPartitionEvent));

        ReflectionTestUtils.setField(configService, "consumer", consumer);

        configService.postConstruct();

        CacheEvent cacheEvent = (CacheEvent) ReflectionTestUtils.getField(configService, "cacheEvent");
        assertNotNull(cacheEvent);
        assertEquals("1.0.0", cacheEvent.getVersion());
    }

    @Test
    void preDestroy_should_close_consumer_and_dispose_subscription() {
        ReflectionTestUtils.setField(configService, "consumer", consumer);
        ReflectionTestUtils.setField(configService, "subscription", subscription);
        when(subscription.isDisposed()).thenReturn(false);

        configService.preDestroy();

        verify(consumer, times(1)).close();
        verify(subscription, times(1)).dispose();
    }

    @Test
    void preDestroy_should_not_dispose_if_subscription_is_null() {
        ReflectionTestUtils.setField(configService, "consumer", consumer);
        ReflectionTestUtils.setField(configService, "subscription", null);

        configService.preDestroy();

        verify(consumer, times(1)).close();
        verifyNoInteractions(subscription);
    }

    @Test
    void getConsumer_should_initialize_client_with_correct_consumer_group() {
        ReflectionTestUtils.setField(configService, "consumer", null);

        EventHubClientBuilder mockBuilder = mock(EventHubClientBuilder.class);
        EventHubConsumerAsyncClient mockConsumer = mock(EventHubConsumerAsyncClient.class);

        when(mockBuilder.connectionString(any(), any())).thenReturn(mockBuilder);
        when(mockBuilder.consumerGroup(any())).thenReturn(mockBuilder);
        when(mockBuilder.buildAsyncConsumerClient()).thenReturn(mockConsumer);

        ConfigService configService = spy(new ConfigService());
        doReturn(mockBuilder).when(configService).createBuilder();

        ReflectionTestUtils.setField(configService, "connectionString", "mock-conn-string");
        ReflectionTestUtils.setField(configService, "eventHubName", "mock-event-hub-name");
        ReflectionTestUtils.setField(configService, "consumerGroup", "mock-consumer-group");

        EventHubConsumerAsyncClient result = configService.getConsumer();

        assertNotNull(result);
        assertEquals(mockConsumer, result);

        verify(mockBuilder).connectionString("mock-conn-string", "mock-event-hub-name");
        verify(mockBuilder).consumerGroup("mock-consumer-group");
        verify(mockBuilder).buildAsyncConsumerClient();
    }

}
