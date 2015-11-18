package templates.views.admin.debug

layout 'layouts/main.tpl',
        pageTitle: "Service bindings of service $serviceInstance",
        additionalScripts: contents {
            script(src: '/javascript/debug.js', "")
            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'
                yieldUnescaped 'var serviceInstance = "'
                yield "$serviceInstance"
                yieldUnescaped '";'
                yieldUnescaped 'window.onload = function(){'
                yieldUnescaped 'initNavbar();'
                yieldUnescaped 'helper = new DebugHelper("'
                yield "$pathServiceInstances"
                yieldUnescaped '", "'
                yield "$serviceDefinitionId"
                yieldUnescaped '", "'
                yield "$planId"
                yieldUnescaped '");'
                yieldUnescaped "helper.listServiceBindings(serviceInstance);"
                yieldUnescaped '}'
            }
        },
        mainBody: contents {
            div(class: "row"){
                div(class: "col-xs-3"){
                    label("Organization Id : ")
                    br()
                    input(type:"text", id:"createServiceBindingOrgGuid")
                }
                div(class: "col-xs-3"){
                    label("Space Id : ")
                    br()
                    input(type:"text", id:"createServiceBindingSpaceGuid")
                }
                div(class: "col-xs-2"){
                    label("Service Binding Id : ")
                    br()
                    input(type:"text", id:"createServiceBindingId")
                }
                div(class: "col-xs-2"){
                    label("App Guid : ")
                    br()
                    input(type:"text", id:"createServiceBindingAppGuid")
                }
                div(class: "col-xs-2"){
                    input(type:"submit", onclick:"helper.addServiceBinding('$serviceInstance'); return false;", value: "Add")
                }
            }

            div(id:"allServiceBindings"){

            }
        }

