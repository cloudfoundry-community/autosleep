*** Settings ***
Resource        Keywords.robot
Documentation   Test if application unbinded is not stopped
Force Tags      Service broker
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${INACTIVITY_IN_S}  30
${INACTIVITY}  PT${INACTIVITY_IN_S}S
&{INSTANCE_PARAMETERS}		idle-duration=${INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}

*** Test Cases ***

1) Unbound application should remain started
    [Documentation]     Check that app are still started ${DEFAULT_INACTIVITY} after their last http activity

	Clean all service data
	Create service broker
	Create service instance      ${INSTANCE_PARAMETERS}

	Stop application
	Start application
    Sleep               10
    Should be started

	Bind application
    Unbind application

	${maxToWait}=      Evaluate  ${INACTIVITY_IN_S}+${INACTIVITY_BUFFER_IN_S}
	Sleep				${maxToWait}
    Should be started


