# autosleep [![Build Status](https://travis-ci.org/Orange-OpenSource/autosleep.svg?branch=develop)](https://travis-ci.org/Orange-OpenSource/autosleep) [![Coverage Status](https://coveralls.io/repos/github/Orange-OpenSource/autosleep/badge.svg?branch=develop)](https://coveralls.io/github/Orange-OpenSource/autosleep?branch=develop)

# Goal
The aim of the auto-sleep project is to give the ability for Cloud Foundry users to automatically have their app stopped after a given period of inactivity, and then automatically started when accessed through traffic received on their routes.

# Status
This is a work in progress. 
You can check the [full specifications](https://docs.google.com/document/d/1tMhIBX3tw7kPEOMCzKhUgmtmr26GVxyXwUTwMO71THI/) and the currently supported features in the [acceptance tests](acceptance/).

Get a deeper introduction in the Autosleep talk at Cf Summit Santa Clata 2016 ( [slides](http://fr.slideshare.net/gberche/autosleep-inactive-apps-get-automatically-put-to-sleep-and-restarted-on-incoming-traffic),  [youtube video recording](https://www.youtube.com/watch?v=fQQRGxqkM-4&index=29&list=PLhuMOCWn4P9gGrKEtCBKYpEl5BXGBCsQZ), and [high-res demo](https://drive.google.com/open?id=0B_RQz82RzSUndnd4TFJOODFkTU0) )

[![Autosleep Cf Summit Santa Clata 2016 talk](https://cloud.githubusercontent.com/assets/4748380/16609625/d3367eba-4355-11e6-9392-25e6958d59d8.png)](http://fr.slideshare.net/gberche/autosleep-inactive-apps-get-automatically-put-to-sleep-and-restarted-on-incoming-traffic)

### What's already working:
For now we provide a [service broker](https://docs.cloudfoundry.org/services/managing-service-brokers.html) which instances will:

* automatically bind applications in space (filtering out applications whose name matches a regexp).
* watch every bound application, measure inactivity (based on **https logs** and **redeploy/restart events**) and stop the application when an inactivity threshold is reached.
* a service dashboard for users to understand behavior of the service (such as time to sleep or current enrollment status)

Download [latest release](https://github.com/Orange-OpenSource/autosleep/releases/) if you want to give it a try, or [build from sources](/doc/build.md)

### What we are working on:
* automatic restart on incoming HTTP trafic (see early work in the [develop](https://github.com/Orange-OpenSource/autosleep/tree/develop) branch)

# Usage by CloudFoundry users

We suppose that the autosleep service broker is already available in your market place. If you need help on that check [how to publish service broker](doc/publish.md).


## Create your autosleep service instance

### Basics

Create an autosleep service instance to watch all applications in the space and automatically put them to sleep after default idle duration:

`
cf cs autosleep default my-autosleep
`

Autosleep **will periodically automatically bind every applications in the space** to this service instance (if you want to fine tune which apps gets auto-bound, please use the [exclude-from-auto-enrollment](#exclude-from-auto-enrollment) parameter to exclude some apps). 

Once bound, your application will be watched for inactivity, and automatically stopped by the autosleep service. If you wish to disable this watch, simply unbind your application from the autosleep service instance.

### Advanced configuration parameters

Optionally the autosleep service broker accepts the following parameters during service creation: 

- [`idle-duration`](#idle-duration)
- [`exclude-from-auto-enrollment `](#exclude-from-auto-enrollment)
- [`auto-enrollment `](#lockauto-enrollment)
- [`secret `](#secret)

<!--
- [`autosleep-despite-route-services-error`](#autosleep-despite-route-services-error)
-->

Only the `auto-enrollment ` field is is mutable, i.e. is accepted on service updates, e.g.
`
cf cs autosleep default my-autosleep -c '{"auto-enrollment": "normal", "secret": "Th1s1zg00dP@$$w0rd"}'
`

If you need to update other fields (e.g. `idle-duration`), rather choose to instanciate a new service instance.up

#### *idle-duration* 
Duration after which bound applications will be considered as inactive. The time format used is [the ISO8601] (https://en.wikipedia.org/wiki/ISO_8601#Durations) duration format.

- *Example:*`'{"idle-duration": "PT1H15M"}'`
 would stop the application after *1 hour and 15 minutes* of inactivity.
- *Default value :*  24H

#### *exclude-from-auto-enrollment* 
If you don't want all the application to be automatically bound, you can set this parameter with a regular expression to filter on application names.

- *Example:*`'{"excludeAppNameRegExp": ".*"}'`
 wouldn't automatically bind any application in the space. Application would have to be bound manually.
- *Default value :*  none (every app in space will be bound).


#### :lock:*auto-enrollment* 

By default this parameter is set as `standard`. If platform teams (admins, org managers, or specific members of the space) don't want all space members to be able to manually permanently unbound apps from the autosleep service themselves, then a forced mode is supported.

In a forced auto-enrollment mode then:

* manually unbound apps will automatically be re-bound again after the inactivity period. Until the then, app teams are free to delete the app if a cleanup is necessary.
* the autosleep service-instance won't be deletable by space members to disable next auto-enrollments

To enable to `forced` mode, set the `auto-enrollment` parameter to ``forced``. As this is a protected parameter, you will have also have to provide a [`secret` parameter](#secret).

- *Example:*`'{"auto-enrollment": "forced"}'`
- *Default value :* `standard`

#### *secret*

Provide a secret if you wish to set/change a protected parameter. Please save it carefully, has you will be asked to provide the same secret the next time you set/change a protected parameter. As a fallback, you may also use the credential password set at deployment time (see  the `security.user.password` in [publish documentation](doc/publish.md)).

- *Example:*`'{"secret": "Th1s1zg00dP@$$w0rd"}'`
- *Default value :* `null`


<!--
#### *autosleep-despite-route-services-error*

On some application, *cloudfoundry api* may refuse to bind *autosleep service instance* (exposing itself as a route service) to the application's routes (we need to do this operation to reroute all application flow to *autosleep* in order to restart the application if requested). Since we perform these binding operations **before** stopping application, if *api* refuse the operation, the application will never be stopped. 
By setting the value of this parameter to `true`, you skip the errors sent by *route binding* operations and put the application to sleep anyway. Be aware that the application will not be restarted automatically by the *autosleep* if requested.

* Example `'{"autosleep-despite-route-services-error" : true}'`
* *Default value*: `false`

-->



# Usage by platform teams

## How to build
If you wish to build the app yourself, go to [build documentation](doc/build.md).

## How to deploy and publish
Once you built the application or if you got it from [latest release](https://github.com/Orange-OpenSource/autosleep/releases/), go to [publish documentation](doc/publish.md).

# How to test
Acceptance tests are available in the source code, as robotframework tests. More information [here](doc/test.md).
