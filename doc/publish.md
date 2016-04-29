#Publish
How to make autosleep service broker available in your market place.

## Deploy autosleep application
This is how to deploy autosleep application in cloudfoundry. If you wish to run it elsewhere you're on your own.
### Retrieve war
Download war and associated _manifest.tmpl.yml_ from [latest release](https://github.com/Orange-OpenSource/autosleep/releases/), or [build it yourself](build.md).
### Prepare your manifest
Make a *manifest.yml* file according to the manifest.tmpl.yml template.

Prerequisites:

* a CC API user with cloudcontroller.read and cloudcontroller.write scopes, and role "SpaceDevelopper" on the enrolleable autosleep spaces
* (optional) a UAA OAuth client, with cloudcontroller.read and cloudcontroller.write scopes
* a mysql service instance


Autosleep service needs properties to work . The properties that are used are:

- __security.user.name__: the basic auth username for the service broker.
- __security.user.password__: the basic auth password for the service broker.
- __cf.client.target.endpoint__: the expected **hostname** of api endpoint of the cloudfoundry instance.
- __cf.client.skip.ssl.validation__: set this property to _true_ if the current cloudfoundry instance use self-signed certificates.
- __cf.client.username__: the username of the pre-requisite CC API user that will be used in by the autosleep service.
- __cf.client.password__: the password of the pre-requisite CC API user that will be used in by the autosleep service.
- __cf.client.clientId__: the client id of the application (optional) used to perform CC API calls. If none provided, it will used ```"cf"```.
- __cf.client.clientSecret__: the client secret of the application (optional) used to perform CC API calls. If none provided, it will used ```""```.
- __cf.security.password.encodingSecret__: the secret used to hash password (optional). If none provided, it will use ```""```.
- __cf.service.broker.id__: the service broker id that is used as a "service offering name" and will appear in the marketplace. If none provided, it will use ```"autosleep"```. Must be unique in the CF instance across all brokers.
- __cf.service.plan.id__: the service plan id. If none provided, it will use ```"default"```. Must be unique in the CF instance across all brokers.

There are two ways of providing these properties to autosleep:

1. In _manifest.yml_: by giving these informations in the _manifest.yml_, in the _JAVA_OPTS_ section.
2. By providing them with the command `cf  set-env <application name> <property name>  <property value>`. Keep in mind that dot characters are forbidden by cloudfoundry and must be replaced by underscores. For example, you may provide the username like this `cf  set-env my-autosleep-service cf_client_username  bobby`

### Deploy autosleep app

`
cf push -f manifest.yml -p autosleep-x-y-z.jar
`    


## Publish on the market place
Check that the autosleep application is running and retrieve its url (`cf app autosleep-app`). 

Then register the app as a service broker:

`cf create-service-broker <broker-name> <login <password> <url>`

where:

- `login` and `password` are the values you provided in the _manifest.yml_ file for environment properties ___security.user.name___ and ___security.user.password___.
- `broker-name` the name of the broker as listed to CF operator.
- `url`: the URL of the autosleep app collected above.

Finally, choose to expose the default plan into one or all organisations

`cf enable-service-access <offering-name> -o <org-name>`

where:
- `offering-name` corresponds to the value of __cf.service.broker.id__ filled in the manifest file.

## Publish as a private broker

`cf create-service-broker <name> <login <password> <url> --space-scoped`
