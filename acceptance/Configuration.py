import os
from ConfigParser import ConfigParser

path = os.path.join(os.path.dirname(__file__), 'acceptance.cfg')
if not (os.path.exists(path) and os.path.isfile(path) and os.access(path, os.R_OK)):
    raise IOError('property must be a valid path: %s' % path)
cfg = ConfigParser()
cfg.read(path)

ORGANIZATION_NAME = cfg.get("information", "organization_name")
SPACE_NAME = cfg.get("information", "space_name")
SERVICE_NAME = cfg.get("information", "service_name")
PLAN_NAME = cfg.get("information", "plan_name")
TESTED_APP_NAME = cfg.get("information", "application_name")
SERVICE_INSTANCE_NAME = cfg.get("information", "service_instance_name")


CLIENT_ENDPOINT = cfg.get("client", "target_endpoint")
CLIENT_SKIP_SSL = cfg.get("client", "skip_ssl_verification").lower() == "true"
CLIENT_USER = cfg.get("client", "username")
CLIENT_PASSWORD = cfg.get("client", "password")


AUTOSLEEP_ENDPOINT = cfg.get("autosleep", "target_endpoint")
USER_NAME = cfg.get("autosleep", "user")
USER_PASSWORD = cfg.get("autosleep", "password")
