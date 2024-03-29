package it.gov.pagopa.standinmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.service.*;
import it.gov.pagopa.standinmanager.util.Constants;
import java.time.ZonedDateTime;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class TestController {

  @Autowired private MailService mailService;
  @GetMapping("/test-email")
  public ResponseEntity testEmail() {
    String s = mailService.sendEmail("test subject", "test message");
    return ResponseEntity.status(HttpStatus.OK).body(s);
  }

  @Autowired
  NodoCalcService nodoCalcService;
  @Autowired
  NodoMonitorService nodoMonitorService;
  @Autowired
  StationCalcService stationCalcService;
  @Autowired
  StationMonitorService stationMonitorService;
  @Autowired
  EventHubService eventHubService;

  @SneakyThrows
  @GetMapping("/test-1")
  public ResponseEntity test1() {
    nodoMonitorService.getAndSaveData();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }
  @SneakyThrows
  @GetMapping("/test-2")
  public ResponseEntity test2() {
    nodoCalcService.runCalculations();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }
  @SneakyThrows
  @GetMapping("/test-3")
  public ResponseEntity test3() {
    stationMonitorService.checkStations();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }
  @SneakyThrows
  @GetMapping("/test-4")
  public ResponseEntity test4() {
    stationCalcService.runCalculations();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }

  @SneakyThrows
  @GetMapping("/test-event")
  public ResponseEntity test5() {
    try {
      eventHubService.publishEvent(ZonedDateTime.now(), "test", Constants.type_added);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }
}
