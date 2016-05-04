/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




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