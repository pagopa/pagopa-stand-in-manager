package it.gov.pagopa.standinmanager.client;

import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Component
public class ForwarderClient {

  @Value("${forwarder.url}")
  private String url;

  @Value("${forwarder.key}")
  private String key;

  @Autowired private RestTemplate restTemplate;
  @Autowired private CosmosEventsRepository cosmosEventsRepository;

  private static String paVerifyRequestBody =
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
          + " xmlns:paf=\"http://pagopa-api.pagopa.gov.it/pa/paForNode.xsd\">\n"
          + "   <soapenv:Header/>\n"
          + "   <soapenv:Body>\n"
          + "      <paf:paVerifyPaymentNoticeReq>\n"
          + "         <idPA>{idPA}</idPA>\n"
          + "         <idBrokerPA>{idBrokerPA}</idBrokerPA>\n"
          + "         <idStation>{idStation}</idStation>\n"
          + "         <qrCode>\n"
          + "            <fiscalCode>{idPA}</fiscalCode>\n"
          + "            <noticeNumber>000000000000000000</noticeNumber>\n"
          + "         </qrCode>\n"
          + "      </paf:paVerifyPaymentNoticeReq>\n"
          + "   </soapenv:Body>\n"
          + "</soapenv:Envelope>";

  public boolean paVerifyPaymentNotice(Station station, StationCreditorInstitution creditorInstitution) {
    log.info("paVerifyPaymentNotice to station [{}]", station.getStationCode());
    cosmosEventsRepository.newEvent(
            station.getStationCode(),
            Constants.EVENT_FORWARDER_CALL,
            String.format(
                    "call forwarder for station [%s]\n[%s:%s%s]",
                    station.getStationCode(),
                    station.getServicePof().getTargetHost(),
                    station.getServicePof().getTargetPort(),
                    station.getServicePof().getTargetPath()));

    RequestEntity<String> request = buildSoapRequest(station, creditorInstitution);

    try {
      ResponseEntity<String> responseEntity = restTemplate.exchange(request, String.class);
      if (responseEntity.getStatusCode().is2xxSuccessful() &&
              responseEntity.getBody().contains("SCONOSCIUTO")) {
        cosmosEventsRepository.newEvent(
                station.getStationCode(),
                Constants.EVENT_FORWARDER_CALL_RESP_SUCCCESS,
                String.format("call forwarder for station [%s] returned SCONOSCIUTO", station.getStationCode()));
        return true;
      } else {
        cosmosEventsRepository.newEvent(
                station.getStationCode(),
                Constants.EVENT_FORWARDER_CALL_RESP_ERROR,
                String.format("call forwarder for station [%s] did not return SCONOSCIUTO", station.getStationCode()));
        return false;
      }
    } catch (Exception e) {
      cosmosEventsRepository.newEvent(
              station.getStationCode(),
              Constants.EVENT_FORWARDER_CALL_RESP_ERROR,
              String.format("call forwarder for station [%s] returned [%s]", station.getStationCode(), e.getMessage()));
      return false;
    }
  }

  public String testPaVerifyPaymentNotice(Station station, StationCreditorInstitution creditorInstitution) {
    log.info("paVerifyPaymentNotice to station [{}]", station.getStationCode());
    RequestEntity<String> request = buildSoapRequest(station, creditorInstitution);
    try {
      ResponseEntity<String> responseEntity = restTemplate.exchange(request, String.class);
      return responseEntity.getBody();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private RequestEntity<String> buildSoapRequest(Station station, StationCreditorInstitution creditorInstitution) {
    final RequestEntity.BodyBuilder requestBuilder = RequestEntity
            .method(HttpMethod.POST, UriComponentsBuilder.fromHttpUrl(url).build().toUri())
            .header("Content-Type", "text/xml")
            .header("Ocp-Apim-Subscription-Key", key)
            .header("X-Host-Url", station.getServicePof().getTargetHost())
            .header("X-Host-Port", Optional.ofNullable(station.getServicePof().getTargetPort())
                    .map(Object::toString).orElse("443"))
            .header("X-Host-Path", station.getServicePof().getTargetPath())
            .header("SOAPAction", "\"paVerifyPaymentNotice\"");

    String replacedBody = paVerifyRequestBody
            .replace("{idPA}", creditorInstitution.getCreditorInstitutionCode())
            .replace("{idBrokerPA}", station.getBrokerCode())
            .replace("{idStation}", station.getStationCode());

    return requestBuilder.body(replacedBody, String.class);
  }
}
