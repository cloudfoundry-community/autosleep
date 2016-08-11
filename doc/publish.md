#Publish manually

This section describes how to maunally make autosleep service broker available in your market place.

## Deploy autosleep application
This is how to deploy autosleep application in cloudfoundry. If you wish to run it elsewhere you're on your own.
### Retrieve wars
Download wars and associated _manifest.tmpl.yml_ from [latest release](https://github.com/Orange-OpenSource/autosleep/releases/), or [build it yourself](build.md).
### Prepare your manifest
Make a *manifest.yml* file according to the manifest.tmpl.yml template.

Prerequisites:

* a CC API user with permissions to act on enrolled app (suggesting to create a dedicated user such as "autosleep", so that its actions on applications give hints on who stopped/started applications). Autosleep uses the password auth credentials with this user.
   * on public CF instances: favor a user with cloudcontroller.read and cloudcontroller.write scopes, and role "SpaceDevelopper" on each the enrolleable autosleep space.
   * on private CF instance: favor an admin user with cloudcontroller.admin scope
* (optional) a UAA OAuth client, with cloudcontroller.read and cloudcontroller.write scopes
* a mysql service instance (with min 10 connections see [related issue 200](https://github.com/Orange-OpenSource/autosleep/issues/200))
* a wildcard route for each of the domains with routes that will trigger autowake up of apps.
* (optional) a dedicated space to deploy autosleep and autowakeup apps on which CF users don't have acces.s


Autosleep service needs properties to work . The properties that are used are:

- __security.user.name__: the basic auth username that protects access to the service broker.
- __security.user.password__: the basic auth password that protects access to the service broker.
- __cf.client.target.host__: the expected **hostname** of cloudfoundry CC api endpoint (port is always 443)
- __cf.client.skip.ssl.validation__: set this property to _true_ if the current cloudfoundry CC API endpoint uses self-signed certificates.
- __cf.client.username__: the username of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
- __cf.client.password__: the password of the pre-requisite CC API user that will be used in by the autosleep service to list/stop/start apps.
- __cf.client.clientId__: the (optional) client id of the application used to perform CC API calls. If none provided, it will used ```"cf"```.
- __cf.client.clientSecret__: the optional client secret of the application (optional) used to perform CC API calls. If none provided, it will used ```""```.
- __cf.security.password.encodingSecret__: the secret used to hash password (optional). If none provided, it will use ```""```.
- __cf.service.broker.id__: the service broker id that is used as a "service offering name" and will appear in the marketplace. If none provided, it will use ```"autosleep"```. Must be unique in the CF instance across all brokers.
- __cf.service.plan.id__: the service plan id. If none provided, it will use ```"default"```. Must be unique in the CF instance across all brokers.
- __autosleep.debug__: a list too enable `DEBUG` logs. So far, the available keys are `autosleep` to turn applicative logs in `DEBUG`, and `spring`for the spring part.

There are two ways of providing these properties to autosleep:

1. In _manifest.yml_: by giving these informations in the _manifest.yml_, in the _JAVA_OPTS_ section.
2. By providing them directly in  the _env_ section of the _manifest.yml_. Note that [acccording to the documentation](http://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html#env-block), you will be able to update them afer the first deployment with the command `cf  set-env <application name> <property name>  <property value>`. Keep in mind that dot characters are forbidden by cloudfoundry cli and must be replaced by underscores. For example, you may provide the username like this `cf  set-env my-autosleep-service cf_client_username  bobby`

### Deploy autosleep app

`
cf push -f manifest.yml
`    

Create a wildcard route for each of domain where sleeping apps will receive traffic. This will

```
cf create-route <autosleep-space> mydomain.org -n '*'
cf map-route autowakeup-app mydomain.org --hostname '*'
```

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

## Access the backoffice UI

The /admin/debug/ endpoint provides the list of all service instances, and bound applications in a central place. It is protected by basic auth credentials (the service broker credentials). 


#Publish automatically

Alternatively, you may use https://github.com/Orange-OpenSource/cloudfoundry-operators-tools-boshrelease#orange-autosleep-service-for-cloudfoundry to automatically install autosleep from a bosh CLI using packaged bosh errands.
