

function DebugHelper  (pathServiceInstance, serviceDefinitionId, planId) {
    this.pathDebugListInstances = "/api/services/";
    this.pathDashboardPfx = "/dashboard/";
    this.pathApiByServicePfx = "/api/services/";
    this.pathApiListBindingSfx = "/bindings/";
    this.pathApiListApplicationSfx = "/applications/";
    this.pathApiListApplications = "/api/applications/";
    this.pathDebugPageServiceBindingsPfx = "/admin/debug/";
    this.pathDebugPageServiceBindingsSfx = "/bindings/";
    this.pathServiceInstance = pathServiceInstance;
    this.serviceDefinitionId = serviceDefinitionId;
    this.planId = planId;
    console.log("DebugHelper - "+serviceDefinitionId+" - "+planId);
}

DebugHelper.prototype.listApplicationsById = function(serviceInstanceId){
    var targetUrl =  this.pathApiByServicePfx+serviceInstanceId +this.pathApiListApplicationSfx;
    this.listApplications(targetUrl,false);
};

DebugHelper.prototype.listAllApplications = function(){
    var targetUrl =  this.pathApiListApplications;
    this.listApplications(targetUrl,true);
};


DebugHelper.prototype.listApplications = function (targetUrl, showDeleteButton ){
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
                row.append($("<div>").addClass("col-xs-2 h5 text-center").html("Last known status"));
                row.append($("<div>").addClass("col-xs-2 h5 text-center").html("Next check"));
                row.append($("<div>").addClass("col-xs-1 h5 text-center").html("State"));
                row.append($("<div>").addClass("col-xs-1"));
                container.append(row);
            }
            $.each(serverResponse.body, function(idx, application){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 text-center").html(application.uuid));
                row.append($("<div>").addClass("col-xs-2 text-center").html(application.name));
                if (application.stateMachine.state != "IGNORED") {
                    row.append($("<div>").addClass("col-xs-2 text-center").html(application.appState));
                } else
                    row.append($("<div>").addClass("col-xs-2 text-center").html("-"));

                if(application.nextCheck != null){
                    row.append($("<div>")
                        .attr("data-countdown",application.nextCheck + diffWithServer, "id","countdown"+idx)
                        .addClass("col-xs-2 text-center"));
                }else
                    row.append($("<div>").addClass("col-xs-2 text-center").html("-"));

                var stateElement = $("<span>").attr("data-toggle","tooltip")
                    .attr("title",application.stateMachine.state)
                    .addClass("col-xs-1 text-center glyphicon");

                    if (application.stateMachine.state === "IGNORED") {
                        stateElement.addClass("glyphicon-eye-close");
                    } else {
                        stateElement.addClass("glyphicon-eye-open");
                    }

                row.append(stateElement);

                if(showDeleteButton){
                    var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                        .append($("<i>").addClass("glyphicon glyphicon-remove text-center"));
                    row.append($("<div>").addClass("col-xs-1").append(button));
                    button.on("click", function(e){
                        e.preventDefault();
                        that.deleteApplication(application.uuid);
                    });
                }

                container.append(row);
            });

            $('[data-countdown]').each(function() {
                 var $this = $(this), finalDate = $(this).data('countdown');
                $this.countdown(finalDate)
                    .on('update.countdown', function(event) {
                        $this.html(event.strftime('%D days %H:%M:%S'));
                    }).on('finish.countdown', function() {
                        that.listApplications(targetUrl, showDeleteButton);
                    });
               });

            $('[data-toggle="tooltip"]').tooltip();
        },
        error : function(xhr){
            displayDanger("Error listing applications: "+xhr.responseText);
        }
    });
};

DebugHelper.prototype.deleteApplication = function (applicationId) {
    var that = this;
    $.ajax({
        url : this.pathApiListApplications+applicationId,
        type : 'DELETE',
        success : function () {
            displaySuccess("application deleted");
            that.listApplications();
        },
        error : function(xhr){
            displayDanger("Error deleting application: "+xhr.responseText);
        }
    });
};


DebugHelper.prototype.addServiceInstance = function(){
    var that = this;
    var data = {
        service_id : this.serviceDefinitionId,
        plan_id : this.planId,
        organization_guid : $("#createServiceInstanceOrgGuid").val(),
        space_guid : $("#createServiceInstanceSpaceGuid").val(),
        parameters : {
            inactivity : $("#createServiceInstanceInactivity").val(),
            excludeAppNameRegExp : $("#createServiceInstanceExclusion").val()
        }
    };
    $.ajax({
        url : this.pathServiceInstance+"/"+$("#createServiceInstanceId").val(),
        type : 'PUT',
        contentType  : 'application/json; charset=UTF-8',
        data : JSON.stringify(data),
        success : function (data) {
            displaySuccess("Service instance created");
            that.listServiceInstances();
        },
        error : function(xhr){
            displayDanger("Error adding service instance: "+xhr.responseText);
        }
    });
};



