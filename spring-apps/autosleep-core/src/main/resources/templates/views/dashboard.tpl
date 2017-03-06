package templates.views

layout 'layouts/main.tpl',
        pageTitle: 'Applications',
        noNavigation: true,
        additionalScripts: contents {
            script(src: '/javascript/bootbox.min.js',"")
            script(src: '/javascript/dashboard.js', "")


            script(type: "text/javascript") {
                yieldUnescaped 'var helper = null;'

                yieldUnescaped 'var serviceInstance = "'
                yield "$serviceInstance"
                yieldUnescaped '";'

                yieldUnescaped 'window.onload = function(){ '
                yieldUnescaped 'initNavbar();'
                yieldUnescaped "listApplications(serviceInstance);"
                yieldUnescaped '}'
            }
        },
        mainBody: contents {
            div(class: "panel panel-default") {
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
                                yield "enrollment : $enrollment"
                            }
                        }
                        li {
                            span(class: "") {
                                yield "excludeFromAutoEnrollment : $excludeFromAutoEnrollment"
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
