*** Settings ***
Resource        Keywords.robot
Documentation   Test apps get automatically bound in standard enrollment mode
Force Tags      Service broker
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${INACTIVITY_IN_S}  30
${INACTIVITY}  PT${INACTIVITY_IN_S}S

*** Test Cases ***

1) Eligible app get automatically enrolled
    [Documentation]     Check that app is automatically bound by service instance
    Clean all service data
    Check broker is published
    Start application
	${regex}					Catenate   SEPARATOR=      ^(?:(?!    ${TESTED_APP_NAME}   ).)*$
    ${parameters}				Create Dictionary	idle-duration=${INACTIVITY}	exclude-from-auto-enrollment=${regex}	autosleep-despite-route-services-error=true
    Create service instance      ${parameters}
    Wait Until Keyword Succeeds     ${INACTIVITY_IN_S}s  3s  Should be bound


2) Excluded app don't get automatically enrolled
    [Documentation]        Check that no application is bound by the service instance
    Clean all service data
    Check broker is published
    Start application
    ${parameters}				Create Dictionary	idle-duration=${INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}	autosleep-despite-route-services-error=true
    Create service instance      ${parameters}
    ${halfPeriod}=      Evaluate  ${INACTIVITY_IN_S}/2
    Sleep                    ${halfPeriod}
    ${app_bound}=		Get Bound Applications
    ${length} = 		Get Length	${app_bound}
    Should Be Equal As Integers	${length}	0


3) Service does not bind stopped applications
    [Documentation]     Check that stopped applications are not bound
    Stop application
    ${parameters}				Create Dictionary	idle-duration=${INACTIVITY}	autosleep-despite-route-services-error=true
    Create service instance      ${parameters}
    Sleep                    ${INACTIVITY_IN_S}
    Should not be bound


