package it.gov.pagopa.standinmanager.scheduler;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.service.ConfigService;
import it.gov.pagopa.standinmanager.service.NodoCalcService;
import it.gov.pagopa.standinmanager.service.NodoMonitorService;
import it.gov.pagopa.standinmanager.service.StationCalcService;
import it.gov.pagopa.standinmanager.service.StationMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URISyntaxException;
import java.time.ZonedDateTime;

@Configuration
@Slf4j
@EnableScheduling
public class ScheduledJobs {

  private final ConfigService configService;
  private final NodoMonitorService nodoMonitorService;
  private final NodoCalcService nodoCalcService;
  private final StationMonitorService stationMonitorService;
  private final StationCalcService stationCalcService;

  public ScheduledJobs(
      ConfigService configService,
      NodoMonitorService nodoMonitorService,
      NodoCalcService nodoCalcService,
      StationMonitorService stationMonitorService,
      StationCalcService stationCalcService) {
    this.configService = configService;
    this.nodoMonitorService = nodoMonitorService;
    this.nodoCalcService = nodoCalcService;
    this.stationMonitorService = stationMonitorService;
    this.stationCalcService = stationCalcService;
  }

  @Scheduled(cron = "${config.refresh.cron:-}")
  public void refreshCache() {
    log.info("[Scheduled] Starting config refresh {}", ZonedDateTime.now());
    this.configService.loadCache();
  }

    @Scheduled(cron = "${nodo.monitor.cron:-}")
    public void checkNodo()
            throws
            DataClientException,
            DataServiceException {
        log.info("[Scheduled] Starting nodo monitor check {}", ZonedDateTime.now());
        this.nodoMonitorService.getAndSaveData();
    }

    @Scheduled(cron = "${nodo.calc.cron:-}")
    public void calcNodo() {
        log.info("[Scheduled] Starting nodo calc {}", ZonedDateTime.now());
        this.nodoCalcService.runCalculations();
    }

    @Scheduled(cron = "${station.monitor.cron:-}")
    public void checkStation() {
        log.info("[Scheduled] Starting stations monitor check {}", ZonedDateTime.now());
        this.stationMonitorService.checkStations();
    }

    @Scheduled(cron = "${station.calc.cron:-}")
    public void calcStation()
            throws URISyntaxException,
            DataClientException,
            DataServiceException {
        log.info("[Scheduled] Starting stations calc check {}", ZonedDateTime.now());
        this.stationCalcService.runCalculations();
    }
}
