#Publish
How to make autosleep service broker available in your market place.

## Deploy autosleep application
This is how to deploy autosleep application in cloudfoundry. If you wish to run it elsewhere you're on your own.
### Retrieve war
Download war from [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/), or [build it yourself] (build.md).
### Prepare your manifest
Make a *manifest.yml* file according to this template:

```
applications:
- name: autosleep
  memory: 1G
  instances: 1
  host: <your-route>
  domain: <your-domain>
  services:
     - redis
  buildpack: java_buildpack
  env:
    JAVA_OPTS: >
      -Dcf.client.target.endpoint=<endpoint>
      -Dcf.client.skip.ssl.validation=false
      -Dcf.client.username=<username>
      -Dcf.client.password=<password>
      -Dcf.client.clientId=<client_id>
      -Dcf.client.clientSecret=<client_secret>
```
### Deploy your app
```
cf push -f manifest.yml -p org.cloudfoundry.autosleep.war 
```    


## Publish on the market place
Check that the autosleep application is running and retrieve its url (`cf app autosleep-app`). 

Then run the following command:
```cf create-service-broker autosleep LOGIN PASSWORD http://your-autsleep-route```