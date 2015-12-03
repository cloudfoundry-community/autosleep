*** Settings ***
Resource        Keywords.robot
Documentation   Test no-optout option, and secret parameter
Force Tags      Service broker
Test Setup      Run Keywords  Clean
Test Teardown   Run Keywords  Clean

*** Variables ***
${DEFAULT_SECRET}  P@$$w0rd!
${DEFAULT_INACTIVITY_IN_S}  6
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S

*** Test Cases ***
1) No no-optout without secret
    [Documentation]              Check that we can not create a service with no-optout option without providing a secret
	${result} =                  Run Process  cf  cs  autosleep  default  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${DEFAULT_INACTIVITY}", "excludeAppNameRegExp" : "${EXCLUDE_ALL_APP_NAMES}", "no_optout" : "true"}
	Should Contain               ${result.stdout}    FAIL
	Should Contain               ${result.stdout}    502
	Should Be Equal As Integers  ${result.rc}    1

2) No-optout can unbind, but will be rebound
    [Documentation]     Check that we can not unbind an app from a service with no-optout option set to true
    # create service instance with noptout
    ${result} =                  Run Process  cf  cs  autosleep  default  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${DEFAULT_INACTIVITY}", "no_optout" : "true", "secret" : "${DEFAULT_SECRET}"}
	Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0

    Bind service instance

    ## unbind service instance
    ${result} =                 Run Process  cf  unbind-service  ${TESTED_APP_NAME}  ${SERVICE_INSTANCE_NAME}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0

    ${longPeriod}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}*3

    Wait Until Keyword Succeeds     ${longPeriod}s  3s  Check App Bound

3) No-optout service can not change without secret
    [Documentation]        Check that the no-optout option of a service can not be updated without providing a secret
    # create service instance  with noptout
    ${result} =                  Run Process  cf  cs  autosleep  default  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${DEFAULT_INACTIVITY}", "excludeAppNameRegExp" : "${EXCLUDE_ALL_APP_NAMES}", "no_optout" : "true", "secret" : "${DEFAULT_SECRET}"}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0
    # update service -> refuse
    ${result} =                  Run Process  cf  update-service  ${SERVICE_INSTANCE_NAME}  -c  {"no_optout" : "false"}
    Should Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    1

4) No-optout service can not change with wrong secret
    [Documentation]        Check that the no-optout option of a service can not be updated without providing the right secret
    # create service instance with noptout
    ${result} =                  Run Process  cf  cs  autosleep  default  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${DEFAULT_INACTIVITY}", "excludeAppNameRegExp" : "${EXCLUDE_ALL_APP_NAMES}", "no_optout" : "true", "secret" : "${DEFAULT_SECRET}"}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0
    # update service -> refuse
    ${result} =                  Run Process  cf  update-service  ${SERVICE_INSTANCE_NAME}  -c  {"no_optout" : "false", "secret" : "whatsthepass?"}
    Should Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    1

5) No-optout service can change with right secret
    [Documentation]        Check that the no-optout option can be changed if the right secret is provided
     # create service instance with noptout
    ${result} =                  Run Process  cf  cs  autosleep  default  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${DEFAULT_INACTIVITY}", "excludeAppNameRegExp" : "${EXCLUDE_ALL_APP_NAMES}", "no_optout" : "true", "secret" : "${DEFAULT_SECRET}"}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0

    # bound app
    Bind service instance


    # update with right secret
    ${result} =                  Run Process  cf  update-service  ${SERVICE_INSTANCE_NAME}  -c  { "no_optout" : "false", "secret" : "${DEFAULT_SECRET}"}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0

    # check unbind -> accept
    Unbind service instance

0) No-optout service can change with admin secret
    [Documentation]        Check that the no-optout option can be changed if the admin secret is provided
    # ask the tester for the admin secret
    #${adminSecret} = 	Get Value From User 	Enter admin secret: 	 hidden=yes
    ${adminSecret}=     Get Value From User On Console  Enter admin secret:
     # create service instance with noptout
    ${result} =                  Run Process  cf  cs  autosleep  default  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${DEFAULT_INACTIVITY}", "excludeAppNameRegExp" : "${EXCLUDE_ALL_APP_NAMES}", "no_optout" : "true", "secret" : "${DEFAULT_SECRET}"}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0

    # bound app
    Bind service instance

    # update with right secret
    ${result} =                  Run Process  cf  update-service  ${SERVICE_INSTANCE_NAME}  -c  { "no_optout" : "false", "secret" : "${adminSecret}"}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0

    # check unbind -> accept
    Unbind service instance
