package it.gov.pagopa.standinmanager.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CacheEvent {
  String cacheVersion;
  String version;
  String timestamp;
}