DebugHelper.prototype.listServiceInstances = function(){
    var that = this;
    $.ajax({
        url : this.pathDebugListInstances,
        success : function (serverResponse) {
            var container = $("#allServiceInstances");
            var row;
            container.empty();

            if(serverResponse.body.length > 0){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 h5 text-center").html("Instance Id"));
                row.append($("<div>").addClass("col-xs-2 h5 text-center").html("Plan Id"));
                row.append($("<div>").addClass("col-xs-1 h5 text-center").html("Interval"));
                row.append($("<div>").addClass("col-xs-1 h5 text-center").html("Exclude"));
                row.append($("<div>").addClass("col-xs-1 h5 text-center").html("No-Optout"));
                row.append($("<div>").addClass("col-xs-1"));//binding links
                row.append($("<div>").addClass("col-xs-1"));//dashboard links
                row.append($("<div>").addClass("col-xs-1"));//delete buttons
                container.append(row);
            }
            $.each(serverResponse.body, function(idx, serviceInstance){
                var linkToDashboard = $("<a>", {href : that.pathDashboardPfx+serviceInstance.service_instance_id
                }).addClass("glyphicon glyphicon-dashboard");

                var linkToBindings = $("<a>", {href : that.pathDebugPageServiceBindingsPfx+serviceInstance.service_instance_id
                +that.pathDebugPageServiceBindingsSfx}).addClass("glyphicon glyphicon-paperclip");

                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4").html(serviceInstance.service_instance_id));
                row.append($("<div>").addClass("col-xs-2 text-center").html(serviceInstance.plan_id));
                row.append($("<div>").addClass("col-xs-1 text-center").html(serviceInstance.interval));
                row.append($("<div>").addClass("col-xs-1 text-center").html(serviceInstance.excludeNames));
                row.append($("<div>").addClass("col-xs-1 text-center").html(serviceInstance.noOptOut.toString()));

                row.append($("<div>").addClass("col-xs-1 text-center").attr("data-toggle","tooltip")
                    .attr("title","bindings").append(linkToBindings));
                row.append($("<div>").addClass("col-xs-1 text-center").attr("data-toggle","tooltip")
                    .attr("title","dashboard").append(linkToDashboard));

                var button = $("<button>", {type : "button"}).addClass("btn btn-circle").attr("data-toggle","tooltip")
                    .attr("title","delete")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                row.append($("<div>").addClass("col-xs-1 text-center").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteServiceInstance(serviceInstance.service_instance_id);
                });
                container.append(row);
            });
            $('[data-toggle="tooltip"]').tooltip();
        },
        error : function(xhr){
            displayDanger("Error listing service instances: "+xhr.responseText);
        }
    });
};

DebugHelper.prototype.deleteServiceInstance = function(serviceInstanceId){
    var that = this;
    $.ajax({
        url : this.pathServiceInstance+"/"+serviceInstanceId
                + "?service_id="+this.serviceDefinitionId+"&plan_id="+this.planId,
        type : 'DELETE',
        success : function () {
            displaySuccess("Service instance deleted");
            that.listServiceInstances();
        },
        error : function(xhr){
            displayDanger("Error deleting service instance: "+xhr.responseText);
        }
    });
};

DebugHelper.prototype.addServiceBinding = function(serviceInstanceId){
    var that = this;
    var data = {
        service_id : this.serviceDefinitionId,
        plan_id : this.planId,
        organization_guid : $("#createServiceBindingOrgGuid").val(),
        space_guid : $("#createServiceBindingSpaceGuid").val(),
        app_guid : $("#createServiceBindingAppGuid").val(),
        parameters : {}
    };
    $.ajax({
        url : this.pathServiceInstance+"/"+serviceInstanceId+"/service_bindings/" + $("#createServiceBindingId").val(),
        type : 'PUT',
        contentType  : 'application/json; charset=UTF-8',
        data : JSON.stringify(data),
        success : function () {
            displaySuccess("Service binding created");
            that.listServiceBindings(serviceInstanceId);
        },
        error : function(xhr){
            displayDanger("Error adding service binding: "+xhr.responseText);
        }
    });
};

DebugHelper.prototype.listServiceBindings = function(serviceInstanceId){
    var that = this;
    $.ajax({
        url : that.pathApiByServicePfx + serviceInstanceId + that.pathApiListBindingSfx ,
        success : function (serverResponse) {
            var container = $("#allServiceBindings");
            var row;
            container.empty();
            if(serverResponse.body.length > 0){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-5 h5").html("Instance Id"));
                row.append($("<div>").addClass("col-xs-5 h5").html("App Guid"));
                row.append($("<div>").addClass("col-xs-2"));
                container.append(row);
            }
            $.each(serverResponse.body, function(idx, serviceBinding){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-5").html(serviceBinding.id));
                row.append($("<div>").addClass("col-xs-5").html(serviceBinding.appGuid));
                var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                row.append($("<div>").addClass("col-xs-2").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteServiceBinding(serviceInstanceId, serviceBinding.id);
                });
                container.append(row);
            });
        },
        error : function(xhr){
            displayDanger("Error listing service bindings: "+xhr.responseText);
        }
    });
};

DebugHelper.prototype.deleteServiceBinding = function(instanceId, bindingId){
    var that = this;
    $.ajax({
        url : this.pathServiceInstance+"/"+instanceId +"/service_bindings/" + bindingId
        + "?service_id="+this.serviceDefinitionId+"&plan_id="+this.planId,
        type : 'DELETE',
        success : function () {
            displaySuccess("Service binding deleted");
            that.listServiceBindings(instanceId);
        },
        error : function(xhr){
            displayDanger("Error deleting service binding: "+xhr.responseText);
        }
    });
};


