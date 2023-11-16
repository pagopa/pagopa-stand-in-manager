package it.gov.pagopa.standinmanager.service;

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.repository.BlacklistStationsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosDataClient;
import it.gov.pagopa.standinmanager.repository.StandInStationsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodoMonitorService {

    @Value("${dataexplorer.url}")
    private String dataExplorerUrl;

    @Value("${dataexplorer.clientId}")
    private String dataExplorerClientId;

    @Value("${dataexplorer.appKey}")
    private String dataExplorerKey;

    @Value("${dataexplorer.dbName}")
    private String database = "NetDefaultDB";

    private String FAULT_QUERY = "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n" +
            "FAULT_CODE\n{stationsFilter}" +
            "| where faultCode in ('PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE','PPT_STAZIONE_INT_PA_TIMEOUT','PPT_STAZIONE_INT_PA_SERVIZIO_NON_ATTIVO')\n" +
            "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"+
            "| summarize count = count() by (stazione)";
    private String TOTALS_QUERY = "declare query_parameters(year:int,month:int,day:int,hour:int,minute:int,second:int);\n" +
            "ReEvent\n{stationsFilter}" +
            "| where tipoEvento in ('paVerifyPaymentNotice','paGetPayment')\n" +
            "| where sottoTipoEvento == 'REQ'\n" +
            "| where insertedTimestamp > make_datetime(year,month,day,hour,minute,second)\n"+
            "| summarize count = count() by (stazione)";

    @Autowired
    private StandInStationsRepository standInStationsRepository;
    @Autowired
    private BlacklistStationsRepository blacklistStationsRepository;
    @Autowired
    private Client kustoClient;
    @Autowired
    private CosmosDataClient cosmosDataClient;



    private ClientRequestProperties getTimeParameters(ZonedDateTime time){
        ClientRequestProperties clientRequestProperties = new ClientRequestProperties();
        clientRequestProperties.setParameter("year",time.getYear());
        clientRequestProperties.setParameter("month",time.getMonthValue());
        clientRequestProperties.setParameter("day",time.getDayOfMonth());
        clientRequestProperties.setParameter("hour",time.getHour());
        clientRequestProperties.setParameter("minute",time.getMinute());
        clientRequestProperties.setParameter("second",time.getSecond());
        return clientRequestProperties;
    }

    private Map<String,Integer> getCount(String query,List<String> filterStations,ZonedDateTime timelimit) throws URISyntaxException, DataServiceException, DataClientException {
        log.info("Running query [{}]", query);
        String replacedQuery = null;
        if(!filterStations.isEmpty()){
            String stations = String.join(",",filterStations.stream().map(s -> "'" + s + "'").collect(Collectors.toList()));
            replacedQuery = query.replace("{stationsFilter}","\n| where not(stazione in ("+stations+"))\n");
        }else{
            replacedQuery = query.replace("{stationsFilter}","");
        }

        KustoOperationResult response = kustoClient.execute(database,replacedQuery, getTimeParameters(timelimit));
        KustoResultSetTable primaryResults = response.getPrimaryResults();
        Map<String,Integer> results = new HashMap<>();
        while(primaryResults.hasNext()){
            primaryResults.next();
            results.put(primaryResults.getString("stazione"),primaryResults.getInt("count"));
        }
        return results;
    }

    public void check() throws URISyntaxException, DataServiceException, DataClientException {
        List<String> excludedStations = blacklistStationsRepository.findAll().stream().map(s->s.getStation()).collect(Collectors.toList());
        ZonedDateTime now = ZonedDateTime.now();
        Map<String, Integer> totals = getCount(TOTALS_QUERY,excludedStations,now.minusYears(5));
        Map<String, Integer> faults = getCount(FAULT_QUERY,excludedStations,now.minusYears(5));
        Map<String, Double> faultsPerc = new HashMap<>();
        totals.forEach((station,totalCount)->{
            Integer faultCount = faults.getOrDefault(station, 0);
            faultsPerc.put(station,((faultCount/(double)totalCount) * 100));
        });
        log.info("totals:{}",totals);
        log.info("faults:{}",faults);
        log.info("faultsPerc:{}",faultsPerc);
    }

}
