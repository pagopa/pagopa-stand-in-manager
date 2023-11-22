package it.gov.pagopa.standinmanager.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import it.gov.pagopa.standinmanager.repository.model.CosmosEvent;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CosmosEventsRepository {

  @Autowired private CosmosClient cosmosClient;

  public static String dbname = "db";
  public static String tablename = "events";

  public void newEvent(String type, String info) {
    save(CosmosEvent.builder().timestamp(Instant.now()).info(info).type(type).build());
  }

  public CosmosItemResponse<CosmosEvent> save(CosmosEvent item) {
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.createItem(item);
  }
}
