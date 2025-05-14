package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.exception.AppError;
import it.gov.pagopa.standinmanager.exception.AppException;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodoMonitorService {

    private static final String STATIONS_FILTER_PLACEHOLDER = "{stationsFilter}";
    private static final String FAULT_CALL_GROUP_BY_STATION_QUERY =
            "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n"
                    + "FAULT_CODE\n"
                    + STATIONS_FILTER_PLACEHOLDER
                    + "| where faultCode in ('PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE','PPT_STAZIONE_INT_PA_TIMEOUT','PPT_STAZIONE_INT_PA_SERVIZIO_NON_ATTIVO')\n"
                    + "| where tipoEvento in ('verifyPaymentNotice','activatePaymentNotice', 'activatePaymentNoticeV2')\n"
                    + "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"
                    + "| summarize count = count() by (stazione)";
    private static final String TOTAL_CALL_GROUP_BY_STATION_QUERY =
            "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n"
                    + "ReEvent\n"
                    + STATIONS_FILTER_PLACEHOLDER
                    + "| where tipoEvento in ('paVerifyPaymentNotice','paGetPayment', 'paGetPaymentV2')\n"
                    + "| where sottoTipoEvento == 'REQ'\n"
                    + "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"
                    + "| summarize count = count() by (stazione)";
    private static final String TOTAL_CALL_QUERY = """
            declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);
            ReEvent
            | where tipoEvento in ('paVerifyPaymentNotice','paGetPayment', 'paGetPaymentV2')
            | where sottoTipoEvento == 'REQ'
            | where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)
            | summarize count = count()""";

    private final String database;
    private final int slotMinutes;
    private final String excludedStations;
    private final Client kustoClient;
    private final CosmosNodeDataRepository cosmosRepository;
    private final ConfigService configService;

    public NodoMonitorService(
            @Value("${data.explorer.dbName}") String database,
            @Value("${adder.slot.minutes}") int slotMinutes,
            @Value("${excludedStations}") String excludedStations,
            Client kustoClient,
            CosmosNodeDataRepository cosmosRepository, ConfigService configService
    ) {
        this.database = database;
        this.slotMinutes = slotMinutes;
        this.excludedStations = excludedStations;
        this.kustoClient = kustoClient;
        this.cosmosRepository = cosmosRepository;
        this.configService = configService;
    }

    /**
     * Monitor traffic for each station that is eligible for Stand-In.
     * Retrieves station total call and fault in the configured slot time {@link NodoMonitorService#slotMinutes} and retrieve
     * the total call in the configured slot.
     * Saves the counts on CosmosDB.
     *
     * @throws DataServiceException Error on query data from Azure Data Explorer
     * @throws DataClientException  Error on query data from Azure Data Explorer
     */
    public void getAndSaveData() throws DataServiceException, DataClientException {
        ZonedDateTime now = ZonedDateTime.now();
        log.info("getAndSaveData [{}]", now);
        ZonedDateTime timeLimit = now.minusMinutes(this.slotMinutes);

        Integer totalCall = getTotalCallCount(timeLimit);

        IncludedOrExcludedStations includedOrExcludedStations = getStationsFilter();
        Map<String, Integer> totalCallForStation =
                getCountGroupedByStations(TOTAL_CALL_GROUP_BY_STATION_QUERY, includedOrExcludedStations, timeLimit);

        Set<String> stationsToInclude = totalCallForStation.keySet();
        Map<String, Integer> faultForStation =
                getCountGroupedByStations(FAULT_CALL_GROUP_BY_STATION_QUERY, new IncludedOrExcludedStations(stationsToInclude), timeLimit);

        List<CosmosNodeCallCounts> stationCounts =
                totalCallForStation.entrySet().stream()
                        .map(
                                entry -> {
                                    Integer faultCount = faultForStation.getOrDefault(entry.getKey(), 0);
                                    return CosmosNodeCallCounts.builder()
                                            .id(UUID.randomUUID().toString())
                                            .station(entry.getKey())
                                            .total(entry.getValue())
                                            .faults(faultCount)
                                            .allStationCallInSlot(totalCall)
                                            .timestamp(now.toInstant())
                                            .build();
                                })
                        .toList();

        if (log.isDebugEnabled()) {
            final StringBuilder totalsString = new StringBuilder();
            totalCallForStation.forEach((key, value) -> totalsString.append(
                    String.format("%n%s calls: %s faults: %s", key, value, faultForStation.get(key))));
            log.debug("totals:{}", totalsString);
        }
        this.cosmosRepository.saveAll(stationCounts);
    }

    private Map<String, Integer> getCountGroupedByStations(
            String query,
            IncludedOrExcludedStations includedOrExcludedStations,
            ZonedDateTime timeLimit
    ) throws DataServiceException, DataClientException {
        Set<String> inclusionList = includedOrExcludedStations.inclusionList;
        Set<String> exclusionList = includedOrExcludedStations.exclusionList;
        String replacedQuery;
        if (exclusionList != null && !exclusionList.isEmpty()) {
            String stations = exclusionList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            replacedQuery = query.replace(STATIONS_FILTER_PLACEHOLDER, "| where stazione !in(" + stations + ")\n");
        } else if (inclusionList != null && !inclusionList.isEmpty()) {
            String stations = inclusionList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            replacedQuery = query.replace(STATIONS_FILTER_PLACEHOLDER, "| where stazione in (" + stations + ")\n");
        } else {
            replacedQuery = query.replace(STATIONS_FILTER_PLACEHOLDER, "");
        }

        log.debug("Running KQL query [{}]", replacedQuery);
        KustoOperationResult response = this.kustoClient.execute(this.database, replacedQuery, getTimeParameters(timeLimit));
        KustoResultSetTable primaryResults = response.getPrimaryResults();
        Map<String, Integer> results = new HashMap<>();
        while (primaryResults.hasNext()) {
            primaryResults.next();
            results.put(primaryResults.getString("stazione"), primaryResults.getInt("count"));
        }
        return results;
    }

    private Integer getTotalCallCount(ZonedDateTime timeLimit) throws DataServiceException, DataClientException {
        log.debug("Running KQL query [{}]", TOTAL_CALL_QUERY);
        KustoOperationResult response = this.kustoClient.execute(this.database, TOTAL_CALL_QUERY, getTimeParameters(timeLimit));
        KustoResultSetTable primaryResults = response.getPrimaryResults();
        if (primaryResults.hasNext()) {
            primaryResults.next();
            return primaryResults.getInt("count");
        }
        throw new AppException(AppError.NO_RESULT_FROM_DATA_EXPLORER_QUERY);
    }

    private ClientRequestProperties getTimeParameters(ZonedDateTime time) {
        ClientRequestProperties clientRequestProperties = new ClientRequestProperties();
        clientRequestProperties.setParameter("year", time.getYear());
        clientRequestProperties.setParameter("month", time.getMonthValue());
        clientRequestProperties.setParameter("day", time.getDayOfMonth());
        clientRequestProperties.setParameter("hour", time.getHour());
        clientRequestProperties.setParameter("minute", time.getMinute());
        clientRequestProperties.setParameter("second", time.getSecond());
        return clientRequestProperties;
    }

    private IncludedOrExcludedStations getStationsFilter() {
        ConfigDataV1 cache = this.configService.getCache();
        Set<String> includedStations = cache.getStations().keySet();

        Set<String> excludedStationsList = new HashSet<>();
        if (this.excludedStations != null && !this.excludedStations.isEmpty()) {
            excludedStationsList = new HashSet<>(Arrays.asList(this.excludedStations.split(",")));
        }
        return new IncludedOrExcludedStations(includedStations, excludedStationsList);
    }

    private record IncludedOrExcludedStations(Set<String> inclusionList,
                                              Set<String> exclusionList) {
        public IncludedOrExcludedStations(Set<String> inclusionList) {
            this(inclusionList, null);
        }
    }
}
