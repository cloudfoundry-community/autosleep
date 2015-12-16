package templates.views

layout 'layouts/main.tpl', true,
        pageTitle: 'Applications',
        noNavigation: skipNavigation,
        additionalScripts: contents {
            if (serviceInstance) {
                script(src: '/javascript/dashboard.js', "")
            } else {
                script(src: '/javascript/debug.js', "")
            }

            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'

                if (serviceInstance) {
                    yieldUnescaped 'var serviceInstance = "'
                    yield "$serviceInstance"
                    yieldUnescaped '";'
                }

                yieldUnescaped 'window.onload = function(){ '
                yieldUnescaped 'initNavbar();'
                if (serviceInstance) {
                    yieldUnescaped 'helper = new DashboardHelper();'
                    yieldUnescaped "helper.listApplications(serviceInstance);"
                } else{
                    yieldUnescaped 'helper = new DebugHelper("'
                    yield "$pathServiceInstances"
                    yieldUnescaped '", "'
                    yield "$serviceDefinitionId"
                    yieldUnescaped '", "'
                    yield "$planId"
                    yieldUnescaped '");'
                    yieldUnescaped "helper.listApplications();"
                }
                yieldUnescaped '}'
            }
        },
        mainBody: contents {

            div(class: "panel panel-default") {
                if (serviceInstance) {
                    div(class: "panel-heading") {
                        h3(class: "panel-title") {
                            yield "Service : $serviceInstance"
                        }
                        ul(class: "o-square-list") {
                            li {
                                span(class: "") {
                                    yield "idleDuration : $idleDuration"
                                }
                            }
                            li {
                                span {
                                    yield "forcedAutoEnrollment : $forcedAutoEnrollment"
                                }
                            }
                            li {
                                span(class: "") {
                                    yield "excludeFromAutoEnrollment : $excludeFromAutoEnrollment"
                                }
                            }
                        }
                    }
                }
                div(class: "panel-body") {
                    div(id: "allApplications") {
                    }
                }
            }

        }
