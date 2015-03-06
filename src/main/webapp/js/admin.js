function VDK_ADMIN() {
    this.eventsHandler = new ApplicationEvents();
    this._init();
}

VDK_ADMIN.prototype = {
    _init: function(){
        $('#admin>div.tabs').tabs();
        this.getRoles();
        this.getUsers();
        this.getJobs();
        this.getSources();
    },
    getJobs: function(){
        var opts = {
                action: "GETJOBS"
            };
    
        $.getJSON("sched", opts, _.bind(function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                $("#jobs").empty();
                this.jobs = data;
                $.each(this.jobs, _.bind(function (i, val) {
                    var li = $('<li/>', {class: 'link', "data-jobKey": val.jobKey});
                    //var running = this.jobs.running.hasOwnProperty("val.jobName")
                    li.text(val.name + " (" + val.state + "): " + val.nextFireTime);
                    li.data("jobKey", val.jobKey);
                    li.addClass(val.state);
                    if(val.state === "waiting"){
                        var bt = $('<button/>');
                        bt.text("start now");
                        bt.click(_.bind(function(){
                            this.startJob(val.jobKey);
                        }, this));
                        li.append(bt);
                    }
                    
                    if(val.state === "running"){
                        var bt2 = $('<button/>');
                        bt2.text("stop");
                        bt2.click(_.bind(function(){
                            this.stopJob(val.jobKey);
                        }, this));
                        li.append(bt2);
                    }
                    
                    $("#jobs").append(li);

                }, this));
            }

        }, this));
    },
    startJob: function(jobKey){
        var opts = {
                action: "STARTJOB", key: jobKey
            };
        $.getJSON("sched", opts, _.bind(function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                alert(vdk.translate(data.message));
                this.getJobs();
                
            }

        }, this));
    },
    stopJob: function(jobKey){
        var opts = {
                action: "STOPJOB", key: jobKey
            };
        $.getJSON("sched", opts, _.bind(function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                alert(vdk.translate(data.message));
                this.getJobs();
            }

        }, this));
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
    getSources: function(){
        var opts = {
                action: "GETSOURCES"
            };
    
        $.getJSON("db", opts, _.bind(function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                this.sources = data;
                $.each(this.sources, _.bind(function (key, val) {
                    var tr = $('<tr/>', {"data-code": key});
                    tr.append('<td>' + val.name + '</td>');
                    tr.append('<td>' + val.conf + '</td>');
                    
                    tr.append('<td><input class="cron" value="' + val.cron + '" /></td>');
                    var td = $('<td/>');
                    var bt = $('<button/>');
                        bt.text("save");
                        bt.click(_.bind(function(){
                            this.saveSource(key, val.conf);
                        }, this));
                        td.append(bt);
                    tr.append(td);
                    
                    tr.data("code", key);
                    
                    $("#sources").append(tr);

                }, this));
            }

        }, this));
    },
    saveSource: function(name, conf){
        var cron = $("#sources tr[data-code~='"+name+"']>td>input.cron").val();
        new Confirm().open(vdk.translate("user.comfirm.save") + " <b>" + name + "</b>?", _.bind(function () {
            var opts = {
                action: "SAVESOURCE", 
                name: name,
                cron: cron,
                conf: conf
                
            };
            $.getJSON("db", opts, _.bind(function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    this.getJobs();
                    alert(data.message);
                }

            }, this));

        }, this));
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
                telefon: $("#i_telefon").val()
                
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