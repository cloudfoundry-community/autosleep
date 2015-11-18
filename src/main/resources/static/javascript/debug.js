

function DebugHelper  (pathServiceInstance, serviceDefinitionId, planId) {
    this.pathDebugListInstances = "/admin/debug/services/instances/";
    this.pathDebugListBindings = "/admin/debug/services/bindings/";
    this.pathDebugListApplications = "/admin/debug/services/applications/";
    this.pathDebugPageServiceBindingsPfx = "/admin/debug/";
    this.pathDebugPageServiceBindingsSfx = "/bindings/";
    this.pathServiceInstance = pathServiceInstance;
    this.serviceDefinitionId = serviceDefinitionId;
    this.planId = planId;
    console.log("DebugHelper - "+serviceDefinitionId+" - "+planId);
}

DebugHelper.prototype.listApplications = function (){
    var that = this;
    $.ajax({
        url : this.pathDebugListApplications,
        success : function (applications) {
            var container = $("#allApplications");
            var row ;
            container.empty();

            if(applications.length > 0){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 h5").html("Guid"));
                row.append($("<div>").addClass("col-xs-2 h5").html("Name"));
                row.append($("<div>").addClass("col-xs-1 h5").html("Status"));
                row.append($("<div>").addClass("col-xs-3 h5").html("Next check"));
                row.append($("<div>").addClass("col-xs-1 h5").html("State"));
                row.append($("<div>").addClass("col-xs-1"));
                container.append(row);
            }
            $.each(applications, function(idx, application){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4").html(application.uuid));
                row.append($("<div>").addClass("col-xs-2").html(application.name));
                row.append($("<div>").addClass("col-xs-1").html(application.appState));

                if(application.nextCheck != null){
                    var nextCheck = new Date(application.nextCheck);
                    var day = nextCheck.getDate ();
                    var month = nextCheck.getMonth() + 1;
                    row.append($("<div>").addClass("col-xs-3").html(month+"/"+day+"/"+nextCheck.getFullYear()+" "+nextCheck.getHours()+":"+nextCheck.getMinutes()+":"+nextCheck.getSeconds()));
                }else
                    row.append($("<div>").addClass("col-xs-3").html("unknown"));
                if (application.stateMachine != null){
                    row.append($("<div>").addClass("col-xs-1").html(application.stateMachine.state));
                }else
                    row.append($("<div>").addClass("col-xs-1").html("unknown"));
                var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                row.append($("<div>").addClass("col-xs-1").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteApplication(application.uuid);
                });

                container.append(row);
            });
        },
        error : function(xhr){
            displayDanger("Error listing applications: "+xhr.responseText);
        }
    });
};


DebugHelper.prototype.deleteApplication = function (applicationId) {
    var that = this;
    $.ajax({
        url : this.pathDebugListApplications+applicationId,
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
        success : function (serviceInstances) {
            var container = $("#allServiceInstances");
            var row;
            container.empty();

            if(serviceInstances.length > 0){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 h5").html("Instance Id"));
                row.append($("<div>").addClass("col-xs-1 h5").html("Definition Id"));
                row.append($("<div>").addClass("col-xs-4 h5").html("Plan Id"));
                row.append($("<div>").addClass("col-xs-1 h5").html("Interval"));
                row.append($("<div>").addClass("col-xs-1 h5").html("Exclude"));
                row.append($("<div>").addClass("col-xs-1"));
                container.append(row);
            }
            $.each(serviceInstances, function(idx, serviceInstance){
                var link = $("<a>", {href : that.pathDebugPageServiceBindingsPfx+serviceInstance.service_instance_id
                +that.pathDebugPageServiceBindingsSfx}).html(serviceInstance.service_instance_id);
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4").append(link));
                row.append($("<div>").addClass("col-xs-1").html(serviceInstance.service_id));
                row.append($("<div>").addClass("col-xs-4").html(serviceInstance.plan_id));
                row.append($("<div>").addClass("col-xs-1").html(serviceInstance.interval));
                row.append($("<div>").addClass("col-xs-1").html(serviceInstance.exclude_names));
                var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                row.append($("<div>").addClass("col-xs-1").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteServiceInstance(serviceInstance.service_instance_id);
                });
                container.append(row);
            });
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
        url : this.pathDebugListBindings + serviceInstanceId ,
        success : function (serviceBindings) {
            var container = $("#allServiceBindings");
            var row;
            container.empty();
            if(serviceBindings.length > 0){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-5 h5").html("Instance Id"));
                row.append($("<div>").addClass("col-xs-5 h5").html("App Guid"));
                row.append($("<div>").addClass("col-xs-2"));
                container.append(row);
            }
            $.each(serviceBindings, function(idx, serviceBinding){
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


