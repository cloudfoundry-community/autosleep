*** Settings ***
Resource        Keywords.robot
Documentation   Test no-optout option, and secret parameter
Force Tags      Service broker
Test Setup      Run Keywords  Clean all service data  Check broker is published
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${DEFAULT_SECRET}  P@$$w0rd!
${DEFAULT_INACTIVITY_IN_S}  6
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S

*** Test Cases ***
1) No forced auto-enrollment without secret
    [Documentation]              Check that we can not create a service with no-optout option without providing a secret
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced   autosleep-despite-route-services-error=true
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \'auto-enrollment\': *     Create service instance  ${parameters}


2) Forced auto-enrollment can unbind, but will be rebound
    [Documentation]     Check that we can not unbind an app from a service with auto-enrollment option set to true
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	secret=${DEFAULT_SECRET}   auto-enrollment=forced
    Create service instance      ${parameters}

    Bind application

    ## unbind service instance
    Unbind application


    ${longPeriod}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}*3

    Wait Until Keyword Succeeds     ${longPeriod}s  3s  Should be bound

3) Forced auto-enrollment can unbind, but will be rebound
    [Documentation]     Check that we can not unbind an app from a service with auto-enrollment option set to true
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	secret=${DEFAULT_SECRET}   auto-enrollment=forced
    Create service instance      ${parameters}

    Bind application

    ## unbind service instance
    Unbind application


    ${longPeriod}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}*3

    Wait Until Keyword Succeeds     ${longPeriod}s  3s  Should be bound

4) Forced auto-enrollment mode can not delete autosleep service instance
    [Documentation]        Check that during forced auto-enrollment mode, a service instance can't be deleted to escape from autoenrolling apps within the space
    # create service instance  with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # delete service -> refuse
    Delete service instance
    ${maxToWait}=      Evaluate  2*${DEFAULT_INACTIVITY_IN_S}
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \this autosleep service instance can't be delete during forced enrollment mode. Switch back to normal enrollment mode to enable deletes


5) Auto-enrollment mode can not change with wrong secret
    [Documentation]        Check that the auto-enrollment option of a service can not be updated without providing the right secret
    # create service instance with noptout
    ${parameters}                Create Dictionary	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}   auto-enrollment=forced  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # update service -> refuse
    ${parameters}                Create Dictionary	auto-enrollment=standard     secret=whatsthepass
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \'secret\': *    Update service instance   ${parameters}


6) Auto-enrollment mode can change with right secret
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

7) Auto-enrollment service can change with admin secret
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
