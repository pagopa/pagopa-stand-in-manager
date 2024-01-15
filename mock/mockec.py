import json
import ssl
import sys
import time
import xml.etree.ElementTree as et

import tornado.ioloop
import tornado.web
from tornado.log import enable_pretty_logging

enable_pretty_logging()


class ResponseHandler(tornado.web.RequestHandler):

    def set_default_headers(self):
        self.set_header("Content-Type", 'text/xml')

    def get(self):
        print("get request received")
        self.write("get request received")

    def post(self):
        # print("post request received")
        body = self.request.body.decode()
        root = et.fromstring(body)
        notice_number = root.find(".//noticeNumber").text
        if notice_number.endswith("999"):
            time.sleep(10)
            response = """<soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:common="http://pagopa-api.pagopa.gov.it/xsd/common-types/v1.0.0/" xmlns:nfp="http://pagopa-api.pagopa.gov.it/node/nodeForPsp.xsd">
                    <soapenv:Body>
                        <nfp:verifyPaymentNoticeRes>
                            <outcome>KO</outcome>
                            <fault>
                                <faultCode>PAA_PAGAMENTO_SCONOSCIUTO</faultCode>
                                <faultString>Pagamento sconosciuto.</faultString>
                                <id>NodoDeiPagamentiSPC</id>
                                <description/>
                            </fault>
                        </nfp:verifyPaymentNoticeRes>
                    </soapenv:Body>
            """
        elif notice_number == "000000000000000000":
            response = """<soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:common="http://pagopa-api.pagopa.gov.it/xsd/common-types/v1.0.0/" xmlns:nfp="http://pagopa-api.pagopa.gov.it/node/nodeForPsp.xsd">
                    <soapenv:Body>
                        <nfp:verifyPaymentNoticeRes>
                            <outcome>KO</outcome>
                            <fault>
                                <faultCode>PAA_PAGAMENTO_SCONOSCIUTO</faultCode>
                                <faultString>Pagamento sconosciuto.</faultString>
                                <id>NodoDeiPagamentiSPC</id>
                                <description/>
                            </fault>
                        </nfp:verifyPaymentNoticeRes>
                    </soapenv:Body>
            """
        else:
            response = """<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:paf="http://pagopa-api.pagopa.gov.it/pa/paForNode.xsd">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <paf:paVerifyPaymentNoticeRes>
                            <outcome>OK</outcome>
                            <paymentList>
                                <paymentOptionDescription>
                                    <amount>10.00</amount>
                                    <options>EQ</options>
                                    <dueDate>2021-12-31</dueDate>
                                    <detailDescription>test</detailDescription>
                                    <allCCP>1</allCCP>
                                </paymentOptionDescription>
                            </paymentList>
                            <paymentDescription>test</paymentDescription>
                            <fiscalCodePA>77777777777</fiscalCodePA>
                            <companyName>PagoPA</companyName>
                            <officeName>office</officeName>
                        </paf:paVerifyPaymentNoticeRes>
                    </soapenv:Body>
                </soapenv:Envelope>
            """
        self.set_status(200)
        self.write(response)


def make_app():
    return tornado.web.Application([
        (r"/", ResponseHandler),
        (r"/servizi/PagamentiTelematiciRPT", ResponseHandler),
    ])


if __name__ == "__main__":
    default_port = '8089' if len(sys.argv) == 1 else sys.argv[1]
    port = int(default_port)
    app = make_app()

    # http_server = tornado.httpserver.HTTPServer(app, ssl_options={
    #     "certfile": "./mockec_new/live/mockec.ddns.net/cert.pem",
    #     "keyfile": "./mockec_new/live/mockec.ddns.net/privkey.pem",
    # })

    # certificato scaduto
    # http_server = tornado.httpserver.HTTPServer(app, ssl_options={
    #     "certfile": "./mockec.ddns.net/cert.pem",
    #     "keyfile": "./mockec.ddns.net/privkey.pem",
    # })

    # mTLS
    ssl_ctx = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
    ssl_ctx.load_cert_chain("./mockec_new/live/mockec.ddns.net/combined_cert.pem", "./mockec_new/live/mockec.ddns.net/privkey.pem")
    # If your certs are not self-signed, load your CA certificates here.
    #ssl_ctx.load_verify_locations("cacerts.pem")
    # ssl_ctx.verify_mode = ssl.CERT_REQUIRED
    ssl_ctx.verify_mode = ssl.CERT_NONE
    http_server = tornado.httpserver.HTTPServer(app, ssl_options=ssl_ctx)

    http_server.listen(port)
    # app.listen(port)
    print(f"mockec running on port {default_port} ...")
    tornado.ioloop.IOLoop.current().start()
