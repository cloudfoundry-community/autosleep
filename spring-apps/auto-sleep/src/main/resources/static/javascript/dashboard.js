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



function listApplications (id){
    var targetUrl =  "/api/services/"+id +"/applications/";
    var that = this;
    $.ajax({
        url : targetUrl,
        success : function (serverResponse) {
            var container = $("#allApplications");
            var row ;
            container.empty();
            var diffWithServer = new Date().getTime() - serverResponse.time;
            if(serverResponse.body.length > 0){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 h5 text-center").html("Guid"));
                row.append($("<div>").addClass("col-xs-2 h5 text-center").html("Name"));
                row.append($("<div>").addClass("col-xs-1 h5 text-center").html("Last known status"));
                row.append($("<div>").addClass("col-xs-2 h5 text-center").html("Next check"));
                row.append($("<div>").addClass("col-xs-1 h5 text-center").html("State"));
                row.append($("<div>").addClass("col-xs-1"));//last log details
                row.append($("<div>").addClass("col-xs-1"));//last event details
                container.append(row);
            }
            $.each(serverResponse.body, function(idx, application){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 text-center").html(application.uuid));
                row.append($("<div>").addClass("col-xs-2 text-center").html(application.name));
                if (application.watched) {
                    row.append($("<div>").addClass("col-xs-1 text-center").html(application.diagnosticInfo.appState));
                } else
                    row.append($("<div>").addClass("col-xs-1 text-center").html("-"));

                if(application.diagnosticInfo.nextCheck != null){
                    row.append($("<div>")
                        .attr("data-countdown",application.diagnosticInfo.nextCheck + diffWithServer, "id","countdown"+idx)
                        .addClass("col-xs-2 text-center"));
                }else
                    row.append($("<div>").addClass("col-xs-2 text-center").html("-"));

                var stateElement = $("<span>").attr("data-toggle","tooltip")
                    .attr("title",application.watched?"WATCHED":"IGNORED")
                    .addClass("col-xs-1 text-center glyphicon");

                if (!application.watched) {
                    stateElement.addClass("glyphicon-eye-close");
                } else {
                    stateElement.addClass("glyphicon-eye-open");
                }
                row.append(stateElement);

                var logElement = $("<a>").attr("data-toggle","tooltip")
                    .attr("title", "Last log")
                    .addClass("col-xs-1 text-center glyphicon glyphicon-list-alt");
                var dialogContent = "No last log known...";
                if(application.diagnosticInfo.lastLog != null) {
                    var time = new Date(application.diagnosticInfo.lastLog.timestamp);
                    dialogContent = '<dl class="dl-horizontal">' +
                        '<dt>Timestamp: </dt><dd>'+time+'</dd>' +
                        '<dt>Type: </dt><dd>'+application.diagnosticInfo.lastLog.messageType +'</dd>' +
                        '<dt>Message: </dt><dd>'+application.diagnosticInfo.lastLog.message +'</dd>' +
                        '<dt>Source: </dt><dd>'+application.diagnosticInfo.lastLog.sourceName +'</dd>' +
                        '</dl>';
                }
                logElement.click(
                    function(dialogContent){
                        return function(){
                            bootbox.dialog({
                                title: "Last application log: ",
                                message: dialogContent
                            });
                        }}(dialogContent));
                row.append(logElement);

                var eventElement = $("<a>").attr("data-toggle","tooltip")
                    .attr("title", "Last cloud event")
                    .addClass("col-xs-1 text-center glyphicon glyphicon-cloud");
                dialogContent = "No cloud event known...";
                if(application.diagnosticInfo.lastEvent != null) {
                    var time = new Date(application.diagnosticInfo.lastEvent.timestamp);
                    dialogContent = '<dl class="dl-horizontal">' +
                        '<dt>Timestamp: </dt><dd>'+time+'</dd>' +
                        '<dt>Name: </dt><dd>'+application.diagnosticInfo.lastEvent.name +'</dd>' +
                        '<dt>Type: </dt><dd>'+application.diagnosticInfo.lastEvent.type +'</dd>' +
                        '<dt>Actor: </dt><dd>'+application.diagnosticInfo.lastEvent.actor +'</dd>' +
                    '</dl>';
                }
                eventElement.click(
                    function(dialogContent){
                        return function(){
                            bootbox.dialog({
                                title: "Last cloud event: ",
                                message: dialogContent
                            });
                        }}(dialogContent));
                row.append(eventElement);
                container.append(row);
            });

            $('[data-countdown]').each(function() {
                var $this = $(this), finalDate = $(this).data('countdown');
                $this.countdown(finalDate)
                    .on('update.countdown', function(event) {
                        $this.html(event.strftime('%D days %H:%M:%S'));
                    }).on('finish.countdown', function() {
                        setTimeout(function(){
                            that.listApplications(id);
                        }, 1000);
                    });
            });

            $('[data-toggle="tooltip"]').tooltip();
        },
        error : function(xhr){
            displayDanger("Error listing applications: "+xhr.responseText);
        }
    });
}