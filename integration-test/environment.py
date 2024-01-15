import json
import os
import logging

import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def before_all(context):
    # initialize precondition cache to avoid check systems up for each scenario
    context.precondition_cache = set()

    context.config.setup_logging()

    logging.debug('Global settings: loading configuration')

    more_userdata = json.load(open(os.path.join(context.config.base_dir + "/config/config.json")))
    for key, cfg in more_userdata.get("services").items():
        if cfg.get("subscription_key") is not None:
            cfg["subscription_key"] = os.getenv(cfg["subscription_key"])

    context.config.update_userdata(more_userdata)

