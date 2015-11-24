package templates.layouts
yieldUnescaped '<!DOCTYPE html>'

html {
    head {
        title(pageTitle)
        link(rel: 'stylesheet', href:'/css/bootstrap-orange.min.css')
        link(rel:'stylesheet', href:'/css/bootstrap-orange-theme.min.css')
        link(rel:'stylesheet', href:'/css/boost.min.css')
        link(rel:'stylesheet', href:'/css/button-circle.css')

        script(src: "/javascript/jquery.min.js", "")
        script(src: "/javascript/boost.min.js", "")
        script(src: "/javascript/main.js", "")
        script(src: "/javascript/jquery.countdown.min.js", "")
        additionalScripts()
    }
    body {
        div(class: 'container') {
            div(class: "alert alert-success", id: "successMessage", hidden: "true","")
            div(class: "alert alert-danger", id: "dangerMessage", hidden: "true", "")
            if (!noNavigation) {
                include template: 'layouts/navbar.tpl'
            }
            mainBody()
        }
    }
}