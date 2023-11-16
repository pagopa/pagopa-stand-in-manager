package it.gov.pagopa.standinmanager.scheduler;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.service.NodoMonitorService;
import it.gov.pagopa.standinmanager.service.StationMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URISyntaxException;
import java.time.LocalDateTime;

@Configuration
@Slf4j
@EnableScheduling
public class ScheduledJobs {

    @Autowired
    private NodoMonitorService nodoMonitorService;

    @Autowired
    private StationMonitorService stationMonitorService;

    @Scheduled(cron = "${nodo.monitor.cron}")
    public void checkNodo() throws InterruptedException, URISyntaxException, DataServiceException, DataClientException,DataServiceException {
        log.info("[Scheduled] Starting nodo monitor check {}", LocalDateTime.now());
        nodoMonitorService.getAndSaveData();
    }

    @Scheduled(cron = "${station.monitor.cron}")
    public void checkStation() throws InterruptedException, URISyntaxException, DataServiceException, DataClientException,DataServiceException {
        log.info("[Scheduled] Starting stations monitor check {}", LocalDateTime.now());
        stationMonitorService.checkStations();
    }



}
