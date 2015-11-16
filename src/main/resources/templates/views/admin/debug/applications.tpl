package templates.views.admin.debug

layout 'layouts/main.tpl',
        pageTitle: 'Applications',
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
                yieldUnescaped "helper.listApplications();"
                yieldUnescaped '}'
            }
        },
        mainBody: contents {

            div(id:"allApplications"){

            }
        }

