package it.gov.pagopa.standinmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import it.gov.pagopa.standinmanager.client.MailService;
import it.gov.pagopa.standinmanager.service.*;
import it.gov.pagopa.standinmanager.util.Constants;
import java.time.ZonedDateTime;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
@Validated
public class InternalController {

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

  private MailService mailService;

  @Operation(summary = "Sends test email to the configured addresses")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @GetMapping(value = { "/email"})
  public ResponseEntity<String> testEmail() {
    String s = mailService.sendEmail("test subject", "test message");
    return ResponseEntity.status(HttpStatus.OK).body(s);
  }

  @Operation(summary = "Retrieves data from dataExplorer and save it in node_data")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @SneakyThrows
  @GetMapping(value = { "/node-data"})
  public ResponseEntity<String> test1() {
    nodoMonitorService.getAndSaveData();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }

  @Operation(summary = "Evaluates data in node_data and stores the aggregate in station_data")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @SneakyThrows
  @GetMapping(value = { "/run-calculations"})
  public ResponseEntity<String> test2() {
    nodoCalcService.runCalculations();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }

  @Operation(summary = "Sends probe request to stations in stand-in")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @SneakyThrows
  @GetMapping(value = { "/check-stations"})
  public ResponseEntity<String> test3() {
    stationMonitorService.checkStations();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }

  @Operation(summary = "Evaluates data in station_data and add/remove station from stand_in_stations")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @SneakyThrows
  @GetMapping(value = { "/station-data"})
  public ResponseEntity<String> test4() {
    stationCalcService.runCalculations();
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }

  @Operation(summary = "Sends a test event on the event-hub")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @GetMapping(value = { "/publish-event"})
  public ResponseEntity<String> test5() {
    try {
      eventHubService.publishEvent(ZonedDateTime.now(), "test", Constants.type_added);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return ResponseEntity.status(HttpStatus.OK).body("OK");
  }

  @Operation(summary = "Sends probe to station specified in input. No impact on the workflow.")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @SneakyThrows
  @GetMapping(value = { "/stations/{station}"})
  public ResponseEntity<String> test6(@PathVariable(name = "station") String stationCode) {
    return ResponseEntity.status(HttpStatus.OK).body(stationMonitorService.testStation(stationCode));
  }

  @Operation(summary = "Removes the station from stand-in manually. The operation is saved in the event registry.")
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "200", description = "OK response"),
                  @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
          })
  @SneakyThrows
  @DeleteMapping(value = { "/stations/{station}"})
  public ResponseEntity<String> removeStationFromStandIn(@PathVariable(name = "station") String stationCode) {
    stationCalcService.removeStationFromStandIn(stationCode);
    return ResponseEntity.ok().build();
  }

}
