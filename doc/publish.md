#Publish
How to make autosleep service broker available in your market place.

## Deploy autosleep application
This is how to deploy autosleep application in cloudfoundry. If you wish to run it elsewhere you're on your own.
### Retrieve war
Download war and associated _manifest.tmpl.yml_ from [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/), or [build it yourself] (build.md).
### Prepare your manifest
Make a *manifest.yml* file according to the manifest.tmpl.yml template.

Autosleep service needs properties to work . The properties that are used are:

- __security.user.name__: the basic auth username for the service.
- __security.user.password__: the basic auth password for the service.
- __cf.client.target.endpoint__: the api endpoint of the cloudfoundry instance.
- __cf.client.skip.ssl.validation__: set this property to _true_ if the current cloudfoundry instance use self-signed certificates.
- __cf.client.username__: the username that will be used in by the autosleep service.
- __cf.client.password__: the password that will be used in by the autosleep service.
- __cf.client.clientId__: the client id of the application (optional). If none provided, it will used ```"cf"```.
- __cf.client.clientSecret__: the client secret of the application (optional). If none provided, it will used ```""```.
- __cf.security.password.encodingSecret__: the secret used to hash password (optional). If none provided, it will use ```""```.
- __cf.service.broker.id__: the service broker id. If none provided, it will use ```"autosleep"```.
- __cf.service.plan.id__: the service plan id. If none provided, it will use ```"default"```.

There are two ways of providing these properties to autosleep.

1. In _manifest.yml_: by giving these informations in the _manifest.yml_, in the _JAVA_OPTS_ section.
2. By providing them with the command ```cf  set-env <application name> <property name>  <property value>```. Keep in mind that dot characters are forbidden by cloudfoundry and must be replaced by underscores. For example, you may provide the username like this ```cf  set-env my-autosleep-service cf_client_username  bobby```

### Deploy your app
```
cf push -f manifest.yml -p org.cloudfoundry.autosleep.war 
```    


## Publish on the market place
Check that the autosleep application is running and retrieve its url (`cf app autosleep-app`). 

Then run the following command:

```cf create-service-broker <name> <login <password> <url>```

where:

- ```login``` and ```password``` are the values you provided in the _manifest.yml_ file for environment properties ___security.user.name___ and ___security.user.password___.
- ```name``` the name of the service as it will appear in the marketplace.
- ```url```: the URL of your servicde broker.


## Publish as a private broker
Currently cf does not support private service broker creation.But you may use ```cf curl``` command.
With the same parameters as above:

```cf curl /v2/service_brokers -X POST -d '{"name":"<name>","broker_url":"<url>","auth_username":"<login>","auth_password":"<password>","space_guid":"<space_id>"}'```

where ```space_id``` is the space guid where the application is deployed.