x=1
while [ $x -le 6 ]
do
  echo "Execution ${x}"
  behave --tags=runnable,midRunnable --summary --show-timings -v

#  curl --location 'https://api.dev.platform.pagopa.it/nodo/node-for-psp/v1' \
#  --header 'SOAPAction: verifyPaymentNotice' \
#  --header 'Content-Type: application/xml' \
#  --data '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:nod="http://pagopa-api.pagopa.gov.it/node/nodeForPsp.xsd">
#      <soapenv:Header/>
#      <soapenv:Body>
#        <nod:verifyPaymentNoticeReq>
#          <idPSP>60000000001</idPSP>
#          <idBrokerPSP>60000000001</idBrokerPSP>
#          <idChannel>60000000001_01</idChannel>
#          <password>pwdpwdpwd</password>
#          <qrCode>
#            <fiscalCode>15376371009</fiscalCode>
#            <noticeNumber>3047328${number}96205999</noticeNumber>
#          </qrCode>
#        </nod:verifyPaymentNoticeReq>
#      </soapenv:Body>
#    </soapenv:Envelope>'
  x=$(( $x + 1 ))
  sleep 300
done

echo "Long run executed"
