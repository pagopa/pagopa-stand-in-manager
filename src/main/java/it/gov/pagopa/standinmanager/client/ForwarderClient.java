package it.gov.pagopa.standinmanager.client;

import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
          + "         <idPA>00000000000</idPA>\n"
          + "         <idBrokerPA>{idBrokerPA}</idBrokerPA>\n"
          + "         <idStation>{idStation}</idStation>\n"
          + "         <qrCode>\n"
          + "            <fiscalCode>00000000000</fiscalCode>\n"
          + "            <noticeNumber>000000000000000000</noticeNumber>\n"
          + "         </qrCode>\n"
          + "      </paf:paVerifyPaymentNoticeReq>\n"
          + "   </soapenv:Body>\n"
          + "</soapenv:Envelope>";

  public boolean verifyPaymentNotice(Station station) {
    cosmosEventsRepository.newEvent(
        Constants.EVENT_FORWARDER_CALL,
        String.format("call forwarder for station [%s]", station.getStationCode()));
    final RequestEntity.BodyBuilder requestBuilder =
        RequestEntity.method(
            HttpMethod.POST, UriComponentsBuilder.fromHttpUrl(url).build().toUri());

    requestBuilder.header("Ocp-Apim-Subscription-Key", key);
    requestBuilder.header("X-Host-Url", station.getServicePof().getTargetHost());
    requestBuilder.header("X-Host-Port", station.getServicePof().getTargetPort() + "");
    requestBuilder.header("X-Host-Path", station.getServicePof().getTargetPath());
    requestBuilder.header("SOAPAction", "paVerifyPaymentNotice");

    String replacedBody =
        paVerifyRequestBody
            .replace("{idBrokerPA}", station.getBrokerCode())
            .replace("{idStation}", station.getStationCode());

    RequestEntity<String> body = requestBuilder.body(paVerifyRequestBody);
    ResponseEntity<String> responseEntity = null;
    try {
      responseEntity = restTemplate.exchange(body, String.class);
    } catch (HttpStatusCodeException e) {
      cosmosEventsRepository.newEvent(
          Constants.EVENT_FORWARDER_CALL_RESP_ERROR,
          String.format(
              "call forwarder for station [%s] returned",
              station.getStationCode(), e.getStatusCode()));
      return false;
    }

    if (responseEntity.getStatusCode().is2xxSuccessful()
        && responseEntity.getBody().contains("SCONOSCIUTO")) {
      cosmosEventsRepository.newEvent(
          Constants.EVENT_FORWARDER_CALL_RESP_SUCCCESS,
          String.format(
              "call forwarder for station [%s] returned SCONOSCIUTO", station.getStationCode()));
      return true;
    } else {
      cosmosEventsRepository.newEvent(
          Constants.EVENT_FORWARDER_CALL_RESP_ERROR,
          String.format(
              "call forwarder for station [%s] did not return SCONOSCIUTO",
              station.getStationCode()));
      return false;
    }
  }
}
