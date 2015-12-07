import base64
import logging
import requests

# hide underneath logs
logging.getLogger('requests.packages.urllib3.connectionpool').setLevel(logging.WARN)


class Autosleep(object):
    ROBOT_LIBRARY_SCOPE = "GLOBAL"

    def __init__(self, endpoint, username, password, application_name):
        self.endpoint = endpoint
        self.application_name = application_name
        self.basic_auth = 'Basic %s' % base64.b64encode('%s:%s' % (username, password))

    def should_not_be_known_by_service(self):
        response = requests.get('%s/api/applications/' % self.endpoint,
                                headers=dict(Authorization=self.basic_auth))
        for application in response.json()['body']:
            if application['name'] == self.application_name :
                raise AssertionError('%s is still listed in the application' % self.application_name)



