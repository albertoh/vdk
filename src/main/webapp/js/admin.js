function VDK_ADMIN() {
    this.eventsHandler = new ApplicationEvents();
    this._init();
}

VDK_ADMIN.prototype = {
    _init: function(){
        this.getRoles();
        this.getUsers();
        this.getJobs();
    },
    getJobs: function(){
        var opts = {
                action: "GETJOBS"
            };
    
        $.getJSON("sched", opts, _.bind(function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                this.jobs = data;
                $.each(this.jobs.jobs, _.bind(function (i, val) {
                    var li = $('<li/>', {class: 'link', "data-jobName": val.jobName, "data-groupName": val.groupName});
                    li.text(val.jobName + ": " + val.fireTime);
                    li.data("jobName", val.jobName);
                    li.data("groupName", val.groupName);
                    li.click(_.bind(function(){
                        this.stopJob(val.jobName, val.groupName);
                    }, this));
                    $("#jobs").append(li);

                }, this));
            }

        }, this));
    },
    stopJob: function(jobName, groupName){
        var opts = {
                action: "STOPJOB", name: jobName, group: groupName
            };
        $.getJSON("sched", opts, function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                alert(vdk.translate(data.message));
            }

        });
    },
    getRoles: function(){
        var opts = {
                action: "GETROLES"
            };
    
        $.getJSON("db", opts, function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                this.roles = data;
                
            }

        });
    },
    getUsers: function(){
        var opts = {
                action: "GETUSERS"
            };
    
        $.getJSON("db", opts, _.bind(function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                this.users = data;
                $.each(this.users, _.bind(function (key, val) {
                    var li = $('<li/>', {class: 'link', "data-code": key});
                    li.text(val.name);
                    li.data("code", key);
                    li.click(_.bind(function(){
                        
                        this.selectUser(key);
                    }, this));
                    $("#users").append(li);

                }, this));
            }

        }, this));
    },
    selectUser: function(code){
        $("#users li").removeClass("selected");
        $("#users li[data-code~='"+code+"']").addClass("selected");
        this.selectedUser = this.users[code];
        $("#user .nazev").val(this.selectedUser.name);
        $("#user .priorita").val(this.selectedUser.priorita);
        $("#user .telefon").val(this.selectedUser.telefon);
        $("#user .email").val(this.selectedUser.email);
        $("#user ul.roles").empty();
        for(var i=0; i<this.selectedUser.roles.length; i++){
            $("#user ul.roles").append('<li>' + this.selectedUser.roles[i] + '</li>');
        }
        
    },
    addUser: function () {
        new Form().open([{name: "name", label: "Nazev"}, {name: "code", label: "Kod"}], function(data){
            var opts = {
                action: "ADDUSER", 
                code: data.code,
                name: data.name
            };
            $.getJSON("db", opts, function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    alert(data.message);
                }

            });
        });
    },
    saveUser: function () {
        new Confirm().open(vdk.translate("user.comfirm.save") + " <b>" + this.selectedUser.name + "</b>?", _.bind(function () {
            var opts = {
                action: "SAVEUSER", 
                code: this.selectedUser.code,
                name: $("#i_nazev").val(),
                priorita: $("#i_priorita").val(),
                email: $("#i_email").val(),
                telefon: $("#i_telefon").val(),
                
            };
            $.getJSON("db", opts, function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    alert(data.message);
                }

            });

        }, this));
    },
    deleteUser: function () {
        
        new Confirm().open(vdk.translate("user.comfirm.delete") + " <b>" + this.selectedUser.name + "</b>?", _.bind(function () {
            $.getJSON("db", {action: "DELETEUSER", code: this.selectedUser.code}, function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    alert(data.message);
                }

            });

        }, this));
    },
    addRole: function (code) {

    }
};