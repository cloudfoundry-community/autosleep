#Acceptance test 
How to run the acceptance tests.

## Preconditions

### What you need on your computer to run the test
- Install robotframework. Check [the instructions](https://code.google.com/p/robotframework/wiki/Installation) on their website. Most of times, `pip install robotframework`will do.
- Retrieve the content of the "acceptance" folder from the sources.
- [CloudFoundry CLI](https://github.com/cloudfoundry/cli#downloads) installed .


### What you need in your cloudfoundry environment
- A test application (a [static website](https://github.com/cloudfoundry/staticfile-buildpack) for instance)
- Autosleep registered as a service broker (see [how to publish on the market place](publish.md)).

## Run the tests
Edit `{ACCEPTANCE_TEST_DIRECTORY}/Keywords.robot` file, to edit this line

```
${TESTED_APP_NAME}  static_test
```
Replace *static_test* with your test application name.

Then simply launch the test with the following command:

```
pybot {ACCEPTANCE_TEST_DIRECTORY}
```


