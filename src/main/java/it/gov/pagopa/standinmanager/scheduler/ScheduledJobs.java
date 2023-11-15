package it.gov.pagopa.standinmanager.scheduler;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import it.gov.pagopa.standinmanager.service.NodoMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Scheduled(fixedDelayString = "${nodo.monitor.fixedRate}")
    @ConditionalOnProperty(value = "monitor.enabled", matchIfMissing = true, havingValue = "true")
    public void check() throws InterruptedException, URISyntaxException, DataServiceException, DataClientException,DataServiceException {
        log.info("[Scheduled] Starting monitor check {}", LocalDateTime.now());
        nodoMonitorService.check();
    }

}
