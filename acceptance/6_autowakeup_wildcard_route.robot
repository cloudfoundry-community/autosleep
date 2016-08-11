*** Settings ***
Resource        Keywords.robot
Library     AsyncLibrary
Documentation   Test the autowakeup feature based on wildcard routes
Force Tags      Autowakeup
Test Setup      Run Keywords  Clean all service data  Check broker is published
Test Teardown   Run Keywords  Clean all service data

*** Test Cases ***

1) Incoming traffic will trigger app restart on ap stop by autosleep
    [Documentation]     Check that app that are stopped by autosleep are restarted on incoming trafic, and that a 503 is send during restart


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
    #second one will get a 503 error because restart is in progress
    Run Keyword And Expect Error  	Invalid status code 503     Ping Application

    #check that app is successfully restarted
    Wait Until Keyword Succeeds     ${maxToWait}  3s  Should be started

    #check that the initial ping that restart the application ended successfully (without http error)
    ${return_value}    async get    ${handle}
    Should Be True  ${return_value} == None



2) Incoming traffic on other stopped app don't trigger a restart
    Stop Application
    Run Keyword And Expect Error  	Invalid status code 404     Ping Application
    Should be stopped
