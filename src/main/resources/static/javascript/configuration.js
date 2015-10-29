


function setCredentials(){
    var data = {
        targetEndpoint: $("#targetEndpoint").val(),
        enableSelfSignedCertificates: $("#enableSelfSignedCertificates").prop("checked"),
        clientId : $("#clientId").val(),
        clientSecret : $("#clientSecret").val(),
        username : $("#username").val(),
        password : $("#password").val()

    };
    $.ajax({
        url : "/admin/configuration/",
        type : 'PUT',
        contentType  : 'application/json; charset=UTF-8',
        data : JSON.stringify(data),
        success : function () {
            displaySuccess("Credential successfully updated");
            $("#setCredentialsBtn").html("Update");
            $("#cleanCredentialsBtn").show();
        },
        error : function(xhr){
            displayDanger("Error while updating credentials: "+xhr.responseText);
        }
    });
}


function cleanCredentials(){
    $.ajax({
        url : "/admin/configuration/",
        type : 'DELETE',
        contentType  : 'application/json; charset=UTF-8',
        data : "",
        success : function () {
            displaySuccess("Credential successfully cleaned");
            $("#setCredentialsBtn").html("Set");
            $("#cleanCredentialsBtn").hide();
        },
        error : function(xhr){
            displayDanger("Error while cleaning credentials: "+xhr.responseText);
        }
    });
}