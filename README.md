# autosleep [![Build Status](https://travis-ci.org/Orange-OpenSource/autosleep.svg?branch=develop)](https://travis-ci.org/Orange-OpenSource/autosleep)

# Goal
The aim of the auto-sleep project is to give the ability for Cloud Foundry users to automatically have their app stopped after a given period of inactivity, and then automatically started when accessed through traffic received on their routes.

# Status
This is a work in progress. 
You can check the [full specifications] (https://docs.google.com/document/d/1tMhIBX3tw7kPEOMCzKhUgmtmr26GVxyXwUTwMO71THI/).

### What's already working:
For now we provide a [service broker] (https://docs.cloudfoundry.org/services/managing-service-brokers.html) which instances will:
* automatically bind applications in space (filtering out applications whose name matches a regexp).
* watch every bound application, measure inactivity (based on **https logs** and **redeploy/restart events**) and stop the application when an inactivity threshold is reached.

Download [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/) if you want to have a try.

### What we are working on:
* automatic restart on incoming HTTP trafic

# Usage by CloudFoundry users

We suppose that the autosleep service broker is already available in your market place. If you need help on that check [how to publish service broker](doc/publish.md).


##Create your autosleep service instance

### Basics

Create an autosleep service instance to get applications in the space automatically put to sleep after an inactivity period:

```
cf cs autosleep default my-autosleep
```

Autosleep **will periodically automatically bind every applications in the space** to this service instance (if you want to fine tune which apps gets auto-bound, please use the [excludeAppNameRegExp](#excludeappnameregexp) parameter to exclude some apps). 

Once bound, your application will be watched for inactivity, and automatically stopped by the autosleep service. If you wish to disable this watch, simply unbind your application from the autosleep service instance.

### Advanced configuration parameters

Optionally the autosleep service broker accepts the following parameters: 

- [```inactivity```] (#inactivity)
- [```excludeAppNameRegExp ```] (#excludeappnameregexp)
- [```no_optout ```] (#lockno_optout)
- [```secret ```] (#secret)

These parameters can be provided on service creations as well as on service updates, eg.

```
cf cs autosleep default my-autosleep -c '{"inactivity": "PT1H15M"}'
```

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

If platform teams (admins, org managers, or specific members of the space) don't want all space members to be able to manually unbound apps from the autosleep service themselves, then a no_optout mode is supported. When set:
* manually unbound apps, will automatically be re-bound again after the activity period
* the autosleep service-instance won't be deletable by space members to disable auto-bindings

To enable to "no_optout" mode, set this parameter to ``true``. As this is a protected parameter, you will have also have to provide a ['secret' parameter] (#secret). 

- *Example:*```'{"no_optout": "true"}'``` 
- *Default value :* false

#### *secret*
Provide a secret if you wish to set/change a protected parameter. Please save it carefully, has you will be asked to provide the same secret the next time you set/change a protected parameter

- *Example:*```'{"secret": "Th1s1zg00dP@$$w0rd"}'``` 
- *Default value :* none


# Usage by platform teams

## How to build
If you wish to build the app yourself, go to [build documentation] (doc/build.md).

## How to deploy and publish
Once you built the application or if you got it from [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/), go to [build documentation] (doc/publish.md).

# How to test
Acceptance tests are available in the source code, as robotframework tests. More information [here] (doc/test.md).
