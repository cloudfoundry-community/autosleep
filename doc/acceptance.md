#Acceptance test 
How to run the acceptance tests.

## Preconditions

### What you need on your computer to run the test
- Retrieve the content of the __acceptance__ folder
- python 2.7 (versions 3.X  of python have not been tested so far)
- Install all the requirements listed by the __acceptance/requirements.txt__ file: `pip install -r acceptance/requirements.txt` (we advise you to use a dedicated environment. Take a look at [virtualenv](https://pypi.python.org/pypi/virtualenv) for that).


### What you need in your cloudfoundry environment
- A test application (a [static website](https://github.com/cloudfoundry/staticfile-buildpack) for instance)
    which is reacheable from where tests execute 
- Autosleep applications deployed as an application in a space where you are allowed to deploy private service brokers.

## Run the tests
1. First copy `{ACCEPTANCE_TEST_DIRECTORY}/acceptance.tmpl.cfg` under `{ACCEPTANCE_TEST_DIRECTORY}/acceptance.cfg`
2. Fill the information in the  `{ACCEPTANCE_TEST_DIRECTORY}/acceptance.cfg` file
3. Simply launch the test with the following command:

`pybot {ACCEPTANCE_TEST_DIRECTORY}`


