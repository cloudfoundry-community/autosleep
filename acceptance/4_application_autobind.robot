*** Settings ***
Resource        Keywords.robot
Documentation   Test if application autobound is stopped
Force Tags      Service broker
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${INACTIVITY_IN_S}  30
${INACTIVITY}  PT${INACTIVITY_IN_S}S

*** Test Cases ***

1) Automatically bind application by service instance
    [Documentation]     Check that app is automatically bound by service instance
    Clean all service data
    Create service broker
	${regex}					Catenate   SEPARATOR=      ^(?:(?!    ${TESTED_APP_NAME}   ).)*$
    ${parameters}				Create Dictionary	idle-duration=${INACTIVITY}	exclude-from-auto-enrollment=${regex}
    Create service instance      ${parameters}
    Wait Until Keyword Succeeds     ${INACTIVITY_IN_S}s  3s  Should be bound


2) Service does not bind ignored applications
    [Documentation]        Check that no application is bound by the service instance
    Clean all service data
    Create service broker
    ${parameters}				Create Dictionary	idle-duration=${INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}
    Create service instance      ${parameters}
    ${halfPeriod}=      Evaluate  ${INACTIVITY_IN_S}/2
    Sleep                    ${halfPeriod}
    ${app_bound}=		Get Bound Applications
    ${length} = 		Get Length	${app_bound}
    Should Be Equal As Integers	${length}	0


