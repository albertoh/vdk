

function VDK() {
    this.nabidka = new Nabidka();
    this.export = new Export();
    this.demands = new Demand();
    this.views = new Views();
    this.init = function () {
        this.user = null;
        this.activeofferid = -1;
        this.getViews();
        this.getOffers();
        $(document).tooltip({
            items: "div.diff, [title]",
            content: function () {

                var element = $(this);
                if (element.is("div.diff")) {
                    return $(this).children(" div.titles").html();
                }
                if (element.is("[title]")) {
                    return element.attr("title");
                }
            }
        });

    }
    this.userOpts = function () {
        if (zdrojUser[vdk.user]) {
            //Prihlaseny uzivatel je NKP, MZK nebo VKOL
            $(".offerdoc").hide();
            $(".offerex").show();
            $("demanddoc").hide();
        } else {
            $(".offerdoc").show();
            $(".offerex").hide();
            $("demanddoc").show();
        }
        ;
        //vdk.getUserOffers();
        $("li.res").each(function () {
            var id = $(this).find("input.groupid").val();
        });
    };
    this.selectView = function () {
        this.views.select();
    };
    this.getViews = function () {
        this.views.get();
    };
    this.saveView = function () {
        this.views.save();
    }
    this.openView = function () {
        this.views.open();
    }
    this.addToNabidka = function (id) {
        this.nabidka.add(id);
    };
    this.openNabidka = function () {
        this.nabidka.open();
    };
    this.openExport = function () {
        this.export.open();
    };
    this.showCSV = function (elem) {
        if (!this.csv) {
            this.csvdialog = $('<div title="CSV format" class="csv1" ></div>');
            this.csv = $('<input style="width:100%;" type="text" value=""/>');
            this.csvdialog.append(this.csv);
            $("body").prepend(this.csvdialog);
            $(this.csv).focus(function () {
                $(this).select();
            });
        }
        this.csv.val($(elem).data("csv"));
        this.csvdialog.dialog({modal: true, width: 700});
    };

    this.getOffers = function () {
        $("#offers li.nabidka").remove();
        this.offers = {};
        $.getJSON("db?action=GETOFFERS", function (json) {
            vdk.offers = json;
            $.each(json, function (key, val) {
                var label = val.nazev + ' (' + val.knihovna + ' do ' + val.expires + ')';
                var expired = val.expired ? " expired" : "";
                if (val.closed) {
                    $("#offers").append('<li class="nabidka' + expired + '" data-offer="' + val.offerId + '"><a href="javascript:filterOffer(' + val.offerId + ');">' + label + '</a></li>');
                }
            });
            $(".nabidka>span").each(function () {
                var offerid = $(this).data("offer");
                if (json.hasOwnProperty(offerid)) {
                    var val = json[offerid];
                    var expired = val.expired ? " expired" : "";
                    var text = '<label class="' + expired + '">' + val.nazev + " (" + val.knihovna + ")</label>";
                    $(this).html(text);

                    var zaznam = $(this).data("zaznam");
                    if (zaznam === "none") {
                        //je to nabidka zvenku, nemame zaznam.
                    } else {
                        $("tr[data-zaznam~='" + zaznam + "']");
                    }
                    var ex = $(this).data("ex");
                    if (ex === "none") {
                        //je to nabidka na cely zaznam.
                    } else {
                        $(this).mouseenter(function () {
                            $("tr[data-md5~='" + ex + "']").addClass("nabidka");
                        });
                        $(this).mouseleave(function () {
                            $("tr[data-md5~='" + ex + "']").removeClass("nabidka");
                        });

                    }
                }
            });
            vdk.getUserOffers();
        });
    };
    this.getUserOffers = function () {
        $("#activeOffers>option").remove();
        $("#useroffers li.nabidka").remove();
        $.each(vdk.offers, function (key, val) {
            if (val.knihovna === vdk.user) {
                vdk.renderUserOffer(val);
            }
        });

    };
    this.renderUserOffer = function (val) {
        var label = val.nazev + ' (do ' + val.expires + ')';
        $("#activeOffers").append('<option value="' + val.offerId + '">' + label + '</option>');
        var expired = val.expired ? " expired" : "";
        var li = $("<li/>");
        li.addClass("nabidka");
        li.data("offer", val.offerId);
        var span1 = $('<span class="ui-icon" style="float:left;" />');
        if (val.expired) {
            span1.addClass("ui-icon-clock");
            span1.attr("title", "expired");
        } else {
            span1.addClass("ui-icon-check");
        }
        li.append(span1);
        var span = $('<span class="closed ui-icon" style="float:left;" />');
        if (val.closed) {
            span.addClass("ui-icon-locked");
            span.attr("title", "nabidka je zavrena");
        } else {
            span.addClass("ui-icon-unlocked");
            span.attr("title", "zavrit nabidku");
            span.click(function () {
                $.post("db", {action: "CLOSEOFFER", id: val.offerId}, function (resp) {
                    if (resp.trim() === "1") {
                        vdk.offers[val.offerId].closed = true;
                        $("#useroffers li.nabidka").each(function () {
                            if ($(this).data("offer") === val.offerId) {
                                $(this).find("span.closed").removeClass("ui-icon-unlocked").addClass("ui-icon-locked");
                            }
                        });
                        //indexujeme
                        $.getJSON("index", {action: "INDEXOFFER", id: val.offerId}, function (resp) {
                            if (resp.error) {
                                alert("error ocurred: " + resp.error);
                            } else {
                                alert("Nabidka uspesne indexovana");
                            }
                        });
                    }

                });
            });
        }
        li.append(span);
        var a = $("<a/>");
        a.text(label);
        a.attr("href", "javascript:vdk.clickUserOffer('" + val.offerId + "');");
        li.append(a);
        $("#useroffers>ul").append(li);
    };
    this.getActiveOffer = function () {

        $.getJSON("db?action=GETOFFER&id=" + vdk.activeofferid, function (json) {
            console.log(json);
            vdk.activeoffer = json;
            $.each(json, function (key, val) {


            });
        });

    };
    this.setActiveOffer = function (offerid) {
        this.activeofferid = offerid;
        $("#useroffers li.nabidka").removeClass("active");
        $("#useroffers li.nabidka").each(function () {
            if (offerid === $(this).data("offer")) {
                $(this).addClass("active");
                return;
            }
        });
        $("#importOfferForm input[name~='idOffer']").val(vdk.activeofferid);
        $("#addToOfferForm input[name~='idOffer']").val(vdk.activeofferid);
        this.getActiveOffer();
        $("#useroffer").show();
    };
    this.clickUserOffer = function (offerid) {
        this.setActiveOffer(offerid);
        $('#activeOffers').val(offerid);
    };
    this.selectActiveOffer = function () {
        var offerid = $('#activeOffers').val();
        this.setActiveOffer(offerid);
    };
    this.addOffer = function () {
        var nazev = prompt("Nazev nabidky", "");
        if (nazev !== null && nazev !== "") {
            $.post("db", {offerName: nazev, action: 'NEWOFFER'}, function (data) {
                var json = jQuery.parseJSON(data);
                vdk.offers[json.offerId] = json;
                vdk.renderUserOffer(json);

                vdk.setActiveOffer(json.offerId);
                $('#activeOffers').val(json.offerId);
            });
        }
    };
    this.addFormToOffer = function () {
        if (this.activeofferid === -1) {
            alert("Neni zadna nabidka activni");
            return;
        }
        $.post("db", $("#addToOfferForm").serialize(), function (data) {
            alert(data);
        });
    };
    this.addToOffer = function (code, id, ex) {
        if (this.activeofferid === -1) {
            alert("Neni zadna nabidka activni");
            return;
        }
        var comment = prompt("Poznamka", "");
        $.post("db", {action: "ADDTOOFFER", idOffer: vdk.activeofferid, docCode: code, id: id, ex: ex, comment: comment}, function (data) {
            alert(data);
        });
    };
}
;

