package it.gov.pagopa.standinmanager.service;

import it.gov.pagopa.standinmanager.config.model.Station;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ForwarderClient {

    @Value("${forwarder.url}")
    private String url;
    @Value("${forwarder.key}")
    private String key;

    @Autowired
    private RestTemplate restTemplate;

    private String paVerifyRequestBody = "";

    public boolean verifyPaymentNotice(Station station){

        final RequestEntity.BodyBuilder requestBuilder = RequestEntity.method(
                HttpMethod.POST,
                UriComponentsBuilder.fromHttpUrl(url).build().toUri()
        );

        requestBuilder.header("Ocp-Apim-Subscription-Key",key);
        requestBuilder.header("X-Host-Url",station.getServicePof().getTargetHost());
        requestBuilder.header("X-Host-Port",station.getServicePof().getTargetPort()+"");
        requestBuilder.header("X-Host-Path",station.getServicePof().getTargetPath());
        requestBuilder.header("SOAPAction","paVerifyPaymentNotice");

        RequestEntity<String> body = requestBuilder.body(paVerifyRequestBody);
        ResponseEntity<String> responseEntity = restTemplate.exchange(body, String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody().contains("SCONOSCIUTO")) {
            return true;
        } else {
            return false;
        }
    }
}
