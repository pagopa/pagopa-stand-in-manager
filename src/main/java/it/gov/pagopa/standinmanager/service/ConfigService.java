package it.gov.pagopa.standinmanager.service;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.model.CacheEvent;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.api.CacheApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service
public class ConfigService {

  private ConfigDataV1 configData;

    @Autowired private CacheApi cacheApi;

    @Value("${nodo-dei-pagamenti-cache-rx-connection-string}")
    private String connectionString;

    @Value("${nodo-dei-pagamenti-cache-rx-consumer-group}")
    private String consumerGroup;

    @Value("${nodo-dei-pagamenti-cache-rx-name}")
    private String eventHubName;

    private EventHubConsumerAsyncClient consumer;

    private CacheEvent cacheEvent;

    private Disposable subscription;

    private EventHubConsumerAsyncClient getConsumer() {
        if (consumer == null) {
            log.info("Cache consumer initialized");
            consumer =
                    new EventHubClientBuilder()
                            .connectionString(connectionString, eventHubName)
                            .consumerGroup(consumerGroup)
                            .buildAsyncConsumerClient();
        }
        return consumer;
    }

    public ConfigDataV1 getCache() {
        // non-blocking check for cache hit
        if (configData != null && (cacheEvent == null || configData.getVersion().equals(cacheEvent.getVersion()))) {
            return configData;
        }

        // double-checked locking to ensure thread-safe cache loading
        synchronized (this) {
            // re-check inside the synchronized block
            if (configData == null || !configData.getVersion().equals(cacheEvent.getVersion())) {
                log.info("Reload cache for new version {}", (cacheEvent != null) ? cacheEvent.getVersion() : "initial load");
                loadCache();
            }
        }
        return configData;
    }

    public ConfigDataV1 getCacheOLD() {
        if (cacheEvent != null) {
            // verify cache version
            if (configData != null && !configData.getVersion().equals(cacheEvent.getVersion())) {
                log.info("Reload cache for new version {}", cacheEvent.getVersion());
                loadCache();
            }
        }

        if (configData == null) {
            loadCache();
        }
        return configData;
    }

    public void loadCache() {
        log.info("loadCache from cache api");
        try {
            configData = cacheApi.cache();
        } catch (Exception e) {
            log.error("Can not get cache", e);
        }
    }

    @PostConstruct
    private void postConstruct() {
        getConsumer().getPartitionIds().subscribe(partitionId -> {
            subscription = getConsumer()
                    .receiveFromPartition(partitionId, EventPosition.latest()) // SOLO eventi futuri
                    .subscribe(event -> {
                        String body = event.getData().getBodyAsString();
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        try {
                            cacheEvent = mapper.readValue(body, CacheEvent.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        log.info(String.format("New cache obj on partitionId %s: %s%n", partitionId, body));
                        // cache is not reloaded here, but at first getCache() call
                        // this to avoid cache reload is busy due to another generation running
                        // loadCache();
                    }, error -> {
                        log.error(String.format("Error on partitionId " + partitionId + ": " + error));
                    });
        });
    }

    @PreDestroy
    private void preDestroy() {
        getConsumer().close();
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
  }
}
