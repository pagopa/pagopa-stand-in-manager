package it.gov.pagopa.standinmanager.service;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.standinmanager.util.Constants;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventHubService {

  @Value("${nodo-dei-pagamenti-stand-in-tx-connection-string}")
  private String connectionString;

  @Value("${nodo-dei-pagamenti-stand-in-tx-name}")
  private String eventHubName;

  private EventHubProducerClient producer;
  @Autowired private ObjectMapper om;

  private EventHubProducerClient getProducer() {
    if (producer == null) {
      producer =
          new EventHubClientBuilder()
              .connectionString(connectionString, eventHubName)
              .buildProducerClient();
    }
    return producer;
  }

  // https://learn.microsoft.com/en-us/azure/event-hubs/event-hubs-java-get-started-send?tabs=connection-string%2Croles-azure-portal#add-code-to-publish-events-to-the-event-hub
  public void publishEvent(ZonedDateTime now, String station, String type)
      throws JsonProcessingException {
    Map e = new HashMap();
    e.put(Constants.TIMESTAMP, now);
    e.put(Constants.STATION, station);
    e.put(Constants.TYPE, type);
    List<EventData> allEvents = Arrays.asList(new EventData(om.writeValueAsString(e)));
    EventDataBatch eventDataBatch = getProducer().createBatch();
    for (EventData eventData : allEvents) {
      if (!eventDataBatch.tryAdd(eventData)) {
        getProducer().send(eventDataBatch);
        eventDataBatch = getProducer().createBatch();
        if (!eventDataBatch.tryAdd(eventData)) {
          throw new IllegalArgumentException(
              "Event is too large for an empty batch. Max size: "
                  + eventDataBatch.getMaxSizeInBytes());
        }
      }
    }
    if (eventDataBatch.getCount() > 0) {
      producer.send(eventDataBatch);
    }
  }

  @PreDestroy
  private void preDestroy() {
    getProducer().close();
  }
}
