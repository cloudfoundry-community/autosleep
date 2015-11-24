package templates.views.admin.debug

layout 'layouts/main.tpl',
        pageTitle: 'Service instances',
        additionalScripts: contents {
            script(src: '/javascript/debug.js', "")
            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'
                yieldUnescaped 'window.onload = function(){'
                yieldUnescaped 'initNavbar();'
                yieldUnescaped 'helper = new DebugHelper("'
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
                div(class: "col-xs-2"){
                    label("Organization Id : ")
                    br()
                    input(type:"text", id:"createServiceInstanceOrgGuid")
                }
                div(class: "col-xs-2"){
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
                    input(type:"text", id:"createServiceInstanceInactivity")
                }
                div(class: "col-xs-2"){
                    label("Exclude names : ")
                    br()
                    input(type:"text", id:"createServiceInstanceExclusion")
                }
                div(class: "col-xs-2"){
                    label("")
                    br()
                    input(type:"submit", class:"btn btn-default", onclick:"helper.addServiceInstance(); return false;", value: "Add")
                }
            }

            div(id:"allServiceInstances"){

            }
        }

