# autosleep [![Build Status](https://travis-ci.org/Orange-OpenSource/autosleep.svg?branch=develop)](https://travis-ci.org/Orange-OpenSource/autosleep)

# Goal
The aim of the auto-sleep project is to give the ability for Cloud Foundry users to automatically have their app stopped after a given period of inactivity, and then automatically started when accessed.

# Status
This is a work in progress. 
You can check the [specification proposal here] (https://docs.google.com/document/d/1tMhIBX3tw7kPEOMCzKhUgmtmr26GVxyXwUTwMO71THI/).

### What's already working:
For now we provide a [service broker] (https://docs.cloudfoundry.org/services/managing-service-brokers.html) which instances will watch any bound application, detect inactivity (based on **https logs** and **redeploy/restart events**) and stop the application if needed.

Download [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/) if you want to have a try.

### What we are working on:
* "opt-out" mode: applications are automatically bound to the service, according to a regexp.
* Automatic restart on incoming HTTP trafic

# How to use
We suppose that you've already published the service broker in your market place. If you need help on that check [how to publish service broker](doc/publish.md).

##Create your autosleep service instance
```
cf cs autosleep default my-autosleep
```
If you don't give any additionnal parameter, the default inactivity duration of 24H will be used. If you wish to manually set another amount of time, use the following:

```
cf cs autosleep default my-autosleep  -c '{"inactivity": "PT1H15M"}'
```  
In this example the application will be considered as inactive after *1 hour and 15 minutes*. The time format used is [the ISO8601] (https://en.wikipedia.org/wiki/ISO_8601) format.
##Bind your app
```
cf bind-service MY_APP my-autosleep
```
Once bound, your application will be watch for inactivity. If you wish to stop this watch, simply unbind your application.

# How to build
If you wish to build the app yourself, go to [build documentation] (doc/build.md).

# How to publish
Once you built the application or if you got it from [latest release] (https://github.com/Orange-OpenSource/autosleep/releases/), go to [build documentation] (doc/publish.md).

# How to test
Acceptance tests are available in the source code, as robotframework tests. More information [here] (doc/test.md).
