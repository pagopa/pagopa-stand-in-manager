from behave import *
import logging
import requests
import utils as utils
from xml.dom.minidom import parseString

# Constants
RESPONSE = "RES"
REQUEST = "REQ"


@given('systems up')
def step_impl(context):
    """
        health check for defined systems
    """
    responses = True

    if "systems up" not in context.precondition_cache:

        for key in context.config.userdata.get("services"):
            row = context.config.userdata.get("services").get(key)

            url = row.get("url") + row.get("healthcheck")
            logging.debug(f"calling: {row.get('name')} -> {url}")
            subscription_key = row.get("subscription_key")
            headers = {'Content-Type': 'application/json'}
            if subscription_key is not None:
                headers['Ocp-Apim-Subscription-Key'] = subscription_key
            resp = requests.get(url, headers=headers, verify=False)
            logging.debug(f"response: {resp.status_code}")
            responses &= (resp.status_code == 200)

        if responses:
            context.precondition_cache.add("systems up")

    assert responses, f"health-check systems or subscription-key errors"


@step('initial SOAP {primitive} request with the {payload_name}')
def step_impl(context, primitive, payload_name):
    payload = context.text or ""
    payload = utils.replace_local_variables(payload, context)
    payload = utils.replace_global_variables(payload, context)
    setattr(context, primitive + "_" + payload_name, payload)


@given('a notice number named {notice_number_name} with aux digit {aux_digit:d}, {code} as code and ends with {end_digits:d}')
def step_impl(context, notice_number_name, aux_digit, code, end_digits):
    if aux_digit == 0 or aux_digit == 3:
        # iuv = f"11{random.randint(10000000000, 99999999999)}00"
        iuv = utils.get_random_string(12)
        notice_number = f"{aux_digit}{code}{iuv}{end_digits}"
    elif aux_digit == 1 or aux_digit == 2:
        # iuv = random.randint(10000000000000000, 99999999999999999)
        iuv = utils.get_random_string(17)
        notice_number = f"{aux_digit}{iuv}"
    else:
        assert False

    setattr(context, notice_number_name, notice_number)


@when('{partner} sends {primitive} SOAP request to {node_microservice} with {payload}')
def step_impl(context, partner, primitive, node_microservice, payload):
    subscription_key = utils.get_subscription_key(context, node_microservice)
    headers = {
        'Content-Type': 'text-xml',
        'SOAPAction': primitive
    }
    if subscription_key is not None:
        headers['Ocp-Apim-Subscription-Key'] = subscription_key

    endpoint_info = utils.get_soap_nodo_url(primitive)
    endpoint = utils.replace_local_variables(endpoint_info.get("endpoint"), context)
    endpoint = utils.replace_global_variables(endpoint, context)
    endpoint_info["endpoint"] = endpoint
    url = utils.get_url(context, node_microservice) + endpoint

    if hasattr(context, "query_params"):
        query_params = getattr(context, "query_params")
        delattr(context, "query_params")
        url += "?" + query_params

    data = None
    if payload != 'None':
        data = getattr(context, primitive + "_" + payload)
    response = utils.execute_request(url=url, method=endpoint_info.get("method"), headers=headers, payload=data)
    setattr(context, primitive + RESPONSE, response)


@then('{partner} receives the HTTP status code {http_status_code} to {primitive} SOAP request')
def step_impl(context, partner, http_status_code, primitive):
    response = getattr(context, primitive + RESPONSE)
    result = response.status_code == int(http_status_code)
    if not result:
        logging.info(f"status_code {response.status_code}, expected {http_status_code}, content {response.content}")
    assert result, f"status_code {response.status_code}, expected {http_status_code}"


@then('{partner} receives {tag_value} as {tag_name} in {primitive} SOAP response')
def step_impl(context, partner, tag_value, tag_name, primitive):
    response = getattr(context, primitive + RESPONSE)
    my_document = parseString(response.content)
    print(response.content)
    if len(my_document.getElementsByTagName(tag_name)) > 0:
        data = my_document.getElementsByTagName(tag_name)[0].firstChild.data
        assert data == tag_value, f"actual: {data}, expected: {tag_value}"
    else:
        assert False, f"{tag_name} not found"
