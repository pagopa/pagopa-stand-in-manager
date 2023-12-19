package it.gov.pagopa.standinmanager.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CosmosStationRepository {

  @Autowired private CosmosClient cosmosClient;

  @Value("${cosmos.db.name}")
  private String dbname;

  public static String tablename = "stand_in_stations";

  private CosmosPagedIterable<CosmosStandInStation> query(SqlQuerySpec query) {
    log.info("executing query:" + query.getQueryText());
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.queryItems(
        query, new CosmosQueryRequestOptions(), CosmosStandInStation.class);
  }

  public CosmosItemResponse<CosmosStandInStation> save(CosmosStandInStation item) {
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.createItem(item);
  }

  public List<CosmosStandInStation> getStations() {
    SqlQuerySpec q = new SqlQuerySpec("SELECT * FROM c");
    return query(q).stream().collect(Collectors.toList());
  }

  public List<CosmosStandInStation> removeStation(String station) {
    SqlQuerySpec q = new SqlQuerySpec("SELECT * FROM c");
    return query(q).stream().collect(Collectors.toList());
  }
}
