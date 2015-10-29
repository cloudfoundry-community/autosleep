
function initNavbar(){
    $("#navigationTabs").children().each(function() {
        var $this = $(this);
        if($this.children().first().attr("href") == window.location.pathname)
            $this.addClass("active");
    });
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
