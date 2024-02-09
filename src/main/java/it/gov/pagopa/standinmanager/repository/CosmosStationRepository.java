package it.gov.pagopa.standinmanager.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    return container.queryItems(query, new CosmosQueryRequestOptions(), CosmosStandInStation.class);
  }

  private void delete(CosmosStandInStation station) {
    log.info("deleting station:" + station.getStation());
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    container.deleteItem(station, new CosmosItemRequestOptions());
  }

  public CosmosItemResponse<CosmosStandInStation> save(CosmosStandInStation item) {
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.createItem(item);
  }

  public List<CosmosStandInStation> getStations() {
    SqlQuerySpec q = new SqlQuerySpec("SELECT * FROM c");
    return query(q).stream().collect(Collectors.toList());
  }

  public List<CosmosStandInStation> getStation(String station) {
    SqlQuerySpec q = new SqlQuerySpec("SELECT * FROM c where c.station = @station");
    List<SqlParameter> paramList = new ArrayList<>();
    paramList.addAll(Arrays.asList(new SqlParameter("@station", station)));
    return query(q.setParameters(paramList)).stream().collect(Collectors.toList());
  }

  public Boolean removeStation(CosmosStandInStation station) {
    try {
      delete(station);
      return true;
    } catch (Exception e) {
      log.error("error removing station", e);
    }
    return false;
  }
}
