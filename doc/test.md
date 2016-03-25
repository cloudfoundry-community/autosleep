#Acceptance test 
How to run the acceptance tests.

## Preconditions

### What you need on your computer to run the test
- Install robotframework. Check [the instructions](https://code.google.com/p/robotframework/wiki/Installation) on their website. Most of times, `pip install robotframework`will do.
- Retrieve the content of the "acceptance" folder from the sources.
- The cloudfoundry python client. To install it, run in the console the following command `pip install cloudfoundry-client`


### What you need in your cloudfoundry environment
- A test application (a [static website](https://github.com/cloudfoundry/staticfile-buildpack) for instance)
- Autosleep deployed as an application in a space where you are allowed to deploy private service brokers.

## Run the tests
1. First copy `{ACCEPTANCE_TEST_DIRECTORY}/acceptance.tmpl.cfg` under `{ACCEPTANCE_TEST_DIRECTORY}/acceptance.cfg`
2. Fill the information in the  `{ACCEPTANCE_TEST_DIRECTORY}/acceptance.cfg` file
3. Simply launch the test with the following command:

`
pybot --pythonpath {ACCEPTANCE_TEST_DIRECTORY} {ACCEPTANCE_TEST_DIRECTORY}
`


