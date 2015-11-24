package templates.views

layout 'layouts/main.tpl', true,
        pageTitle: 'Applications',
        noNavigation: skipNavigation,
        additionalScripts: contents {
            script(src: '/javascript/debug.js', "")
            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'

                if (serviceInstance) {
                    yieldUnescaped 'var serviceInstance = "'
                    yield "$serviceInstance"
                    yieldUnescaped '";'
                }

                yieldUnescaped 'window.onload = function(){ '
                yieldUnescaped 'initNavbar();'
                yieldUnescaped 'helper = new DebugHelper("'
                yield "$pathServiceInstances"
                yieldUnescaped '", "'
                yield "$serviceDefinitionId"
                yieldUnescaped '", "'
                yield "$planId"
                yieldUnescaped '");'

                if (serviceInstance) {
                    yieldUnescaped "helper.listApplicationsById(serviceInstance);"
                } else {
                    yieldUnescaped "helper.listAllApplications();"
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
                                    yield "interval : $interval"
                                }
                            }
                            li {
                                span {
                                    yield "no optout : $noOptout"
                                }
                            }
                            li {
                                span(class: "") {
                                    yield "exclude : $excludeNames"
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
