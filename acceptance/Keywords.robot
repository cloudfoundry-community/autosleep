*** Settings ***
Library         String
Library         Process

*** Variables ***
${TESTED_APP_NAME}  static_test
${SERVICE_INSTANCE_NAME}  my-autosleep-acc
${DEFAULT_INACTIVITY}  PT20S
${DEFAULT_INACTIVITY_IN_S}  20
# Sometimes app instance aren't well synchronize. ${INACTIVITY_BUFFER_IN_S} will be added after inactivity, before checking anything
${INACTIVITY_BUFFER_IN_S}  20

*** Keywords ***
Create service instance
    [Documentation]             Create a service instance, checking that it doesn't fail
	[Arguments]                 ${inactivity}=${DEFAULT_INACTIVITY}
	${result} =                 Run Process  cf  cs  autosleep  opt-in  ${SERVICE_INSTANCE_NAME}  -c  {"inactivity": "${inactivity}"}
	Should Not Contain          ${result.stdout}    FAIL
	Should Be Equal As Integers    ${result.rc}    0
	[Return]                    ${result.rc}

Bind service instance
    [Documentation]             Bind tested app to basic service instance, checking that it doesn't fail
	${result} =                 Run Process  cf  bind-service  ${TESTED_APP_NAME}  ${SERVICE_INSTANCE_NAME}
    Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0
    [Return]                    ${result.rc}

Unbind service instance
    [Documentation]             Unbind, checking it doesn't fail
	${result} =                 Run Process  cf  unbind-service  ${TESTED_APP_NAME}  ${SERVICE_INSTANCE_NAME}
	Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0
    [Return]                    ${result.rc}

Delete service instance
    [Documentation]             Delete base service instance, checking it doesn't fail
	${result} =                 Run Process  cf  delete-service  -f  ${SERVICE_INSTANCE_NAME}
	Should Not Contain          ${result.stdout}    FAIL
    Should Be Equal As Integers  ${result.rc}    0
    [Return]                    ${result.rc}

Clean
    [Documentation]     Try to unbind and delete base service instance, no matter if it fails
	Run Process         cf  unbind-service  ${TESTED_APP_NAME}  ${SERVICE_INSTANCE_NAME}
	Run Process         cf  delete-service  -f  ${SERVICE_INSTANCE_NAME}

Check App Started
    [Documentation]     Return true if cf app is started
    [Arguments]         ${name}
	${result} =         Run Process         cf  app  ${name}
	Should Contain  ${result.stdout}    started

Check App Stopped
    [Documentation]     Return true if cf app is stopped
    [Arguments]         ${name}
	${result} =         Run Process         cf  app  ${name}
	Should Contain  ${result.stdout}    stopped

Restart App
    [Documentation]     Restart App
    [Arguments]         ${name}
    ${result} =         Run Process         cf  restart  ${name}
    Log  ${result.stdout}
    Should Be Equal As Integers  ${result.rc}    0

Simulate HTTP Activity
    [Documentation]     Curl first route of this app
    [Arguments]         ${name}
    ${appdata} =         Run Process         cf  app  ${name}
    Log  ${appdata.stdout}
    Should Be Equal As Integers  ${appdata.rc}    0

    ${urlsList}=	Get Regexp Matches	${appdata.stdout}	urls: (.*)
    ${onlyUrls}=    Get Substring   ${urlsList[0]}  6
    @{urls} =	Split String	${onlyUrls}	 ,

    ${result} =         Run Process         curl  ${urls[0]}
    Should Be Equal As Integers  ${result.rc}    0