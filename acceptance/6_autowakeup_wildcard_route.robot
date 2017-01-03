*** Settings ***
Resource        Keywords.robot
Library     AsyncLibrary
Documentation   Test the autowakeup feature based on wildcard routes
Force Tags      Autowakeup
Test Setup      Run Keywords  Clean all service data  Check broker is published
Test Teardown   Run Keywords  Clean all service data

*** Test Cases ***

1) Incoming traffic will trigger app restart a sleeping appl
    [Documentation]
    ...     = Automatic restart tests =
    ...     *This test needs to be run on a multi-instances autowakeup (at least 2) to succeed*
    ...     - Check that app that are stopped by autosleep are restarted on incoming trafic
    ...     - Check that two parallel calls will both trigger start on multiple instances, without causing error
    ...     - Check that traffic sent during restart will receive a 503 error


    ${smallPeriod}=     Evaluate  ${DEFAULT_INACTIVITY_IN_S}/4
    ${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}

    log     starting the target application, and waiting for it to be stopped
    Create service instance
    Bind application
    Stop Application
    Start application
    Should be started
    Wait Until Keyword Succeeds     ${maxToWait}  10s  Should be stopped

    log     sending incoming trafic, two requests in parallel
    # first one will be the one to trigger restart. we sent it async (cause it will wait for the app to be started) and will check its result later
    ${handle}  async run   Ping Application
    ${handle2}  async run   Ping Application

    Sleep  400 ms  Wait a little so that next call won't arrive in parallel with the other

    #second one will get a 503 error because restart is in progress
    Run Keyword And Expect Error  	Invalid status code 503     Ping Application

    #check that app is successfully restarted
    Wait Until Keyword Succeeds     ${maxToWait}  3s  Should be started

    #check that the initial ping that restart the application ended successfully (without http error)
    ${return_value}    async get    ${handle}
    Should Be True  ${return_value} == None



2) Incoming traffic on other unbound stopped app doesn't trigger a restart
    [Documentation]        Check that orphan traffic received on non-enrolled apps does not trigger a restart
    Stop Application
    Run Keyword And Expect Error  	Invalid status code 404     Ping Application
    Should be stopped
