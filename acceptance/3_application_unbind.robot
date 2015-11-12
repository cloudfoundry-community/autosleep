*** Settings ***
Resource        Keywords.robot
Documentation   Test if application unbinded is not stopped
Force Tags      Service broker
Test Teardown   Run Keywords  Delete service instance

*** Test Cases ***

1) Detect inactivity after http activity
    [Documentation]     Check that app are still started ${DEFAULT_INACTIVITY} after their last http activity

	Clean
	Create service instance

    Restart App         ${TESTED_APP_NAME}
    Sleep               10
    Check App Started   ${TESTED_APP_NAME}

	Bind service instance
    Unbind service instance

	${maxToWait}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}
	Sleep				${maxToWait}
    Check App Started       ${TESTED_APP_NAME}


