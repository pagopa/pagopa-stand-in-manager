Feature: Verify ko of payments
#  Execute a verifyPaymentNotice to not-responding station

#  Background:
#    Given systems up

  @runnable
  Scenario: Send verifyPaymentNotice - PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE
    Given a notice number named notice_number with aux digit 3, 04 as code and ends with 999
    And initial SOAP verifyPaymentNotice request with the payload
      """
      <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:nod="http://pagopa-api.pagopa.gov.it/node/nodeForPsp.xsd">
         <soapenv:Header/>
         <soapenv:Body>
            <nod:verifyPaymentNoticeReq>
               <idPSP>#psp#</idPSP>
               <idBrokerPSP>#broker_psp#</idBrokerPSP>
               <idChannel>#channel#</idChannel>
               <password>#channel_password#</password>
               <qrCode>
                  <fiscalCode>#organization#</fiscalCode>
                  <noticeNumber>$notice_number$</noticeNumber>
               </qrCode>
            </nod:verifyPaymentNoticeReq>
         </soapenv:Body>
      </soapenv:Envelope>
      """
    When PSP sends verifyPaymentNotice SOAP request to nodo with payload
    Then PSP receives the HTTP status code 200 to verifyPaymentNotice SOAP request
    And PSP receives PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE as faultCode in verifyPaymentNotice SOAP response


#  PPT_STAZIONE_INT_PA_TIMEOUT
#  PPT_STAZIONE_INT_PA_SERVIZIO_NON_ATTIVO