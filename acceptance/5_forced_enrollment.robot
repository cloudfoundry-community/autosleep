*** Settings ***
Resource        Keywords.robot
Documentation   Users can only transiently opt-out
Force Tags      Service broker
Test Setup      Run Keywords  Clean all service data  Check broker is published
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${DEFAULT_SECRET}  P@$$w0rd!
${DEFAULT_INACTIVITY_IN_S}  6
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S

*** Test Cases ***
1) No forced auto-enrollment without secret
    [Documentation]              Check that we can not create a service with auto-enrollment=forced option without providing a secret
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced   autosleep-despite-route-services-error=true
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \'auto-enrollment\': *     Create service instance  ${parameters}


2) app opt-opt outs are transient
    [Documentation]     In forced auto-enrollment, check that we can not permamently unbind an app from a service:
    ...                 Applications will be automatically rebound
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	secret=${DEFAULT_SECRET}   auto-enrollment=forced
    Create service instance      ${parameters}

    Bind application

    ## unbind service instance
    Unbind application


    ${longPeriod}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}*3

    Wait Until Keyword Succeeds     ${longPeriod}s  3s  Should be bound


3) can not delete autosleep service instance
    [Documentation]        Check that during forced auto-enrollment mode, a service instance can't be deleted to escape from autoenrolling apps within the space
    # create service instance  with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # delete service -> refuse
    ${error_delete}=    Run Keyword And Expect Error    *       Delete service instance
    Should Match        ${error_delete}     InvalidStatusCode: 502*Service broker error: this autosleep service instance can't be deleted during forced enrollment mode. Switch back to normal enrollment mode to allow its deletion.*


4) Auto-enrollment mode can not change with wrong secret
    [Documentation]        Check that the auto-enrollment option of a service can not be updated without providing the right secret
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # update service -> refuse
    ${parameters}                Create Dictionary	auto-enrollment=standard     secret=whatsthepass
    ${error_update}=    Run Keyword And Expect Error        *       Update service instance     ${parameters}
    Should Match    ${error_update}         InvalidStatusCode: 502*Service broker error: \'secret\': *


5) Auto-enrollment mode can change with right secret
    [Documentation]        Check that the auto-enrollment option can be changed if the right secret is provided
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    Bind application


    # update with right secret
    ${parameters}                Create Dictionary	auto-enrollment=standard  secret=${DEFAULT_SECRET}
    Update service instance      ${parameters}


    # check unbind -> accept
    Unbind application

6) Auto-enrollment mode can change with admin secret
    [Documentation]        Check that the auto-enrollment option can be changed if the admin secret is provided
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # bound app
    Bind application

    # update with right admin secret
    ${parameters}                Create Dictionary	auto-enrollment=standard	secret=${USER_PASSWORD}

    # check unbind -> accept
    Unbind application
