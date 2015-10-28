

function ServicesHelper  (pathServiceInstance, serviceDefinitionId, planId) {
    this.pathDebugListServiceInstances = "/admin/debug/services/servicesinstances/";
    this.pathDebugListServiceBindings = "/admin/debug/services/servicebindings/";
    this.pathDebugPageServiceBindings = "/admin/debug/services_bindings//";
    this.pathServiceInstance = pathServiceInstance;
    this.serviceDefinitionId = serviceDefinitionId;
    this.planId = planId;
    console.log("ServicesHelper - "+ pathServiceInstance + " - "+serviceDefinitionId+" - "+planId);
}

ServicesHelper.prototype.addServiceInstance = function(){
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
            console.log(JSON.stringify(data));
            console.log(typeof data);
            that.listServiceInstances();
        },
        error : function(error){
            console.error("error:"+error);
        }
    })
};

ServicesHelper.prototype.listServiceInstances = function(){
    var that = this;
    $.ajax({
        url : this.pathDebugListServiceInstances,
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
                var link = $("<a>", {href : that.pathDebugPageServiceBindings+serviceInstance.instanceId}).html(serviceInstance.instanceId);
                container.append($("<div>").addClass("col-xs-4").append(link));
                container.append($("<div>").addClass("col-xs-2").html(serviceInstance.definitionId));
                container.append($("<div>").addClass("col-xs-3").html(serviceInstance.planId));
                var button = $("<button>", {type : "button"}).addClass("btn btn-circle")
                    .append($("<i>").addClass("glyphicon glyphicon-remove"));
                container.append($("<div>").addClass("col-xs-1").append(button));
                button.on("click", function(e){
                    e.preventDefault();
                    that.deleteServiceInstance(serviceInstance.instanceId);
                });
            });
        },
        error : function(error){
            console.error("error:"+error);
        }
    })
};

ServicesHelper.prototype.addServiceInstance = function(){
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
            console.log(JSON.stringify(data));
            console.log(typeof data);
            that.listServiceInstances();
        },
        error : function(error){
            console.error("error:"+error);
        }
    })
};

ServicesHelper.prototype.deleteServiceInstance = function(serviceInstanceId){
    var that = this;
    $.ajax({
        url : this.pathServiceInstance+"/"+serviceInstanceId
                + "?service_id="+this.serviceDefinitionId+"&plan_id="+this.planId,
        type : 'DELETE',
        success : function (data) {
            that.listServiceInstances();
        },
        error : function(error){
            console.error("error:"+error);
        }
    });
};

ServicesHelper.prototype.addServiceBinding = function(serviceInstanceId){
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
        success : function (data) {
            console.log(JSON.stringify(data));
            console.log(typeof data);
            that.listServiceBindings(serviceInstanceId);
        },
        error : function(error){
            console.error("error:"+error);
        }
    })
};

ServicesHelper.prototype.listServiceBindings = function(serviceInstanceId){
    var that = this;
    $.ajax({
        url : this.pathDebugListServiceBindings + serviceInstanceId ,
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
        error : function(error){
            console.error("error:"+error);
        }
    })
};

ServicesHelper.prototype.deleteServiceBinding = function(instanceId, bindingId){
    var that = this;
    $.ajax({
        url : this.pathServiceInstance+"/"+instanceId +"/service_bindings/" + bindingId
        + "?service_id="+this.serviceDefinitionId+"&plan_id="+this.planId,
        type : 'DELETE',
        success : function (data) {
            that.listServiceBindings(instanceId);
        },
        error : function(error){
            console.error("error:"+error);
        }
    });
};