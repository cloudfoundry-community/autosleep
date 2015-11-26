# autosleep [![Build Status](https://travis-ci.org/Orange-OpenSource/autosleep.svg?branch=develop)](https://travis-ci.org/Orange-OpenSource/autosleep)

# Goal
The aim of the auto-sleep project is to give the ability for Cloud Foundry users to automatically have their app stopped after a given period of inactivity, and then automatically started when accessed.

# Status
This is a work in progress. 
You can check the [specification proposal here] (https://docs.google.com/document/d/1tMhIBX3tw7kPEOMCzKhUgmtmr26GVxyXwUTwMO71THI/).

### What's already working:
For now we provide a [service broker] (https://docs.cloudfoundry.org/services/managing-service-brokers.html) which instances will:
* automatically bind applications in space (according to a regexp).
* watch every bound application, detect inactivity (based on **https logs** and **redeploy/restart events**) and stop the application if needed.

Download [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/) if you want to have a try.

### What we are working on:
* automatic restart on incoming HTTP trafic

# How to use
We suppose that you've already published the service broker in your market place. If you need help on that check [how to publish service broker](doc/publish.md).

##Create your autosleep service instance

### Basics
Create your service instance: 
```
cf cs autosleep default my-autosleep
```

Autosleep **will automatically bind every application in the space** to this service instance. (if you want to prevent this, please use the [excludeAppNameRegExp](#excludeappnameregexp) parameter).

Later, it will also watch for newly created applications and bind them too (every 24H). 

Once bound, your application will be watch for inactivity. If you wish to stop this watch, simply unbind your application.

### Advanced configuration parameters
Autosleep service broker handle the following parameters: 

- [```inactivity```] (#inactivity)
- [```excludeAppNameRegExp ```] (#excludeappnameregexp)
- [```no_optout ```] (#lockno_optout)
- [```secret ```] (#secret)

These parameters can be provided on service creations as well as on service updates. 

#### *inactivity* 
Duration after which bound applications will be considered as inactive. The time format used is [the ISO8601] (https://en.wikipedia.org/wiki/ISO_8601#Durations) duration format.

- *Example:*```'{"inactivity": "PT1H15M"}'``` 
 would stop the application after *1 hour and 15 minutes* of inactivity.
- *Default value :*  24H

#### *excludeAppNameRegExp* 
If you don't want all the application to be automatically bound, you can set this parameter with a regular expression to filter on application names.

- *Example:*```'{"excludeAppNameRegExp": ".*"}'``` 
 wouldn't automatically bind any application in the space. Application would have to be bound manually.
- *Default value :*  none (every app in space will be bound).


#### :lock:*no_optout* 
If you don't want the application to be able to manually unbound itselves, set this parameter to ``true``. As this is a protected parameter, you will have also have to provide a ['secret' parameter] (#secret). 

- *Example:*```'{"no_optout": "true"}'``` 
- *Default value :* false

#### *secret*
Provide a secret if you wish to set/change a protected parameter. Please save it carefully, has you will be asked to provide the same secret the next time you set/change a protected parameter

- *Example:*```'{"secret": "Th1s1zg00dP@$$w0rd"}'``` 
- *Default value :* none



# How to build
If you wish to build the app yourself, go to [build documentation] (doc/build.md).

# How to publish
Once you built the application or if you got it from [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/), go to [build documentation] (doc/publish.md).

# How to test
Acceptance tests are available in the source code, as robotframework tests. More information [here] (doc/test.md).
