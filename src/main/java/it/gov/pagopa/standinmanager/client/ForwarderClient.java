package it.gov.pagopa.standinmanager.client;

import it.gov.pagopa.standinmanager.config.model.Station;
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

  private String paVerifyRequestBody =
      "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
          + " xmlns:paf=\"http://pagopa-api.pagopa.gov.it/pa/paForNode.xsd\">\n"
          + "   <soapenv:Header/>\n"
          + "   <soapenv:Body>\n"
          + "      <paf:paVerifyPaymentNoticeReq>\n"
          + "         <idPA>{idPA}</idPA>\n"
          + "         <idBrokerPA>{idBrokerPA}</idBrokerPA>\n"
          + "         <idStation>{idStation}</idStation>\n"
          + "         <qrCode>\n"
          + "            <fiscalCode>{fiscalCode}</fiscalCode>\n"
          + "            <noticeNumber>{noticeNumber}</noticeNumber>\n"
          + "         </qrCode>\n"
          + "      </paf:paVerifyPaymentNoticeReq>\n"
          + "   </soapenv:Body>\n"
          + "</soapenv:Envelope>";

  public boolean verifyPaymentNotice(Station station) {

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
            .replace("{idPA}", "")
            .replace("{idBrokerPA}", station.getBrokerCode())
            .replace("{idStation}", station.getStationCode())
            .replace("{fiscalCode}", "")
            .replace("{noticeNumber}", "000000000000000000");

    RequestEntity<String> body = requestBuilder.body(paVerifyRequestBody);
    ResponseEntity<String> responseEntity = null;
    try {
      responseEntity = restTemplate.exchange(body, String.class);
    } catch (HttpStatusCodeException e) {
      return false;
    }

    if (responseEntity.getStatusCode().is2xxSuccessful()
        && responseEntity.getBody().contains("SCONOSCIUTO")) {
      return true;
    } else {
      return false;
    }
  }
}
