package templates.layouts
div(class: 'navbar') {
    div(class: 'navbar-inner') {
        div(class: "nav-collapse"){
            ul(class: "nav nav-tabs", id: "navigationTabs"){
                li(){
                    a(href: "/admin/debug/", "Services")
                }
            }
        }

    }
}