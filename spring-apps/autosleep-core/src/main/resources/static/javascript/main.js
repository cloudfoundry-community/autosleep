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
