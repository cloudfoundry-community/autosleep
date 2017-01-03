*** Settings ***
Resource        Keywords.robot
Documentation   Test users can permanently opt out in standard enrollment mode
Force Tags      Service broker
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${INACTIVITY_IN_S}  30
${INACTIVITY}  PT${INACTIVITY_IN_S}S
&{INSTANCE_PARAMETERS}		idle-duration=${INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}	autosleep-despite-route-services-error=true

*** Test Cases ***

1) Unbound application should not be put to sleep
    [Documentation]     Check that app are still started ${DEFAULT_INACTIVITY} after their last http activity

	Clean all service data
	Check broker is published
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


