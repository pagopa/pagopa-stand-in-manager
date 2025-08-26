package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataService {

  @Autowired private CosmosStationRepository cosmosStationRepository;

  public List<String> getStations() {
    return cosmosStationRepository.getStations().stream()
        .map(s -> s.getStation())
        .collect(Collectors.toList());
  }
}
