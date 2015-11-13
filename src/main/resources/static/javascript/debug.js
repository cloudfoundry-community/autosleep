

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
    $.ajax({
        url : this.pathDebugListApplications,
        success : function (applications) {
            var container = $("#allApplications");
            container.empty();
            if(applications.length > 0){
                container.append($("<div>").addClass("col-xs-3").html("Guid"));
                container.append($("<div>").addClass("col-xs-3").html("Name"));
                container.append($("<div>").addClass("col-xs-3").html("State"));
                container.append($("<div>").addClass("col-xs-3").html("Next check"));
            }
            $.each(applications, function(idx, application){
                container.append($("<div>").addClass("col-xs-3").append(application.uuid));
                container.append($("<div>").addClass("col-xs-3").append(application.name));
                container.append($("<div>").addClass("col-xs-3").append(application.state));
                container.append($("<div>").addClass("col-xs-3").append(application.nextCheck));
            });
        },
        error : function(xhr){
            displayDanger("Error listing applications: "+xhr.responseText);
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
        parameters : {inactivity : $("#createServiceInstancInactivity").val()}

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
            container.empty();
            if(serviceInstances.length > 0){
                container.append($("<div>").addClass("col-xs-4").html("Instance Id"));
                container.append($("<div>").addClass("col-xs-2").html("Definition Id"));
                container.append($("<div>").addClass("col-xs-3").html("Plan Id"));
                container.append($("<div>").addClass("col-xs-1"));
            }
            $.each(serviceInstances, function(idx, serviceInstance){
                var link = $("<a>", {href : that.pathDebugPageServiceBindingsPfx+serviceInstance.service_instance_id
                +that.pathDebugPageServiceBindingsSfx}).html(serviceInstance.service_instance_id);
                container.append($("<div>").addClass("col-xs-4").append(link));
                container.append($("<div>").addClass("col-xs-2").html(serviceInstance.service_id));
                container.append($("<div>").addClass("col-xs-3").html(serviceInstance.plan_id));
                var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                container.append($("<div>").addClass("col-xs-1").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteServiceInstance(serviceInstance.service_instance_id);
                });
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
            container.empty();
            if(serviceBindings.length > 0){
                container.append($("<div>").addClass("col-xs-5").html("Instance Id"));
                container.append($("<div>").addClass("col-xs-5").html("App Guid"));
                container.append($("<div>").addClass("col-xs-2"));
            }
            $.each(serviceBindings, function(idx, serviceBinding){
                container.append($("<div>").addClass("col-xs-5").html(serviceBinding.id));
                container.append($("<div>").addClass("col-xs-5").html(serviceBinding.appGuid));
                var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                container.append($("<div>").addClass("col-xs-1").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteServiceBinding(serviceInstanceId, serviceBinding.id);
                });
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


