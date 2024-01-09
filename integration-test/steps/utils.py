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


def get_fdr_url(request_type=""):
    request_type_mapping = {
        "create": {
            "endpoint": "/psps/#psp#/fdrs/$flow_name$",
            "method": "POST"
        },
        "add_payments": {
            "endpoint": "/psps/#psp#/fdrs/$flow_name$/payments/add",
            "method": "PUT"
        },
        "del_payments": {
            "endpoint": "/psps/#psp#/fdrs/$flow_name$/payments/del",
            "method": "PUT"
        },
        "del_flow": {
          "endpoint": "/psps/#psp#/fdrs/$flow_name$",
          "method": "DELETE"
        },
        "publish": {
            "endpoint": "/psps/#psp#/fdrs/$flow_name$/publish",
            "method": "POST"
        },
        "created_payments": {
            "endpoint": "/psps/#psp#/created/fdrs/$flow_name$/organizations/#organization#/payments",
            "method": "GET"
        },
        "created_fdr": {
            "endpoint": "/psps/#psp#/created/fdrs/$flow_name$/organizations/#organization#",
            "method": "GET"
        },
        "psp_get_published_payments": {
            "endpoint": "/psps/#psp#/published/fdrs/$flow_name$/revisions/$revision$/organizations/#organization#/payments",
            "method": "GET"
        },
        "psp_get_published_fdr": {
            "endpoint": "/psps/#psp#/published/fdrs/$flow_name$/revisions/$revision$/organizations/#organization#",
            "method": "GET"
        },
        "org_get_all_published_fdr": {
            "endpoint": "/organizations/#organization#/fdrs",
            "method": "GET"
        },
        "org_get_published_fdr": {
            "endpoint": "/organizations/#organization#/fdrs/$flow_name$/revisions/$revision$/psps/#psp#",
            "method": "GET"
        },
        "org_get_payments": {
            "endpoint": "/organizations/#organization#/fdrs/$flow_name$/revisions/$revision$/psps/#psp#/payments",
            "method": "GET"
        },
        "psp_get_all_published_fdr": {
            "endpoint": "/psps/#psp#/published",
            "method": "GET"
        },
        "org_get_all_published_fdr_by_psp": {
            "endpoint": "/organizations/#organization#/fdrs?pspId=#psp#&page=1&size=1000&publishedGt=$today_date$",
            "method": "GET"
        },
        "get_all_created": {
            "endpoint": "/psps/#psp#/created",
            "method": "GET"
        }
    }
    return request_type_mapping.get(request_type)


def execute_request(url, method, headers, payload=None):
    debug_requests_on()
    req = requests.request(method=method, url=url, headers=headers, data=payload, verify=False)
    debug_requests_off()
    return req


def get_subscription_key(context, partner):
    data = get_config_by_partner(context, partner)
    return data.get("subscription_key")


def get_url(context, partner):
    data = get_config_by_partner(context, partner)
    return data.get("url")


def get_config_by_partner(context, partner):
    key = "fdr-psp" if partner.lower() == "psp" else "fdr-org"
    return context.config.userdata.get("services").get(key)


def generate_iuv():
    return get_random_string(14)


def generate_iur():
    return get_random_string(10)


def get_random_string(length):
    return ''.join(random.choice(string.digits) for i in range(length))


def append_to_query_params(context, query_param):
    query_params = ""
    if hasattr(context, "query_params"):
        query_params = getattr(context, "query_params") + "&"
    query_params += query_param
    setattr(context, "query_params", query_params)


def get_yesterday():
    today = datetime.datetime.today().astimezone()
    return (today - datetime.timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S.%fZ")
