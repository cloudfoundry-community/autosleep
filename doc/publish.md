#Publish
How to make autosleep service broker available in your market place.

## Deploy autosleep application
This is how to deploy autosleep application in cloudfoundry. If you wish to run it elsewhere you're on your own.
### Retrieve war
Download war and associated _manifest.tmpl.yml_ from [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/), or [build it yourself] (build.md).
### Prepare your manifest
Make a *manifest.yml* file according to the manifest.tmpl.yml template.
### Deploy your app
```
cf push -f manifest.yml -p org.cloudfoundry.autosleep.war 
```    


## Publish on the market place
Check that the autosleep application is running and retrieve its url (`cf app autosleep-app`). 

Then run the following command:
```cf create-service-broker autosleep LOGIN PASSWORD http://your-autsleep-route```
where ___LOGIN___ and ___PASSWORD___ are the values you provided in the _manifest.yml_ file for environment properties ___security.user.name___ and ___security.user.password___