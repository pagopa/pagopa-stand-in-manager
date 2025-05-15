package it.gov.pagopa.standinmanager.client;

import it.gov.pagopa.standinmanager.config.model.Service;
import it.gov.pagopa.standinmanager.config.model.Station;
import it.gov.pagopa.standinmanager.config.model.StationCreditorInstitution;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
class ForwarderClientTest {

    @InjectMocks
    private ForwarderClient forwarderClient;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CosmosEventsRepository cosmosEventsRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forwarderClient, "url", "http://test-url.com");
        ReflectionTestUtils.setField(forwarderClient, "key", "test-key");
    }

    private Station createMockStation() {
        Station station = new Station();
        station.setBrokerCode("broker1");
        station.setStationCode("station1");
        Service service = new Service();
        service.setTargetHost("http://test.it");
        service.setTargetPath("/test");
        service.setTargetPort(8080L);
        station.setServicePof(service);
        return station;
    }

    private StationCreditorInstitution createMockCreditorInstitution() {
        StationCreditorInstitution institution = new StationCreditorInstitution();
        institution.setCreditorInstitutionCode("CI123");
        return institution;
    }

    @Test
    void testPaVerifyPaymentNotice_SuccessfulWithSconosciuto() {
        Station station = createMockStation();
        StationCreditorInstitution institution = createMockCreditorInstitution();

        ResponseEntity<String> mockResponse = new ResponseEntity<>("<response>SCONOSCIUTO</response>", HttpStatus.OK);
        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(String.class)))
                .thenReturn(mockResponse);

        boolean result = forwarderClient.paVerifyPaymentNotice(station, institution);

        assertTrue(result);
        Mockito.verify(cosmosEventsRepository, Mockito.atLeastOnce())
                .newEvent(Mockito.eq(station.getStationCode()), Mockito.anyString(), Mockito.contains("SCONOSCIUTO"));
    }

    @Test
    void testPaVerifyPaymentNotice_SuccessfulWithoutSconosciuto() {
        Station station = createMockStation();
        StationCreditorInstitution institution = createMockCreditorInstitution();

        ResponseEntity<String> mockResponse = new ResponseEntity<>("<response>ALTRO</response>", HttpStatus.OK);
        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(String.class)))
                .thenReturn(mockResponse);

        boolean result = forwarderClient.paVerifyPaymentNotice(station, institution);

        assertFalse(result);
        Mockito.verify(cosmosEventsRepository).newEvent(Mockito.eq(station.getStationCode()), Mockito.eq(Constants.EVENT_FORWARDER_CALL_RESP_ERROR), Mockito.anyString());
    }

    @Test
    void testPaVerifyPaymentNotice_WithException() {
        Station station = createMockStation();
        StationCreditorInstitution institution = createMockCreditorInstitution();

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(String.class)))
                .thenThrow(new RestClientException("Connection error"));

        boolean result = forwarderClient.paVerifyPaymentNotice(station, institution);

        assertFalse(result);
        Mockito.verify(cosmosEventsRepository).newEvent(Mockito.eq(station.getStationCode()), Mockito.eq(Constants.EVENT_FORWARDER_CALL_RESP_ERROR), Mockito.contains("Connection error"));
    }

    @Test
    void testTestPaVerifyPaymentNotice_Success() {
        Station station = createMockStation();
        StationCreditorInstitution institution = createMockCreditorInstitution();

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(String.class)))
                .thenReturn(new ResponseEntity<>("<response>OK</response>", HttpStatus.OK));

        String response = forwarderClient.testPaVerifyPaymentNotice(station, institution);

        assertEquals("<response>OK</response>", response);
    }

    @Test
    void testTestPaVerifyPaymentNotice_WithException() {
        Station station = createMockStation();
        StationCreditorInstitution institution = createMockCreditorInstitution();

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(String.class)))
                .thenThrow(new RestClientException("Timeout"));

        String response = forwarderClient.testPaVerifyPaymentNotice(station, institution);

        assertEquals("Timeout", response);
    }
}
