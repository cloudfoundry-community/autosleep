yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        title(pageTitle)
        link(rel: 'stylesheet', href:'/css/bootstrap-orange.min.css')
        link(rel:'stylesheet', href:'/css/bootstrap-orange-theme.min.css')
        link(rel:'stylesheet', href:'/css/boost.min.css')

        script(src: "/javascript/jquery.min.js", "")
        script(src: "/javascript/boost.min.js", "")
        additionalScripts()
    }
    body {
        div(class: 'container') {
            div(class: 'navbar') {
                div(class: 'navbar-inner') {
                    a(class: 'brand',
                            href: '/admin/debug/services_instances/',
                            'Services')
                }
            }
            mainBody()
        }
    }
}