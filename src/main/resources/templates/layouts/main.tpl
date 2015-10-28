package templates.layouts
yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        title(pageTitle)
        link(rel: 'stylesheet', href:'/css/bootstrap-orange.min.css')
        link(rel:'stylesheet', href:'/css/bootstrap-orange-theme.min.css')
        link(rel:'stylesheet', href:'/css/boost.min.css')

        script(src: "/javascript/jquery.min.js", "")
        script(src: "/javascript/boost.min.js", "")
        script(src: "/javascript/main.js", "")
        additionalScripts()
    }
    body {
        div(class: 'container') {
            div(class: "alert alert-success", id: "successMessage", hidden: "true","")
            div(class: "alert alert-danger", id: "dangerMessage", hidden: "true", "")
            include template: 'layouts/navbar.tpl'
            mainBody()
        }
    }
}