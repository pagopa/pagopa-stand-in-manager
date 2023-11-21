package it.gov.pagopa.standinmanager.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.standinmanager.repository.model.ForwarderCallCounts;
import it.gov.pagopa.standinmanager.repository.model.NodeCallCounts;
import it.gov.pagopa.standinmanager.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CosmosStationDataRepository {

  @Autowired
  private CosmosClient cosmosClient;

  public static String dbname = "db";
  public static String tablename = "stationData";


  private CosmosPagedIterable<ForwarderCallCounts> query(SqlQuerySpec query) {
    log.info("executing query:" + query.getQueryText());
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.queryItems(query, new CosmosQueryRequestOptions(), ForwarderCallCounts.class);
  }

  public CosmosItemResponse<ForwarderCallCounts> save(ForwarderCallCounts item) {
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.createItem(item);
  }

  public List<ForwarderCallCounts> getStationCounts(ZonedDateTime dateFrom) {
    List<SqlParameter> paramList = new ArrayList<>();
    paramList.addAll(Arrays.asList(
            new SqlParameter("@from", dateFrom.toInstant())
    ));
    SqlQuerySpec q =
            new SqlQuerySpec(
                    "SELECT * FROM c where c.timestamp >= @from"
            )
                    .setParameters(paramList);
    return query(q).stream().collect(Collectors.toList());
  }

}
