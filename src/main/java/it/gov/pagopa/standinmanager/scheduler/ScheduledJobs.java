package it.gov.pagopa.standinmanager.scheduler;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.service.*;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
@EnableScheduling
public class ScheduledJobs {

  @Autowired private ConfigService configService;

  @Autowired private NodoMonitorService nodoMonitorService;

  @Autowired private NodoCalcService nodoCalcService;

  @Autowired private StationMonitorService stationMonitorService;

  @Autowired private StationCalcService stationCalcService;

  @Scheduled(cron = "${config.refresh.cron:-}")
  public void refreshCache()
      throws InterruptedException,
          URISyntaxException,
          DataServiceException,
          DataClientException,
          DataServiceException {
    log.info("[Scheduled] Starting config refresh {}", ZonedDateTime.now());
    configService.loadCache();
  }

  @Scheduled(cron = "${nodo.monitor.cron:-}")
  public void checkNodo()
      throws InterruptedException,
          URISyntaxException,
          DataServiceException,
          DataClientException,
          DataServiceException {
    log.info("[Scheduled] Starting nodo monitor check {}", ZonedDateTime.now());
    nodoMonitorService.getAndSaveData();
  }

  @Scheduled(cron = "${nodo.calc.cron:-}")
  public void calcNodo()
      throws InterruptedException,
          URISyntaxException,
          DataServiceException,
          DataClientException,
          DataServiceException {
    log.info("[Scheduled] Starting nodo calc {}", ZonedDateTime.now());
    nodoCalcService.runCalculations();
  }

  @Scheduled(cron = "${station.monitor.cron:-}")
  public void checkStation()
      throws InterruptedException,
          URISyntaxException,
          DataServiceException,
          DataClientException,
          DataServiceException {
    log.info("[Scheduled] Starting stations monitor check {}", ZonedDateTime.now());
    stationMonitorService.checkStations();
  }

  @Scheduled(cron = "${station.calc.cron:-}")
  public void calcStation()
      throws InterruptedException,
          URISyntaxException,
          DataServiceException,
          DataClientException,
          DataServiceException {
    log.info("[Scheduled] Starting stations calc check {}", ZonedDateTime.now());
    stationCalcService.runCalculations();
  }
}
