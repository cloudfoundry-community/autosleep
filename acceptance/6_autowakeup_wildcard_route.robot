*** Settings ***
Resource        Keywords.robot
Documentation   Test the autowakeup feature based on wildcard routes
Force Tags      Autowakeup
Test Setup      Run Keywords  Clean all service data  Check broker is published  Create service instance  Bind application
Test Teardown   Run Keywords  Clean all service data

*** Test Cases ***

1) Stopped app should restart on incoming trafic
    [Documentation]     Check that app that are stopped by autosleep are restarted on incoming trafic

    log     starting the target application, and waiting for it to be stopped
    ${smallPeriod}=     Evaluate  ${DEFAULT_INACTIVITY_IN_S}/4
    ${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}
	Start application
    Should be started
    Wait Until Keyword Succeeds     ${maxToWait}  10s  Should be stopped

    log     sending incoming trafic
    Ping Application
    Wait Until Keyword Succeeds     ${maxToWait}  3s  Should be started