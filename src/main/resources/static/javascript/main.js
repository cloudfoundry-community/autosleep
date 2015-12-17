
function initNavbar(){
    var lastActive = null;
    $("#navigationTabs").children().each(function() {
        var $this = $(this);
        if(window.location.pathname.indexOf($this.children().first().attr("href") ) == 0){
            lastActive = $this;
        }
    });
    if(lastActive != null){
        lastActive.addClass("active");
    }
}


function displaySuccess(message){
    $("#dangerMessage").hide();
    $("#successMessage").html(message);
    $("#successMessage").show();
    setTimeout(function(){$("#successMessage").hide();},5000);
}

function displayDanger(message){
    $("#successMessage").hide();
    $("#dangerMessage").html(message);
    $("#dangerMessage").show();
    setTimeout(function(){$("#dangerMessage").hide();},5000);
}
