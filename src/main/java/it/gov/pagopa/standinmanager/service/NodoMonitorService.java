package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.repository.CosmosNodeDataRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosNodeCallCounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.utils.Either;

import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodoMonitorService {

    private final String FAULT_QUERY =
            "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n"
                    + "FAULT_CODE\n"
                    + "{stationsFilter}| where faultCode in"
                    + " ('PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE','PPT_STAZIONE_INT_PA_TIMEOUT','PPT_STAZIONE_INT_PA_SERVIZIO_NON_ATTIVO')\n"
                    + "| where tipoEvento in ('verifyPaymentNotice','activatePaymentNotice', 'activatePaymentNoticeV2')\n"
                    + "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"
                    + "| summarize count = count() by (stazione)";
    private final String TOTALS_QUERY =
            "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n"
                    + "ReEvent\n{stationsFilter}"
                    + "| where tipoEvento in ('paVerifyPaymentNotice','paGetPayment', 'paGetPaymentV2')\n"
                    + "| where sottoTipoEvento == 'REQ'\n"
                    + "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"
                    + "| summarize count = count() by (stazione)";
    @Value("${dataexplorer.dbName}")
    private String database;
    @Value("${adder.slot.minutes}")
    private int slotMinutes;

    @Value("${excludedStations}")
    private String excludedStations;

    @Autowired
    private Client kustoClient;
    @Autowired
    private CosmosNodeDataRepository cosmosRepository;

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

    private Map<String, Integer> getCount(
            String query,
            Either<Set<String>, Set<String>> includedOrExcludedStations,
            ZonedDateTime timelimit)
            throws URISyntaxException, DataServiceException, DataClientException {

        String replacedQuery = null;
        Optional<Set<String>> inclusionList = includedOrExcludedStations.left();
        Optional<Set<String>> exclusionList = includedOrExcludedStations.right();
        if (exclusionList.isPresent() && !exclusionList.get().isEmpty()) {
            String stations = exclusionList.get().stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            replacedQuery = query.replace("{stationsFilter}", "| where stazione !in(" + stations + ")\n");
        } else if (inclusionList.isPresent() && !inclusionList.get().isEmpty()) {
            String stations = inclusionList.get().stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            replacedQuery = query.replace("{stationsFilter}", "| where stazione in (" + stations + ")\n");
        } else {
            replacedQuery = query.replace("{stationsFilter}", "");
        }

        log.debug("Running KQL query [{}]", replacedQuery);
        KustoOperationResult response =
                kustoClient.execute(database, replacedQuery, getTimeParameters(timelimit));
        KustoResultSetTable primaryResults = response.getPrimaryResults();
        Map<String, Integer> results = new HashMap<>();
        while (primaryResults.hasNext()) {
            primaryResults.next();
            results.put(primaryResults.getString("stazione"), primaryResults.getInt("count"));
        }
        return results;
    }

    public void getAndSaveData()
            throws URISyntaxException, DataServiceException, DataClientException {
        ZonedDateTime now = ZonedDateTime.now();
        log.info("getAndSaveData [{}]", now);
        Set<String> excludedStationsList = new HashSet<>();
        if (excludedStations != null && !excludedStations.isEmpty()) {
            excludedStationsList = Arrays.asList(excludedStations.split(",")).stream().collect(Collectors.toSet());
        }

        Map<String, Integer> totals = getCount(TOTALS_QUERY, Either.right(excludedStationsList), now.minusMinutes(slotMinutes));
        Set<String> allStations = totals.keySet();
        Map<String, Integer> faults = getCount(FAULT_QUERY, Either.left(allStations), now.minusMinutes(slotMinutes));
        List<CosmosNodeCallCounts> stationCounts =
                totals.entrySet().stream()
                        .map(
                                entry -> {
                                    Integer faultCount = faults.getOrDefault(entry.getKey(), 0);
                                    return CosmosNodeCallCounts.builder()
                                            .id(UUID.randomUUID().toString())
                                            .station(entry.getKey())
                                            .total(entry.getValue())
                                            .faults(faultCount)
                                            .timestamp(now.toInstant())
                                            .build();
                                })
                        .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            final StringBuilder totalsString = new StringBuilder();
            totals.entrySet().stream()
                    .forEach(
                            s -> {
                                totalsString.append(
                                        "\n" + s.getKey() + " calls:" + s.getValue() + " faults:" + faults.get(s));
                            });
            log.debug("totals:{}", totalsString);
        }
        cosmosRepository.saveAll(stationCounts);
    }
}
