*** Settings ***
Resource            Keywords.robot
Documentation   Test basic service broker lifecycle
Force Tags      Service broker



*** Test Cases ***
0) Prepare
    [Documentation]     Clean service bindings and instance if exist
    Clean all service data
    Check broker is not published

1) create service broker
    [Documentation]     Create a service broker
    Create service broker

2) create service instance
    [Documentation]     Create a service instance
    Create service instance

3) bind
    [Documentation]    Bind ${TESTED_APP_NAME} to base service instance
    Bind application

4) unbind
    [Documentation]     Unbind application
    Unbind application

5) delete service instance
    [Documentation]     Delete service instance
    Delete service instance
    ${maxToWait}=      Evaluate  2*${DEFAULT_INACTIVITY_IN_S}
    Wait Until Keyword Succeeds     ${maxToWait}  3s  Should not be known by service

6) delete service broker
    [Documentation]     Delete service broker
    Delete service broker


