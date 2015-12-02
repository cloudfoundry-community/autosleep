*** Settings ***
Resource        Keywords.robot
Documentation   Test no-optout option, and secret parameter
Force Tags      Service broker
Test Setup      Run Keywords  Clean all service data
Test Teardown   Run Keywords  Clean all service data

*** Variables ***
${DEFAULT_SECRET}  P@$$w0rd!
${DEFAULT_INACTIVITY_IN_S}  6
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S

*** Test Cases ***
1) No no-optout without secret
    [Documentation]              Check that we can not create a service with no-optout option without providing a secret
    ${parameters}                Create Dictionary	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}   no_optout=true
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \'no_optout\': *     Create service instance  ${parameters}


2) No-optout can unbind, but will be rebound
    [Documentation]     Check that we can not unbind an app from a service with no-optout option set to true
    # create service instance with noptout
    ${parameters}                Create Dictionary	inactivity=${DEFAULT_INACTIVITY}	secret=${DEFAULT_SECRET}   no_optout=true
    Create service instance      ${parameters}

    Bind application

    ## unbind service instance
    Unbind application


    ${longPeriod}=      Evaluate  ${DEFAULT_INACTIVITY_IN_S}*3

    Wait Until Keyword Succeeds     ${longPeriod}s  3s  Should be bound

3) No-optout service can not change without secret
    [Documentation]        Check that the no-optout option of a service can not be updated without providing a secret
    # create service instance  with noptout
    ${parameters}                Create Dictionary	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}   no_optout=true  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # update service -> refuse
    ${parameters}                Create Dictionary	no_optout=false
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \'secret\': *    Update service instance   ${parameters}


4) No-optout service can not change with wrong secret
    [Documentation]        Check that the no-optout option of a service can not be updated without providing the right secret
    # create service instance with noptout
    ${parameters}                Create Dictionary	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}   no_optout=true  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # update service -> refuse
    ${parameters}                Create Dictionary	no_optout=false     secret=whatsthepass
    Run Keyword And Expect Error    InvalidStatusCode: 502*Service broker error: \'secret\': *    Update service instance   ${parameters}


5) No-optout service can change with right secret
    [Documentation]        Check that the no-optout option can be changed if the right secret is provided
    # create service instance with noptout
    ${parameters}                Create Dictionary	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}   no_optout=true  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    Bind application


    # update with right secret
    ${parameters}                Create Dictionary	no_optout=false  secret=${DEFAULT_SECRET}
    Update service instance      ${parameters}


    # check unbind -> accept
    Unbind application

0) No-optout service can change with admin secret
    [Documentation]        Check that the no-optout option can be changed if the admin secret is provided
    # ask the tester for the admin secret
    #${adminSecret} = 	Get Value From User 	Enter admin secret: 	 hidden=yes
    ${adminSecret}=     Get Value From User On Console  Enter admin secret:
    # create service instance with noptout
    ${parameters}                Create Dictionary	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}   no_optout=true  secret=${DEFAULT_SECRET}
    Create service instance       ${parameters}

    # bound app
    Bind application

    # update with right secret
    ${parameters}                Create Dictionary	no_optout=false	secret=${adminSecret}

    # check unbind -> accept
    Unbind application
