import httplib
import json
import logging
import os

import requests
from cloudfoundry_client import CloudFoundryClient, InvalidStatusCode


class Cloudfoundry(object):
    ROBOT_LIBRARY_SCOPE = "GLOBAL"

    def __init__(self, target_endpoint, skip_verification, login, password, organization_name, space_name,
                 application_name, service_broker_endpoint, service_broker_name, service_broker_auth_user,
                 service_broker_auth_password, instance_name, default_create_instance_parameters):
        Cloudfoundry._check_parameters(default_create_instance_parameters)
        self.proxies = dict(http=os.environ.get('HTTP_PROXY', ''), https=os.environ.get('HTTPS_PROXY', ''))
        self.client = CloudFoundryClient(target_endpoint, skip_verification=skip_verification, proxy=self.proxies)
        self.client.init_with_credentials(login, password)
        organization = self.client.organization.get_first(name=organization_name)
        if organization is None:
            raise AssertionError('Unknown organization %s' % organization_name)
        space = self.client.space.get_first(organization_guid=organization['metadata']['guid'], name=space_name)
        if space is None:
            raise AssertionError('Unknown space %s' % space_name)
        self.space_guid = space['metadata']['guid']
        application = self.client.application.get_first(space_guid=self.space_guid, name=application_name)
        if application is None:
            raise AssertionError('Unknown application %s in space %s' % (application_name, space_name))
        self.application_guid = application['metadata']['guid']
        self.service_broker_name = service_broker_name
        self.service_broker_endpoint = service_broker_endpoint
        self.service_broker_auth_user = service_broker_auth_user
        self.service_broker_auth_password = service_broker_auth_password
        self.instance_name = instance_name
        self.default_create_instance_parameters = default_create_instance_parameters

        self.instance_guid = None
        self.binding_guid = None
        self.broker_guid = None
        for service_broker in self.client.service_broker.list(space_guid=self.space_guid):
            if service_broker['entity']['name'] == self.service_broker_name \
                    and service_broker['entity']['broker_url'] == self.service_broker_endpoint:
                self.broker_guid = service_broker['metadata']['guid']
        self.plan_guid = None
        self._set_plan_from_broker()

    def clean_all_service_data(self):
        logging.info('clean_all_service_data - %s - %s - %s', self.space_guid, self.instance_name,
                     self.application_guid)
        instance = self.client.service_instance.get_first(space_guid=self.space_guid, name=self.instance_name)
        if instance is not None:
            logging.info('clean_all_service_data instance got - %s', instance['metadata']['guid'])
            cleaned = False
            while not cleaned:
                for binding in self.client.service_binding.list(service_instance_guid=instance['metadata']['guid']):
                    logging.info('clean_all_service_data binding - %s', binding['metadata']['guid'])
                    self.client.service_binding.remove(binding['metadata']['guid'])
                    logging.info('clean_all_service_data - binding deleted')
                try:
                    self.client.service_instance.remove(instance['metadata']['guid'])
                    cleaned = True
                except InvalidStatusCode, ex:
                    if ex.status_code == httplib.BAD_REQUEST and type(ex.body) == dict and \
                                    ex.body['error_code'] == 'CF-AssociationNotEmpty':
                        logging.debug('some binding appeared in the meantime. looping again')
                        pass
                    elif ex.status_code == httplib.BAD_GATEWAY and type(ex.body) == dict and \
                                    " can't be deleted during forced enrollment" in ex.body['description']:
                        logging.info('%s is in forced mode. Updating it as standard' % instance['metadata']['guid'])
                        parameters = dict()
                        parameters['auto-enrollment'] = 'standard'
                        parameters['secret'] = self.service_broker_auth_password
                        self.client.service_instance.update(instance['metadata']['guid'], parameters=parameters)
                    else:
                        raise
            logging.info('clean_all_service_data - instance deleted')
        self.instance_guid = None
        self.binding_guid = None

    def create_service_broker(self):
        if self.broker_guid is not None:
            raise AssertionError('Please delete service broker before creating a new one')
        else:
            service_broker = self.client.service_broker.create(broker_url=self.service_broker_endpoint,
                                                               broker_name=self.service_broker_name,
                                                               auth_username=self.service_broker_auth_user,
                                                               auth_password=self.service_broker_auth_password,
                                                               space_guid=self.space_guid)
            self.broker_guid = service_broker['metadata']['guid']
            logging.info('create_service_broker - broker created')
            self._set_plan_from_broker()

    def check_broker_is_published(self):
        if self.broker_guid is None:
            self.create_service_broker()

    def delete_service_broker(self):
        if self.broker_guid is None:
            raise AssertionError('Please create service broker before deleting it')
        else:
            self.client.service_broker.remove(self.broker_guid)
            self.broker_guid = None
            self.plan_guid = None

    def check_broker_is_not_published(self):
        if self.broker_guid is not None:
            self.delete_service_broker()

    def _set_plan_from_broker(self):
        if self.broker_guid is None:
            self.plan_guid = None
        else:
            service = self.client.service.get_first(service_broker_guid=self.broker_guid)
            if service is None:
                raise AssertionError('No service for service broker %s' % self.service_broker_name)
            logging.info('_set_plan_from_broker - service got')
            plan = self.client.service_plan.get_first(service_guid=service['metadata']['guid'])
            if plan is None:
                raise AssertionError('No plan for service broker %s' % self.service_broker_name)
            self.plan_guid = plan['metadata']['guid']

    def create_service_instance(self, parameters=None):
        if self.instance_guid is not None:
            raise AssertionError('Please delete service instance before creating a new one')
        else:
            if parameters is not None:
                Cloudfoundry._check_parameters(parameters)
            parameters_sent = parameters if parameters is not None \
                else self.default_create_instance_parameters if self.default_create_instance_parameters is not None \
                else {}
            logging.info('create_service_instance - parameters - %s', json.dumps(parameters_sent))
            instance = self.client.service_instance.create(space_guid=self.space_guid,
                                                           instance_name=self.instance_name,
                                                           plan_guid=self.plan_guid,
                                                           parameters=parameters_sent)
            self.instance_guid = instance['metadata']['guid']
            logging.info('create_service_instance - %s', self.instance_guid)

    def update_service_instance(self, parameters):
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before deleting it')
        else:
            logging.info('update_service_instance - parameters - %s', json.dumps(parameters))
            self.client.service_instance.update(self.instance_guid, parameters=parameters)
            logging.info('update_service_instance - ok')

    def delete_service_instance(self):
        logging.info('delete_service_instance - start')
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before deleting it')
        else:
            self.client.service_instance.remove(self.instance_guid)
            self.instance_guid = None
            logging.info('delete_service_instance - ok')

    def bind_application(self):
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before binding it')
        else:
            binding = self.client.service_binding.create(app_guid=self.application_guid,
                                                         instance_guid=self.instance_guid)
            self.binding_guid = binding['metadata']['guid']
            logging.info('bind_application - %s', self.binding_guid)

    def unbind_application(self):
        if self.binding_guid is None:
            raise AssertionError('Please bind application before unbinding it')
        else:
            self.client.service_binding.remove(self.binding_guid)
            self.binding_guid = None
            logging.info('unbind_application - ok')

    def should_be_bound(self):
        if self._get_application_binding() is None:
            raise AssertionError('Application should be bound to service %s' % self.instance_name)
        else:
            logging.info('should_be_bound - ok')

    def should_not_be_bound(self):
        if self._get_application_binding() is not None:
            raise AssertionError('Application should not be bound to service %s' % self.instance_name)
        else:
            logging.info('should_not_be_bound - ok')

    def is_application_bound(self):
        result = self._get_application_binding() is not None
        logging.info('is_application_bound - %s', json.dumps(result))
        return result

    def _get_application_binding(self):
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before testing if bound')
        else:
            return self.client.service_binding.get_first(service_instance_guid=self.instance_guid,
                                                         app_guid=self.application_guid)

    def get_bound_applications(self):
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before getting bound applications')
        else:
            result = []
            for app in self.client.service_binding.list(service_instance_guid=self.instance_guid):
                result.append(app['metadata']['guid'])
            logging.info('get_bound_applications - %d bounded', len(result))
            return result

    def should_be_started(self):
        instances = self.client.application.get_instances(self.application_guid)
        for instance_number, instance in instances.items():
            if instance['state'] != 'RUNNING':
                raise AssertionError('Instance %s is not running' % instance_number)
        logging.info('should_be_started - ok')

    def should_be_stopped(self):
        try:
            self.client.application.get_instances(self.application_guid)
            raise AssertionError('No instance started should have be found')
        except InvalidStatusCode, e:
            if e.status_code == httplib.BAD_REQUEST:
                logging.info('should_be_stopped - ok')
            else:
                raise

    def start_application(self):
        self.client.application.start(self.application_guid)
        logging.info('start_application - ok')

    def stop_application(self):
        self.client.application.stop(self.application_guid)
        logging.info('stop_application - ok')

    def ping_application(self, path="/"):
        application_summary = self.client.application.get_summary(self.application_guid)
        routes = application_summary.get('routes')
        if routes is None or len(routes) == 0:
            raise AssertionError('No route found for application %s', self.application_guid)

        uri_found = None
        logging.info(json.dumps(routes))
        for route in routes:
            if route.get('host') is not None and route.get('domain') is not None:
                uri_found = '%s.%s' % (route['host'], route['domain']['name'])
                break
        if uri_found is None:
            raise AssertionError('No uri found for application %s', self.application_guid)
        logging.info('ping_application - requesting %s', uri_found)
        response = requests.get('http://%s%s' % (uri_found, path), timeout=10.0, proxies=self.proxies,
                                headers={"Cache-Control": "no-cache"})
        logging.info('ping_application - response - %d - %s', response.status_code, response.text)
        if response.status_code != httplib.OK:
            raise AssertionError('Invalid status code %d' % response.status_code)
        else:
            logging.info('ping_application - ok')

    @staticmethod
    def _check_parameters(parameters):
        if parameters is not None and not isinstance(parameters, dict):
            raise AssertionError('create instance parameters should be a dictionary, got %s - %s'
                                 % (type(parameters), parameters))
