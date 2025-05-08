package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.config.model.ConfigDataV1;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.utils.Either;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodoMonitorService {

    private static final String STATIONS_FILTER_PLACEHOLDER = "{stationsFilter}";
    private static final String FAULT_QUERY =
            "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n"
                    + "FAULT_CODE\n"
                    + STATIONS_FILTER_PLACEHOLDER
                    + "| where faultCode in ('PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE','PPT_STAZIONE_INT_PA_TIMEOUT','PPT_STAZIONE_INT_PA_SERVIZIO_NON_ATTIVO')\n"
                    + "| where tipoEvento in ('verifyPaymentNotice','activatePaymentNotice', 'activatePaymentNoticeV2')\n"
                    + "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"
                    + "| summarize count = count() by (stazione)";
    private static final String TOTALS_QUERY =
            "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n"
                    + "ReEvent\n"
                    + STATIONS_FILTER_PLACEHOLDER
                    + "| where tipoEvento in ('paVerifyPaymentNotice','paGetPayment', 'paGetPaymentV2')\n"
                    + "| where sottoTipoEvento == 'REQ'\n"
                    + "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"
                    + "| summarize count = count() by (stazione)";

    private final String database;
    private final int slotMinutes;
    private final String excludedStations;
    private final Client kustoClient;
    private final CosmosNodeDataRepository cosmosRepository;
    private final ConfigService configService;

    public NodoMonitorService(
            @Value("${dataexplorer.dbName}") String database,
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

    public void getAndSaveData() throws DataServiceException, DataClientException {
        ZonedDateTime now = ZonedDateTime.now();
        log.info("getAndSaveData [{}]", now);
        Set<String> excludedStationsList = getExcludedStationsList();

        Map<String, Integer> totalCallForStation = getCount(TOTALS_QUERY, Either.right(excludedStationsList), now.minusMinutes(this.slotMinutes));
        Set<String> allStations = totalCallForStation.keySet();
        Map<String, Integer> faultForStation = getCount(FAULT_QUERY, Either.left(allStations), now.minusMinutes(slotMinutes));
        Integer totalCall = totalCallForStation.values().stream().reduce(0, Integer::sum);

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

    private Map<String, Integer> getCount(
            String query,
            Either<Set<String>, Set<String>> includedOrExcludedStations,
            ZonedDateTime timeLimit
    ) throws DataServiceException, DataClientException {

        String replacedQuery = null;
        Optional<Set<String>> inclusionList = includedOrExcludedStations.left();
        Optional<Set<String>> exclusionList = includedOrExcludedStations.right();
        if (exclusionList.isPresent() && !exclusionList.get().isEmpty()) {
            String stations = exclusionList.get().stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            replacedQuery = query.replace(STATIONS_FILTER_PLACEHOLDER, "| where stazione !in(" + stations + ")\n");
        } else if (inclusionList.isPresent() && !inclusionList.get().isEmpty()) {
            String stations = inclusionList.get().stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            replacedQuery = query.replace(STATIONS_FILTER_PLACEHOLDER, "| where stazione in (" + stations + ")\n");
        } else {
            replacedQuery = query.replace(STATIONS_FILTER_PLACEHOLDER, "");
        }

        log.debug("Running KQL query [{}]", replacedQuery);
        KustoOperationResult response =
                this.kustoClient.execute(this.database, replacedQuery, getTimeParameters(timeLimit));
        KustoResultSetTable primaryResults = response.getPrimaryResults();
        Map<String, Integer> results = new HashMap<>();
        while (primaryResults.hasNext()) {
            primaryResults.next();
            results.put(primaryResults.getString("stazione"), primaryResults.getInt("count"));
        }
        return results;
    }

    private @NotNull Set<String> getExcludedStationsList() {
        ConfigDataV1 cache = this.configService.getCache();
        Set<String> excludedStationsList = new HashSet<>(cache.getStations().keySet());

        if (this.excludedStations != null && !this.excludedStations.isEmpty()) {
            excludedStationsList.addAll(Arrays.asList(this.excludedStations.split(",")));
        }
        return excludedStationsList;
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
}
