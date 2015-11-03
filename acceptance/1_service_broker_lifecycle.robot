*** Settings ***
Resource            Keywords.robot
Documentation   Test basic service broker lifecycle
Force Tags      Service broker



*** Test Cases ***
0) Prepare
    [Documentation]     Clean service bindings and instance if exist
    Clean

1) create service
    [Documentation]     Create a service instance
    Create service instance
    ${result} =    Run Process    cf  services
    Should Contain    ${result.stdout}    ${SERVICE_INSTANCE_NAME}

2) bind
    [Documentation]    Bind ${TESTED_APP_NAME} to base service instance
    Bind service instance

3) unbind
    [Documentation]     Unbind service instance
    Unbind service instance

4) delete service
    [Documentation]     Delete service instance
    Delete service instance
    ${result} =    Run Process    cf  services
    Should Not Contain    ${result.stdout}    ${SERVICE_INSTANCE_NAME}

