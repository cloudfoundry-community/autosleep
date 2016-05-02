import logging
import requests
from requests.auth import HTTPBasicAuth
# hide underneath logs
logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.WARN)


class Autosleep(object):
    ROBOT_LIBRARY_SCOPE = "GLOBAL"

    def __init__(self, endpoint, username, password, application_name):
        self.endpoint = endpoint
        self.application_name = application_name
        self.username = username
        self.password = password

    def should_not_be_known_by_service(self):
        response = requests.get('%s/api/applications/' % self.endpoint,
                                auth=HTTPBasicAuth(self.username, self.password))
        for application in response.json()['body']:
            if application['name'] == self.application_name :
                raise AssertionError('%s is still listed in the application' % self.application_name)



