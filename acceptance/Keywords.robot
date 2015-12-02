*** Settings ***
Library         String
Library         Process


*** Variables ***
#autosleep under brokers-sandboxes
${SPACE_GUID}	2d745a4b-67e3-4398-986e-2adbcf8f7ec9
#default
${SERVICE_NAME}	autosleep
${PLAN_GUID}	0179eda6-bbcb-408a-aff9-d21d748056dc
${TESTED_APP_NAME}	static_test
${TESTED_APP_GUID}  3a4e9275-e937-4735-b272-84ddea21b1f6
${SERVICE_INSTANCE_NAME}  my-autosleep-acc
${DEFAULT_INACTIVITY_IN_S}  20
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S
${EXCLUDE_ALL_APP_NAMES}  .*
# Sometimes app instance aren't well synchronize. ${INACTIVITY_BUFFER_IN_S} will be added after inactivity, before checking anything
${INACTIVITY_BUFFER_IN_S}  20
&{DEFAULT_INSTANCE_PARAMETERS}	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}

*** Settings ***
Library			Cloudfoundry	${SPACE_GUID}	${TESTED_APP_GUID}	${SERVICE_NAME}	${PLAN_GUID}	${SERVICE_INSTANCE_NAME}   ${DEFAULT_INSTANCE_PARAMETERS}



*** Keywords ***
Get Value From User On Console
    [Arguments]    ${prompt}
    Evaluate    sys.__stdout__.write("""\n${prompt}""")    sys
    ${input}=    Evaluate    unicode(raw_input())
    [Return]    ${input}