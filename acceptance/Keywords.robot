*** Settings ***
Library         String
Library         Process
Variables       ./Configuration.py

*** Variables ***
${DEFAULT_INACTIVITY_IN_S}  20
${DEFAULT_INACTIVITY}  PT${DEFAULT_INACTIVITY_IN_S}S
${EXCLUDE_ALL_APP_NAMES}  .*
# Sometimes app instance aren't well synchronize. ${INACTIVITY_BUFFER_IN_S} will be added after inactivity, before checking anything
${INACTIVITY_BUFFER_IN_S}  20
&{DEFAULT_INSTANCE_PARAMETERS}	idle-duration=${DEFAULT_INACTIVITY}	exclude-from-auto-enrollment=${EXCLUDE_ALL_APP_NAMES}	autosleep-despite-route-services-error=true

*** Settings ***
Library			./Cloudfoundry.py	${CLIENT_ENDPOINT}    ${CLIENT_SKIP_SSL}	${CLIENT_USER}	${CLIENT_PASSWORD}      ${ORGANIZATION_NAME}    ${SPACE_NAME}	${TESTED_APP_NAME}	${AUTOSLEEP_ENDPOINT}	${SERVICE_NAME}  ${USER_NAME}  ${USER_PASSWORD}	 ${SERVICE_INSTANCE_NAME}  ${DEFAULT_INSTANCE_PARAMETERS}
Library         ./Autosleep.py       ${AUTOSLEEP_ENDPOINT}   ${USER_NAME}   ${USER_PASSWORD}   ${TESTED_APP_NAME}


*** Keywords ***
