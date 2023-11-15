package it.gov.pagopa.standinmanager.repository;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.standinmanager.repository.model.StationCounts;
import it.gov.pagopa.standinmanager.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CosmosDataClient {

  @Value("${cosmos.endpoint}")
  private String endpoint;

  @Value("${cosmos.key}")
  private String key;

  public static String dbname = "db";
  public static String tablename = "data";

  private com.azure.cosmos.CosmosClient client;


  private com.azure.cosmos.CosmosClient getClient() {
    if (client == null) {
      client = new CosmosClientBuilder().endpoint(endpoint).key(key).buildClient();
    }
    return client;
  }

  private CosmosPagedIterable<StationCounts> query(SqlQuerySpec query) {
    log.info("executing query:" + query.getQueryText());
    CosmosContainer container = getClient().getDatabase(dbname).getContainer(tablename);
    return container.queryItems(query, new CosmosQueryRequestOptions(), StationCounts.class);
  }

  public List<StationCounts> getStationCounts(
      List<String> stations,
      LocalDate dateFrom) {
    List<SqlParameter> paramList = new ArrayList<>();
    paramList.addAll(Arrays.asList(
            new SqlParameter("@stations", stations),
            new SqlParameter("@from", Util.format(dateFrom))
        ));
    SqlQuerySpec q =
        new SqlQuerySpec(
                "SELECT * FROM c where"
                    + " c.stazione in @stations"
                    + " and c.datetime >= @from"
        )
            .setParameters(paramList);
    return query(q).stream().collect(Collectors.toList());
  }

}
