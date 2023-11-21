package it.gov.pagopa.standinmanager.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.standinmanager.repository.model.NodeCallCounts;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CosmosNodeDataRepository {

  @Autowired private CosmosClient cosmosClient;

  public static String dbname = "db";
  public static String tablename = "nodeData";

  private CosmosPagedIterable<NodeCallCounts> query(SqlQuerySpec query) {
    log.info("executing query:" + query.getQueryText());
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.queryItems(query, new CosmosQueryRequestOptions(), NodeCallCounts.class);
  }

  public Iterable<CosmosBulkOperationResponse<Object>> saveAll(List<NodeCallCounts> items) {
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    List<CosmosItemOperation> cosmosItemOperationStream =
        items.stream()
            .map(
                s ->
                    CosmosBulkOperations.getCreateItemOperation(
                        s, new PartitionKey(s.getPartitionKey())))
            .collect(Collectors.toList());
    return container.executeBulkOperations(cosmosItemOperationStream);
  }

  public CosmosItemResponse<NodeCallCounts> save(NodeCallCounts item) {
    CosmosContainer container = cosmosClient.getDatabase(dbname).getContainer(tablename);
    return container.createItem(item);
  }

  public List<NodeCallCounts> getStationCounts(ZonedDateTime dateFrom) {
    List<SqlParameter> paramList = new ArrayList<>();
    paramList.addAll(Arrays.asList(new SqlParameter("@from", dateFrom.toInstant())));
    SqlQuerySpec q =
        new SqlQuerySpec("SELECT * FROM c where c.timestamp >= @from").setParameters(paramList);
    return query(q).stream().collect(Collectors.toList());
  }
}