function LoginDialog() {

}
LoginDialog.prototype = {
    _init: function () {


    }
};

function Views() {

}
Views.prototype = {
    _init: function () {


    },
    get: function () {
        var url = "db?action=LOADVIEWS";
        $.getJSON(url, function (data) {
            $.each(data.views, function (i, item) {
                $("#saved_views").append('<option value="' + item.query + '">' + item.nazev + '</option>');
            });
        });
    },
    select: function () {
        var query = $("#saved_views").val();
        window.location.href = "?" + query;
    },
    open: function () {

        if (!this.loaded) {
            this.dialog = $("<div/>", {title: dict['select.view']});
            $("body").append(this.dialog);
            this.dialog.load("forms/view.vm");
            this.loaded = true;
        }
        this.dialog.dialog({modal: true, width: 400, height: 300});
    },
    save: function () {
        if ($("#viewName").val() === "") {
            return;
        }
        var url = "db?action=SAVEVIEW&" + $('#viewForm').serialize() + "&" + $('#searchForm').serialize();
        $.get(url, function (data) {
            alert(data);
        });
    }
};

function Demand() {
    this.loaded = false;
    this.activeid = -1;
    this.retreive();
}

Demand.prototype = {
    add: function () {

        var nazev = prompt("Nazev poptavky", "");
        if (nazev !== null && nazev !== "") {
            $.post("db", {name: nazev, action: 'NEWDEMAND'}, _.bind(function (data) {
                var json = jQuery.parseJSON(data);
                this.demands[json.id] = json;
                this.renderUserDemand(json);

                this.setActiveDemand(json.id);
                $('#activeDemands').val(json.id);
            }, this));
        }
    },
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
                    $("#demands").append('<li class="demand" data-demand="' + val.id + '"><a href="javascript:filterDemand(' + val.id + ');">' + label + '</a></li>');
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
    addTo: function (code, id, ex) {
        if (this.activeid === -1) {
            alert("Neni zadna poptavka activni");
            return;
        }
        var comment = prompt("Poznamka", "");
        $.post("db", {action: "ADDTODEMAND", id: this.activeid, docCode: code, zaznam: id, ex: ex, comment: comment}, function (data) {
            alert(data);
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


function Nabidka() {
    this.loaded = false;
}

Nabidka.prototype = {
    open: function () {
        if (!this.loaded) {
            this.dialog = $("<div/>", {title: dict['select.offer']});
            $("body").append(this.dialog);
            this.dialog.load("nabidka.vm", function () {
                vdk.getUserOffers();
            });
            this.loaded = true;
        }
        this.dialog.dialog({modal: true, width: 680, height: 500});
        //vdk.getUserOffers();
    },
    add: function (code) {
        alert(code);
    },
    getAll: function () {
        $.getJSON("db?action=GETOFFERS", function () {

        })
    }
};

function Results() {
}

Results.prototype = {
    init: function () {

        $("#facets").accordion({
            heightStyle: "content"
        });
        this.getGrouped();
        this.checkDifferences();
        this.doRange("#rokvydani_range", "#rokvydani_select", "rokvydani", true);
        this.doRange("#pocet_range", "#pocet_select", "pocet_exemplaru", false);
        this.zdrojNavButtons();
    },
    zdrojNavButtons: function () {
        $('#facets input.chci').button({
            icons: {
                primary: "ui-icon-check"
            },
            text: false
        });
        $('#facets input.nechci').button({
            icons: {
                primary: "ui-icon-cancel"
            },
            text: false
        }).change(function () {
            saveWanted(id, identifier);
        });
    },
    doRange: function (id, sel, field, withMin) {
        var minv = $(id).data("min");
        var maxv = $(id).data("max");
        var min = 0;
        if (withMin) {
            min = minv;
        }
        $(id).slider({
            range: true,
            min: min,
            max: maxv,
            values: [minv, maxv],
            slide: function (event, ui) {
                $(sel + ">span.label").html("od " + ui.values[ 0 ] + " - do " + ui.values[ 1 ]);
                $(sel).data("from", ui.values[ 0 ]);
                $(sel).data("to", ui.values[ 1 ]);
            }
        });
        $(sel + ">span.go").button({
            icons: {
                primary: "ui-icon-arrowthick-1-e"
            },
            text: false
        });
        $(sel + ">span.go").click(function () {
            addRange(field, $(sel).data("from"), $(sel).data("to"));
        });
    },
    checkDifferences: function () {
        $("li.res").each(function () {
            var res = $(this).attr('id');
            if ($(jq(res) + " input.numDocs").val() > 1) {
                var eq = true;
                for (var i = 0; i < $(jq(res) + " span.title").length - 1; i++) {
                    eq = ($(jq(res) + " span.title").eq(i).html() === $(jq(res) + " span.title").eq(i + 1).html()) && eq;
                }
                if (!eq) {
                    $(jq(res) + " div.diff").show();
                }
            }
        });
    },
    addRow: function (exs, json, j, field, code) {
        var row = $('<tr class="' + field + ' ' + json.zdroj + '" data-md5="' + j.md5 + '">');
        var icon = zdrojIcon(json.zdroj, j.isNKF);
        row.append('<td><img width="16" src="' + icon + '" title="' + json.zdroj + '"/>' +
                '<a style="float:right;" class="ui-icon ui-icon-extlink" target="_view" href="original?id=' + json.id + '&path=' + json.file + '">view</a></td>');
        row.append("<td>" + jsonElement(j, "signatura") + "</td>");
        row.append("<td class=\"" + jsonElement(j, "status") + "\">" + jsonElement(j, "status", "status") + "</td>");
        row.append("<td>" + jsonElement(j, "dilchiKnih") + "</td>");
        row.append("<td>" + jsonElement(j, "svazek") + "</td>");
        row.append("<td>" + jsonElement(j, "cislo") + "</td>");
        row.append("<td>" + jsonElement(j, "rok") + "</td>");
        var checks = $('<td class="dofondu" style="width:95px;"></td>');
        if (vdk.user !== null) {
            this.addAkce(checks, json.id, json.zdroj, code, j.md5);
        } else {
            checks.append('<span style="margin-left:60px;">&nbsp;</span>');
        }
        row.data("md5", j.md5);
        row.append(checks);

        exs.append(row);
        if (exs.children().length > 3) {
            $(row).addClass("more");
        }
        return row;
    },
    actionOffer: function (code, id, ex) {
        var span = $('<span/>', {class: 'offerex', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-flag" >');
        a.attr('title', 'přidat do nabídky');
        a.attr('href', 'javascript:vdk.addToOffer("' + code + '", "' + id + '", "' + ex + '")');
        a.text('offer');
        span.append(a);
        return span;
    },
    actionDemand: function (code, id, ex) {
        var span = $('<span/>', {class: 'demandex', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-cart" >');
        a.attr('title', 'přidat do poptávky');
        a.attr('href', 'javascript:vdk.demands.addTo("' + code + '", "' + id + '", "' + ex + '")');
        a.text('offer');
        span.append(a);
        return span;
    },
    addAkce: function (checks, id, zdroj, code, exemplar) {
        if (zdrojUser[vdk.user] === zdroj) {
            checks.append(vdk.results.actionOffer(code, id, exemplar));
        } else {
            checks.append(vdk.results.actionDemand(code, id, exemplar));
        }
    },
    addActions: function (checks, id, zdroj, code, exemplar) {

        if (zdrojUser[vdk.user] !== zdroj) {
            var id1 = id + exemplar + "_chci";
            var check1 = $('<input class="chci" type="radio" name="' + id + exemplar + '" id="' + id1 + '" />');
            var label1 = $('<label for="' + id1 + '">${chci}</label>');
            $(checks).append(check1);
            $(checks).append(label1);
            $(check1).change(function () {
                saveWanted(id, code, exemplar);
            });
            $(check1).button({
                icons: {
                    primary: "ui-icon-check"
                },
                text: false
            });

            var id2 = id + exemplar + "_nechci";
            var check2 = $('<input class="nechci" type="radio" name="' + id + exemplar + '" id="' + id2 + '" />');
            var label2 = $('<label for="' + id2 + '">${nechci}</label>');
            $(checks).append(check2);
            $(checks).append(label2);
            $(check2).button({
                icons: {
                    primary: "ui-icon-cancel"
                },
                text: false
            }).change(function () {
                saveWanted(id, code, exemplar);
            });
        }

        if (zdrojUser[vdk.user] === zdroj) {
            var id3 = id + exemplar + "_nabidnu";
            var check3 = $('<input class="nabidnu" type="checkbox" name="' + id3 + '" id="' + id3 + '" />');
            var label3 = $('<label for="' + id3 + '">${nabidnu}</label>');
            $(checks).append(check3);
            $(checks).append(label3);
            $(check3).button({
                icons: {
                    primary: "ui-icon-pin-s"
                },
                text: false
            }).change(function () {
                saveWeOffer(id, code, exemplar);
            });
        }

        getWanted(id);
        getWeOffer(id, exemplar);
    },
    exMD5: function (obj, dest, field, code) {

        var jsonAll = $(obj).data();
        //var json = jsonAll.ex;
        var json1 = jQuery.parseJSON(jsonAll[field]);
        if (json1 == null)
            return;

        for (var e = 0; e < json1.exemplare.length; e++) {
            var json = json1.exemplare[e];
            if (json !== null && json.ex.length > 0) {

                var exs = $(dest + " table.tex>tbody");
                for (var i = 0; i < json.ex.length; i++) {
                    var j = json.ex[i];
                    var sig = jsonElement(j, "signatura");
                    if (sig.indexOf("SF") !== 0) {
                        var row = vdk.results.addRow(exs, json, j, field, code);
                    }
                }
            } else {
                //$(dest).append("<div>"+$(obj).data()+"</div>");
            }
        }
    },
    parseExemplare: function (obj, dest) {
        var jsonAll = $(obj).data();
        //var json = jsonAll.ex;
        var json1 = jQuery.parseJSON(jsonAll.ex);

        for (var e = 0; e < json1.exemplare.length; e++) {
            var json = json1.exemplare[e];
            if (json !== null && json.ex.length > 0) {

                var exs = $(dest + " table.tex>tbody");
                for (var i = 0; i < json.ex.length; i++) {
                    var j = json.ex[i];
                    this.addRow(exs, jsonAll.icon, j);
                }
            } else {
                //$(dest).append("<div>"+$(obj).data()+"</div>");
            }
        }
        //$(".fg-toolbar", dest + " table.tex").hide();
    },
    getGrouped: function () {
        $(".res").each(function () {
            if ($(this).hasClass("collapsed")) {
                var res = $(this).attr('id');
                $(jq(res) + " .ex").remove();
            } else {
                var res = $(this).attr('id');
                var code = $(jq(res) + ">input.code").val();
                $(jq(res) + " .ex").each(function () {
                    vdk.results.exMD5($(this), jq(res), 'ex', code);
                    vdk.results.exMD5($(this), jq(res), 'nabidka', code);
                });
                if ($(jq(res) + " tr.more").length > 0) {
                    $(jq(res) + " table.tex>thead>tr>th.actions>span").show();
                    $(jq(res) + " table.tex>thead>tr>th.actions>span").click(function (e) {
                        $(jq(res) + " table.tex>tbody>tr.more").toggle();
                    });
                } else {
                    $(jq(res) + " table.tex>thead>tr>th.actions>span").hide();
                }

            }
        });
        $(".collapsed").each(function () {
            var id = $(this).attr('id');
            var code = $(jq(id) + ">input.code").val();
            var url = "inc/collapsed.jsp?q=" + code;
            $.get(url, function (data) {
                $(jq(id)).append(data);
                $(jq(id) + " .u_res").each(function () {
                    var res = $(this).attr('id');
                    vdk.results.exMD5($(jq(res) + " .ex"), jq(res), 'ex', code);
                    vdk.results.exMD5($(jq(res) + " .ex"), jq(res), 'nabidka', code);
                    //doTable(jq(res));
                });
            });
        });
    }
}




function Export() {
    this.loaded = false;
}

Export.prototype = {
    open: function () {
        var url = "csv/export.vm" + window.location.search + "&rows=10000";
        window.open(url, "export");
//        if(!this.loaded){
//            this.dialog = $("<div/>", {id: 'export', title: 'export'});
//            $("body").append(this.dialog);
//            this.loaded = true;
//        }
//        var area = $("<textarea/>");
//        var text = "";
//        $('.csv').each(function(){
//            text = text + $(this).data("csv") + "\n";
//        });
//        area.text(text);
//        this.dialog.html(area);
//        this.dialog.dialog({width: '90%', height: '400'});
    }
};



var vdk = new VDK();