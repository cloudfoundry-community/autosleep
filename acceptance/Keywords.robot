*** Settings ***
Library         String
Library         Process


*** Variables ***
#autosleep under brokers-sandboxes
${ORGANIZATION_NAME}    brokers-sandboxes
${SPACE_NAME}	autosleep
${SERVICE_NAME}	autosleep
${PLAN_NAME}	default
${TESTED_APP_NAME}	static_test
${SERVICE_INSTANCE_NAME}  my-autosleep-acc
${DEFAULT_INACTIVITY_IN_S}  20
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S
${EXCLUDE_ALL_APP_NAMES}  .*
# Sometimes app instance aren't well synchronize. ${INACTIVITY_BUFFER_IN_S} will be added after inactivity, before checking anything
${INACTIVITY_BUFFER_IN_S}  20
&{DEFAULT_INSTANCE_PARAMETERS}	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}

*** Settings ***
Library			Cloudfoundry	${ORGANIZATION_NAME}    ${SPACE_NAME}	${TESTED_APP_NAME}	${SERVICE_NAME}	${PLAN_NAME}	${SERVICE_INSTANCE_NAME}   ${DEFAULT_INSTANCE_PARAMETERS}



*** Keywords ***
Get Value From User On Console
    [Arguments]    ${prompt}
    Evaluate    sys.__stdout__.write("""\n${prompt}""")    sys
    ${input}=    Evaluate    unicode(raw_input())
    [Return]    ${input}