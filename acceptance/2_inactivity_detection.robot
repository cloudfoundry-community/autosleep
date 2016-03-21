*** Settings ***
Resource        Keywords.robot
Documentation   Test if inactivity is detected
Force Tags      Service broker
Test Setup      Run Keywords  Clean all service data  Check broker is published  Create service instance  Bind application
Test Teardown   Run Keywords  Clean all service data

*** Test Cases ***

1) Detect inactivity after http activity
    [Documentation]     Check that app are stopped ${DEFAULT_INACTIVITY} after their last http activity

    ${smallPeriod}=     Evaluate  ${DEFAULT_INACTIVITY_IN_S}/4
    ${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}
	Stop application
	Start application
    Should be started
    Ping application
    Sleep               ${smallPeriod}
    Ping application
    Should be started

    Wait Until Keyword Succeeds     ${maxToWait}  10s  Should be stopped


2) Detect inactivity after a restart
    [Documentation]     Check that app are stopped ${DEFAULT_INACTIVITY} after their last reboot

    Stop application
	Start application
    Sleep               10
    Should be started

    ${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}

    Wait Until Keyword Succeeds     ${maxToWait}  10s  Should be stopped
