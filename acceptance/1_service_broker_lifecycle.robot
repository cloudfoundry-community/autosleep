*** Settings ***
Resource            Keywords.robot
Documentation   Test basic service broker lifecycle
Force Tags      Service broker



*** Test Cases ***
0) Prepare
    [Documentation]     Clean service bindings and instance if exist
    Clean all service data

1) create service
    [Documentation]     Create a service instance
    Create service instance


2) bind
    [Documentation]    Bind ${TESTED_APP_NAME} to base service instance
    Bind application

3) unbind
    [Documentation]     Unbind application
    Unbind application

4) delete service
    [Documentation]     Delete service instance
    Delete service instance

