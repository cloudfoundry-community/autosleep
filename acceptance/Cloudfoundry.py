from cloudfoundry_client import CloudFoundryClient, InvalidStatusCode
import httplib
import logging
import json


class Cloudfoundry(object):
    ROBOT_LIBRARY_SCOPE = "GLOBAL"

    def __init__(self, target_endpoint, skip_verification, login, password, organization_name, space_name,
                 application_name, service_name, plan_name, instance_name, default_create_instance_parameters):
        Cloudfoundry._check_parameters(default_create_instance_parameters)
        self.client = CloudFoundryClient(target_endpoint, skip_verification=skip_verification)
        self.client.credentials_manager.init_with_credentials(login, password)
        organization = self.client.organization.get_first(name=organization_name)
        if organization is None:
            raise AssertionError('Unknown organization %s' % organization_name)
        space = self.client.space.get_first(organization_guid=organization['metadata']['guid'], name=space_name)
        if space is None:
            raise AssertionError('Unknown space %s' % space_name)
        self.space_guid = space['metadata']['guid']
        application = self.client.application.get_first(space_guid=self.space_guid, name=application_name)
        if application is None:
            raise AssertionError('Unknown application %s' % application)
        self.application_guid = application['metadata']['guid']
        service = self.client.service.get_first(label=service_name)
        if service is None:
            raise AssertionError('Unknown service %s' % application)
        self.service_name = service_name
        self.plan_guid = None
        for plan in self.client.service_plan.list(service_guid=service['metadata']['guid']):
            if plan['entity']['name'] == plan_name:
                self.plan_guid = plan['metadata']['guid']
                break
        if self.plan_guid is None:
            raise AssertionError('Unknown plan %s' % plan_name)
        self.instance_name = instance_name
        self.default_create_instance_parameters = default_create_instance_parameters
        self.instance_guid = None
        self.binding_guid = None

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
                    else:
                        raise
            logging.info('clean_all_service_data - instance deleted')
        self.instance_guid = None
        self.binding_guid = None

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
            instance = self.client.service_instance.create(self.space_guid, self.instance_name, self.plan_guid,
                                                           parameters_sent)
            self.instance_guid = instance['metadata']['guid']
            logging.info('create_service_instance - %s', self.instance_guid)

    def update_service_instance(self, parameters):
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before deleting it')
        else:
            logging.info('update_service_instance - parameters - %s', json.dumps(parameters))
            self.client.service_instance.update(self.instance_guid, parameters)
            logging.info('update_service_instance - ok')

    def delete_service_instance(self):
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
            binding = self.client.service_binding.create(self.application_guid, self.instance_guid)
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
        if self.instance_guid is None:
            raise AssertionError('Please create service instance before testing if bound')
        else:
            binding = self.client.service_binding.get_first(service_instance_guid=self.instance_guid,
                                                            app_guid=self.application_guid)
            if binding is None:
                raise AssertionError('Application should be bound to service %s' % self.instance_name)
            else:
                logging.info('should_be_bound - ok')

    def is_application_bound(self):
        if self.instance_guid is not None:
            raise AssertionError('Please create service instance before testing if bound')
        else:
            binding = self.client.service_binding.get_first(service_instance_guid=self.instance_guid,
                                                            app_guid=self.application_guid)
            result = binding is not None
            logging.info('is_application_bound - %s', json.dumps(result))
            return result

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
        instance_stats = self.client.application.get_stats(self.application_guid)
        if len(instance_stats) == 0:
            raise AssertionError('No stats found for application %s', self.application_guid)
        uri_found = None
        logging.info(json.dumps(instance_stats))
        for _, instance_stat in instance_stats.items():
            if instance_stat.get('stats', None) is not None:
                if len(instance_stat['stats'].get('uris', [])) > 0:
                    uri_found = instance_stat['stats']['uris'][0]
                    break
        if uri_found is None:
            raise AssertionError('No uri found for application %s', self.application_guid)
        logging.info('ping_application - requesting %s', uri_found)
        conn = httplib.HTTPConnection(uri_found, 80)
        conn.request("GET", path)
        response = conn.getresponse()
        status = response.status
        logging.info('ping_application - response - %d - %s', status, response.read())
        if status != httplib.OK:
            raise AssertionError('Invalid status code %d' % response.status)
        else:
            logging.info('ping_application - ok')

    @staticmethod
    def _check_parameters(parameters):
        if parameters is not None and not isinstance(parameters, dict):
            raise AssertionError('create instance parameters should be a dictionary, got %s - %s'
                                 % (type(parameters), parameters))
