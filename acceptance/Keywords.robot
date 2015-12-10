*** Settings ***
Library         String
Library         Process
Variables       Configuration.py

*** Variables ***
${DEFAULT_INACTIVITY_IN_S}  20
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S
${EXCLUDE_ALL_APP_NAMES}  .*
# Sometimes app instance aren't well synchronize. ${INACTIVITY_BUFFER_IN_S} will be added after inactivity, before checking anything
${INACTIVITY_BUFFER_IN_S}  20
&{DEFAULT_INSTANCE_PARAMETERS}	inactivity=${DEFAULT_INACTIVITY}	excludeAppNameRegExp=${EXCLUDE_ALL_APP_NAMES}

*** Settings ***
Library			Cloudfoundry	${CLIENT_ENDPOINT}    ${CLIENT_SKIP_SSL}	${CLIENT_USER}	${CLIENT_PASSWORD}      ${ORGANIZATION_NAME}    ${SPACE_NAME}	${TESTED_APP_NAME}	${SERVICE_NAME}	${PLAN_NAME}	${SERVICE_INSTANCE_NAME}   ${DEFAULT_INSTANCE_PARAMETERS}
Library         Autosleep       ${AUTOSLEEP_ENDPOINT}   ${USER_NAME}   ${USER_PASSWORD}   ${TESTED_APP_NAME}


*** Keywords ***
