package templates.views.admin

layout 'layouts/main.tpl',
        pageTitle: 'Configuration',
        additionalScripts: contents {
            script(src: '/javascript/configuration.js', "")
            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'
                yieldUnescaped 'window.onload = function(){'
                yieldUnescaped 'initNavbar();'
                if(clientConfiguration == null){
                    yieldUnescaped '$("#cleanCredentialsBtn").hide();'
                }
                yieldUnescaped '}'
            }
        },
        mainBody: contents {
            form(role : "form") {
                div(class : "form-group") {
                    label(for: "targetEndpoint", "Target endpoint")
                    input(type: "url", id:"targetEndpoint", value: (clientConfiguration == null? "" : "${clientConfiguration.targetEndpoint}"))
                }
                div(class: "checkbox") {
                    label(){
                        input(id: "enableSelfSignedCertificates", type: "checkbox", checked: (clientConfiguration == null? "false" : "${clientConfiguration.enableSelfSignedCertificates}" )){
                            yield "Enable Self Signed"
                        }
                    }
                }

                div(class : "form-group") {
                    label(for: "clientId", "Client Id")
                    input(type: "text", id:"clientId", value: (clientConfiguration == null? "" : "${clientConfiguration.clientId}"))
                }

                div(class : "form-group") {
                    label(for: "clientSecret", "Client Secret")
                    input(type: "password", id:"clientSecret", value: "")
                }

                div(class : "form-group") {
                    label(for: "username", "Username")
                    input(type: "text", id:"username", value: (clientConfiguration == null? "" : "${clientConfiguration.username}"))
                }

                div(class : "form-group") {
                    label(for: "password", "Password")
                    input(type: "password", id:"password", value: "")
                }

                button (class: "btn", type: "button", onclick: "cleanCredentials()", id: "cleanCredentialsBtn") {
                    yield "Clear"
                }

                button (class: "btn btn-default", type: "button", onclick: "setCredentials()", id: "setCredentialsBtn"){
                    yield clientConfiguration == null? "Set" : "Update"
                }



            }
        }
