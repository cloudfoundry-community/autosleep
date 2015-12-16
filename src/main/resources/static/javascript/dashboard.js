function DashboardHelper(){
    this.pathApiByServicePfx = "/api/services/";
    this.pathApiListApplicationSfx = "/applications/";
}

DashboardHelper.prototype.listApplications = function(serviceInstanceId){
    var targetUrl =  this.pathApiByServicePfx+serviceInstanceId +this.pathApiListApplicationSfx;
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
                row.append($("<div>").addClass("col-xs-2 h5 text-center").html("State"));
                container.append(row);
            }
            $.each(serverResponse.body, function(idx, application){
                row = $("<row>").addClass("row");
                row.append($("<div>").addClass("col-xs-4 text-center").html(application.uuid));
                row.append($("<div>").addClass("col-xs-2 text-center").html(application.name));
                if (application.watched) {
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
                    .attr("title",application.watched?"WATCHED":"IGNORED")
                    .addClass("col-xs-2 text-center glyphicon");

                if (!application.watched) {
                    stateElement.addClass("glyphicon-eye-close");
                } else {
                    stateElement.addClass("glyphicon-eye-open");
                }

                row.append(stateElement);
                container.append(row);
            });

            $('[data-countdown]').each(function() {
                var $this = $(this), finalDate = $(this).data('countdown');
                $this.countdown(finalDate)
                    .on('update.countdown', function(event) {
                        $this.html(event.strftime('%D days %H:%M:%S'));
                    }).on('finish.countdown', function() {
                        that.listApplications(serviceInstanceId);
                    });
            });

            $('[data-toggle="tooltip"]').tooltip();
        },
        error : function(xhr){
            displayDanger("Error listing applications: "+xhr.responseText);
        }
    });
};