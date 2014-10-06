
function Demand() {
    this.loaded = false;
    this.activeid = -1;
    this.retreive();
}

Demand.prototype = {
    open: function () {
        if (!this.loaded) {
            this.dialog = $("<div/>", {title: dict['select.demand']});
            $("body").append(this.dialog);
            this.dialog.load("demands.vm", _.bind(function () {
                this.getUserDemands();
            }, this));
            this.loaded = true;
        }
        this.dialog.dialog({modal: true, width: 680, height: 500});
    },
    retreive: function () {
        $("#demands li.demand").remove();
        this.demands = {};
        $.getJSON("db?action=GETDEMANDS", _.bind(function (json) {
            this.demands = json;
            $.each(json, function (key, val) {
                var label = val.nazev + ' (' + val.knihovna + ' )';
                if (val.closed) {
                    //$("#demands").append('<li class="demand" data-demand="' + val.id + '"><a href="javascript:filterDemand(' + val.id + ');">' + label + '</a></li>');
                }
            });
            $(".demand>div").each(function () {
                var demandid = $(this).data("id");
                if (json.hasOwnProperty(demandid)) {
                    var val = json[demandid];
                    var text = '<label>' + val.nazev + " (" + val.knihovna + ")</label>";
                    $(this).html(text);
                }
            });
            this.getUserDemands();
        }, this));
    },
    getUserDemands: function () {
        $("#activeDemands>option").remove();
        $("#userdemands li.demand").remove();
        $.each(this.demands, _.bind(function (key, val) {
            if (val.knihovna === vdk.user) {
                this.renderUserDemand(val);
            }
        }, this));

    },
    renderUserDemand: function (val) {
        var label = val.nazev;
        $("#activeDemands").append('<option value="' + val.id + '">' + label + '</option>');
        var li = $("<li/>");
        li.addClass("demand");
        li.data("demand", val.id);
        var span1 = $('<span class="ui-icon" style="float:left;" />');
        if (val.expired) {
            span1.addClass("ui-icon-clock");
        } else {
            span1.addClass("ui-icon-check");
        }
        li.append(span1);
        var span = $('<span class="closed ui-icon" style="float:left;" />');
        if (val.closed) {
            span.addClass("ui-icon-locked");
            span.attr("title", "poptavka je zavrena");
        } else {
            span.addClass("ui-icon-unlocked");
            span.attr("title", "zavrit poptavku");
            span.click(_.bind(function (e) {
                this.close(val.id);
            }, this));
        }
        li.append(span);
        var a = $("<a/>");
        a.text(label);
        a.attr("href", "javascript:vdk.demands.clickUser('" + val.id + "');");
        li.append(a);
        $("#userdemands>ul").append(li);
    },
    getActive: function () {

        $.getJSON("db?action=GETOFFER&id=" + this.activeid, _.bind(function (json) {
            console.log(json);
            this.active = json;
            $.each(json, function (key, val) {


            });
        }, this));

    },
    setActive: function (id) {
        this.activeid = id;
        $("#userdemands li.demand").removeClass("active");
        $("#userdemands li.demand").each(function () {
            if (id === $(this).data("demand")) {
                $(this).addClass("active");
                return;
            }
        });
        $("#importDemandForm input[name~='idDemand']").val(this.activeid);
        $("#addToDemandForm input[name~='idDemand']").val(this.activeid);
        this.getActive();
        $("#userdemand").show();
    },
    clickUser: function (id) {
        this.setActive(id);
        $('#activeDemands').val(id);
    },
    selectActive: function () {
        var id = $('#activeDemands').val();
        this.setActive(id);
    },
    add: function (code, id, ex) {
        var comment = prompt("Poznamka", "");
        if(comment===null) return;
        $.getJSON("db", {action: "ADDTODEMAND", docCode: code, zaznam: id, ex: ex, comment: comment}, function (data) {
            if (data.error) {
                alert("error ocurred: " + data.error);
                return;
            }
            $.getJSON("index", {action: "ADDDEMAND", docCode: code, zaznam: id, ex: ex}, _.bind(function (resp) {
                if (resp.error) {
                    alert("error ocurred: " + resp.error);
                } else {
                    alert("Poptavka uspesne indexovana");
                }
            }, this));
        });

    },
    remove: function (code, id, ex) {
        $.getJSON("db", {action: "REMOVEDEMAND", docCode: code, zaznam: id, ex: ex}, function (data) {
            if (data.error) {
                alert("error ocurred: " + data.error);
                return;
            }
            $.getJSON("index", {action: "REMOVEDEMAND", docCode: code, zaznam: id, ex: ex}, _.bind(function (resp) {
                if (resp.error) {
                    alert("error ocurred: " + resp.error);
                } else {
                    alert("Poptavka uspesne odstranena");
                }
            }, this));
        });

    },
    close: function (id) {
        $.post("db", {action: "CLOSEDEMAND", id: id}, _.bind(function (resp) {
            if (resp.trim() === "1") {
                
                //indexujeme
                $.getJSON("index", {action: "INDEXDEMAND", id: id}, _.bind(function (resp) {
                    if (resp.error) {
                        alert("error ocurred: " + resp.error);
                    } else {
                        
                        this.demands[id].closed = true;
                        $("#userdemands li.demand").each(function () {
                            if ($(this).data("demand") === id) {
                                $(this).find("span.closed").removeClass("ui-icon-unlocked").addClass("ui-icon-locked");
                            }
                        });

                        alert("Poptavka uspesne indexovana");
                    }
                }, this));
            }

        }, this));
    }
};

