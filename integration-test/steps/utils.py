import re
import os
import requests
import logging
import datetime
import contextlib
import string
import random
from http.client import HTTPConnection


def debug_requests_on():
    '''Switches on logging of the requests module.'''
    HTTPConnection.debuglevel = 1

    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)
    requests_log = logging.getLogger("requests.packages.urllib3")
    requests_log.setLevel(logging.DEBUG)
    requests_log.propagate = True


def debug_requests_off():
    '''Switches off logging of the requests module, might be some side-effects'''
    HTTPConnection.debuglevel = 0

    root_logger = logging.getLogger()
    root_logger.setLevel(logging.WARNING)
    root_logger.handlers = []
    requests_log = logging.getLogger("requests.packages.urllib3")
    requests_log.setLevel(logging.WARNING)
    requests_log.propagate = False


def get_global_conf(context, field):
    return context.config.userdata.get("global_configuration").get(field)


def replace_local_variables(payload, context):
    pattern = re.compile('\\$\\w+\\$')
    match = pattern.findall(payload)
    for field in match:
        value = getattr(context, field.replace('$', '').split('.')[0])
        payload = payload.replace(field, value)
    return payload


def replace_global_variables(payload, context):
    pattern = re.compile('#\\w+#')
    match = pattern.findall(payload)
    for field in match:
        name = field.replace('#', '').split('.')[0]
        if name in context.config.userdata.get("global_configuration"):
            value = get_global_conf(context, name)
            payload = payload.replace(field, value)
    return payload


def get_soap_nodo_url(primitive=-1):
    primitive_mapping = {
        "verificaBollettino": "/node-for-psp/v1",
        "verifyPaymentNotice": "/node-for-psp/v1",
        "activatePaymentNotice": "/node-for-psp/v1",
        "activatePaymentNoticeV2": "/node-for-psp/v1",
        "sendPaymentOutcome": "/node-for-psp/v1",
        "sendPaymentOutcomeV2": "/node-for-psp/v1",
        "activateIOPayment": "/node-for-io/v1",
        "nodoVerificaRPT": "/nodo-per-psp/v1",
        "nodoAttivaRPT": "/nodo-per-psp/v1",
        "nodoInviaFlussoRendicontazione": "/nodo-per-psp/v1",
        "nodoChiediElencoFlussiRendicontazione": "/nodo-per-pa/v1",
        "nodoChiediFlussoRendicontazione": "/nodo-per-pa/v1",
        "demandPaymentNotice": "/node-for-psp/v1",
        "nodoChiediCatalogoServizi": "/nodo-per-psp-richiesta-avvisi/v1",
        "nodoChiediCatalogoServiziV2": "/node-for-psp/v1",
        "nodoChiediCopiaRT": "/nodo-per-pa/v1",
        "nodoChiediInformativaPA": "/nodo-per-psp/v1",
        "nodoChiediListaPendentiRPT": "/nodo-per-pa/v1",
        "nodoChiediNumeroAvviso": "/nodo-per-psp-richiesta-avvisi/v1",
        "nodoChiediStatoRPT": "/nodo-per-pa/v1",
        "nodoChiediTemplateInformativaPSP": "/nodo-per-psp/v1",
        "nodoInviaCarrelloRPT": "/nodo-per-pa/v1",
        "nodoInviaRPT": "/nodo-per-pa/v1",
        "nodoInviaRT": "/nodo-per-psp/v1",
        "nodoPAChiediInformativaPA": "/nodo-per-pa/v1",
        "nodoChiediElencoQuadraturePSP": "/nodo-per-psp/v1",
        "nodoChiediInformativaPSP": "/nodo-per-pa/v1",
        "nodoChiediElencoQuadraturePA": "/nodo-per-pa/v1",
        "nodoChiediQuadraturaPA": "/nodo-per-pa/v1"
    }

    return {
        "endpoint": primitive_mapping.get(primitive),
        "method": "POST"
    }


def execute_request(url, method, headers, payload=None):
    debug_requests_on()
    req = requests.request(method=method, url=url, headers=headers, data=payload, verify=False)
    debug_requests_off()
    return req


def get_subscription_key(context, node_microservice):
    data = context.config.userdata.get("services").get(node_microservice)
    return data.get("subscription_key")


def get_url(context, node_microservice):
    data = context.config.userdata.get("services").get(node_microservice)
    return data.get("url")


# def generate_iuv():
#     return get_random_string(14)


def get_random_string(length):
    return ''.join(random.choice(string.digits) for _ in range(length))


def append_to_query_params(context, query_param):
    query_params = ""
    if hasattr(context, "query_params"):
        query_params = getattr(context, "query_params") + "&"
    query_params += query_param
    setattr(context, "query_params", query_params)
