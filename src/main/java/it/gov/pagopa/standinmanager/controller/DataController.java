package it.gov.pagopa.standinmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import it.gov.pagopa.standinmanager.model.GetResponse;
import it.gov.pagopa.standinmanager.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Slf4j
public class DataController {

  @Autowired private DataService dataService;

  @Operation(summary = "Get the list of standin stations")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Get the list of standin stations",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = GetResponse.class))
            }),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
      })
  @GetMapping("/stations")
  @Valid
  public ResponseEntity<GetResponse> getEvents() {
    return ResponseEntity.status(HttpStatus.OK).body(GetResponse.builder().stations(dataService.getStations()).build());
  }
}
