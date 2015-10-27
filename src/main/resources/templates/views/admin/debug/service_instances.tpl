package templates.views.admin.debug

layout 'layouts/main.tpl',
        pageTitle: 'Service instances',
        additionalScripts: contents {
            script(src: '/javascript/services.js', "")
            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'
                yieldUnescaped 'window.onload = function(){'
                yieldUnescaped 'helper = new ServicesHelper("'
                yield "$pathServiceInstances"
                yieldUnescaped '", "'
                yield "$serviceDefinitionId"
                yieldUnescaped '", "'
                yield "$planId"
                yieldUnescaped '");'
                yieldUnescaped "helper.listServiceInstances();"
                yieldUnescaped '}'
            }
        },
        mainBody: contents {
            div(class: "row"){
                div(class: "col-xs-3"){
                    label("Organization Id : ")
                    br()
                    input(type:"text", id:"createServiceInstanceOrgGuid")
                }
                div(class: "col-xs-3"){
                    label("Space Id : ")
                    br()
                    input(type:"text", id:"createServiceInstanceSpaceGuid")
                }
                div(class: "col-xs-2"){
                    label("Service Instance Id : ")
                    br()
                    input(type:"text", id:"createServiceInstanceId")
                }
                div(class: "col-xs-2"){
                    label("Inactivity : ")
                    br()
                    input(type:"text", id:"createServiceInstancInactivity")
                }
                div(class: "col-xs-2"){
                    input(type:"submit", onclick:"helper.addServiceInstance(); return false;", value: "Add")
                }
            }

            div(class: "row", id:"allServiceInstances"){

            }
        }

