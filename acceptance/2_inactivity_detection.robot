*** Settings ***
Resource        Keywords.robot
Documentation   Test if inactivity is detected
Force Tags      Service broker

*** Settings ***
Test Setup      Run Keywords  Clean  Create service instance  Bind service instance
Test Teardown   Run Keywords  Unbind service instance  Delete service instance

*** Test Cases ***

1) Detect inactivity after http activity
    [Documentation]     Check that app are stopped ${DEFAULT_INACTIVITY} after their last http activity

    Restart App         ${TESTED_APP_NAME}
    Sleep               10
    Check App Started   ${TESTED_APP_NAME}

    Simulate HTTP Activity  ${TESTED_APP_NAME}
    Check App Started       ${TESTED_APP_NAME}

    ${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}

    Wait Until Keyword Succeeds     ${maxToWait}  10s  Check App Stopped   ${TESTED_APP_NAME}


2) Detect inactivity after a restart
    [Documentation]     Check that app are stopped ${DEFAULT_INACTIVITY} after their last reboot

    Restart App         ${TESTED_APP_NAME}
    Sleep               10
    Check App Started   ${TESTED_APP_NAME}

    ${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}

    Wait Until Keyword Succeeds     ${maxToWait}  10s  Check App Stopped   ${TESTED_APP_NAME}

