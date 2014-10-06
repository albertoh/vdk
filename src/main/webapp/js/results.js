

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
        a.attr('href', 'javascript:vdk.offers.addToActive("' + code + '", "' + id + '", "' + ex + '")');
        a.text('offer');
        span.append(a);
        return span;
    },
    actionAddDemand: function (code, id, ex) {
        var span = $('<span/>', {class: 'demandexadd', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-cart" >');
        a.attr('title', 'přidat do poptávky');
        a.attr('href', 'javascript:vdk.demands.add("' + code + '", "' + id + '", "' + ex + '")');
        a.text('demand');
        span.append(a);
        return span;
    },
    actionRemoveDemand: function (code, id, ex) {
        var span = $('<span/>', {class: 'demandexrem', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-cancel" >');
        a.attr('title', 'odstranit z poptávky');
        a.attr('href', 'javascript:vdk.demands.remove("' + code + '", "' + id + '", "' + ex + '")');
        a.text('demand');
        span.append(a);
        return span;
    },
    addAkce: function (checks, id, zdroj, code, exemplar) {
        if (zdrojUser[vdk.user] === zdroj) {
            checks.append(vdk.results.actionOffer(code, id, exemplar));
        }
        checks.append(vdk.results.actionAddDemand(code, id, exemplar));
        checks.append(vdk.results.actionRemoveDemand(code, id, exemplar));
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
        if (json1 === null)
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
                    //vdk.results.exMD5($(this), jq(res), 'nabidka', code);
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
};
