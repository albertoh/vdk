
vdk.eventsHandler.addHandler(function(type, configuration) {
    if (type === "demands") {
        vdk.results.init();
        vdk.offers.init();
    }
});

function Results() {
}

Results.prototype = {
    init: function () {
        $("#facets").accordion({
            heightStyle: "content"
        });
        this.checkDifferences();
        this.zdrojNavButtons();
        this.doRange("#rokvydani_range", "#rokvydani_select", "rokvydani", true);
        this.doRange("#pocet_range", "#pocet_select", "pocet_exemplaru", false);
        this.parseDocs();
        this.renderOfferTitleDoc();
    },
    renderOfferTitleDoc: function(){
        $(".nabidka_ext").each(function () {
            var json = $(this).data('nabidka_ext');
            $.each(json, _.bind(function (key, val) {
                if (vdk.isLogged && val.knihovna === vdk.user.code) {
                    this.renderUserOffer(val);
                }
                if(val.fields['245a']){
                    $(this).text(val.fields['245a']);
                }
            }, this));
            
        });
    },
    parseDocs: function(){
        $(".res").each(function () {
            
            var res = $(this).attr('id');
            var code = $(jq(res) + ">input.code").val();
            var zaznam = $(jq(res) + ">input.identifier").val();
            $(jq(res) + " .ex").each(function () {
                vdk.results.parseDocExemplars($(this), code);
            });
            var $actions = $(this).find('.docactions');
            $actions.append(vdk.results.actionOriginal(zaznam));
            if(vdk.isLogged){
                $actions.append(vdk.results.actionOffer(code));
                $actions.append(vdk.results.actionAddDemand(code));
            }
            $actions.append(vdk.results.actionCSV($(this).data("csv")));
            
        });
    },
    parseDocExemplars: function(div, code){
        var json = jQuery.parseJSON($(div).data("ex")).exemplare;
        var table = $(div).find("table.tex");
        if(json.length === 0){
            table.hide();
            return;
        }
        
        for(var i = 0; i<json.length; i++){
            var jsonex = json[i].ex;
            for(var j=0; j<jsonex.length; j++){
                $(table).append(this.renderDocExemplar(jsonex[j], json[i].id, json[i].zdroj, code));
            }
        }
            
        if ($(table).find("tr.more").length > 0) {
            var span =  $(table).find("thead>tr>th.actions>span");
            span.show();
            span.click(function (e) {
                $(table).find("tr.more").toggle();
            });
        } else {
           $(table).find("thead>tr>th.actions").css("float","none");
           $(table).find("thead>tr>th.actions").text(" ");
        }
            
    },
    renderDocExemplar: function(json, zaznam, zdroj, code){
        var row = $('<tr class="" data-md5="' + json.md5 + '">');
        row.data("md5", json.md5);
        
        var icon = zdrojIcon(zdroj, json.isNKF);
        var filePath = "";
        if(json.hasOwnProperty("file")){
            '&path=' + json.file;
        }
        row.append('<td><img width="16" src="' + icon + '" title="' + zdroj + '"/>' +
                '<a style="float:right;" class="ui-icon ui-icon-extlink" target="_view" href="original?id=' + zaznam + filePath + '">view</a></td>');
        row.append("<td>" + jsonElement(json, "signatura") + "</td>");
        row.append("<td class=\"" + jsonElement(json, "status") + "\">" + jsonElement(json, "status", "status") + "</td>");
        row.append("<td>" + jsonElement(json, "dilchiKnih") + "</td>");
        row.append("<td>" + jsonElement(json, "svazek") + "</td>");
        row.append("<td>" + jsonElement(json, "cislo") + "</td>");
        row.append("<td>" + jsonElement(json, "rok") + "</td>");
        var checks = $('<td class="actions" style="width:95px;"></td>');
        if (vdk.isLogged !== null) {
            this.addAkce(row, checks, zaznam, zdroj, code, json.md5);
        } else {
            checks.append('<span style="margin-left:60px;">&nbsp;</span>');
        }
        row.append(checks);

//        exs.append(row);
//        if (exs.children().length > 3) {
//            $(row).addClass("more");
//        }
        return row;
    },
    actionOriginal: function (id) {
        var span = $('<button/>', {class: 'original', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-extlink" >');
        a.attr('title', 'nahlédnout originální metadata');
        a.attr('href', 'javascript:vdk.showOriginal("' + id + '")');
        a.text('original');
        span.append(a);
        return span;
    },
    actionCSV: function (csv) {
        var span = $('<button/>', {class: 'original', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-document" >');
        a.attr('title', 'csv format');
        //a.attr('href', 'javascript:vdk.showCSV("'+csv+'")');
        a.attr('href', 'javascript:void(0);');
        a.click(function(){vdk.showCSV(csv)});
        a.text('csv');
        span.append(a);
        return span;
    },
    actionOffer: function (code, id, ex) {
        var span = $('<button/>', {class: 'offerex', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-flag" >');
        a.attr('title', 'přidat do nabídky');
        a.attr('href', 'javascript:vdk.offers.addToActive("' + code + '", "' + id + '", "' + ex + '")');
        a.text('offer');
        span.append(a);
        return span;
    },
    actionAddDemand: function (code, id, ex) {
        var span = $('<button/>', {class: 'demandexadd', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-cart" >');
        a.attr('title', 'přidat do poptávky');
        a.attr('href', 'javascript:vdk.demands.add("' + code + '", "' + id + '", "' + ex + '")');
        a.text('demand');
        span.append(a);
        return span;
    },
    actionWant: function (zaznamOffer) {
        var span = $('<button/>', {class: 'wanteddoc', 'data-wanted': zaznamOffer, style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-star" >');
        a.attr('title', dict['offer.want'] + ' "' + dict['chci.do.fondu'] + '"');
        a.attr('href', 'javascript:vdk.offers.wantDoc(' + zaznamOffer + ', true)');
        a.text('chci');
        span.append(a);
        return span;
    },
    actionDontWant: function (zaznamOffer) {
        var span = $('<button/>', {class: 'wanteddoc', 'data-wanted': zaznamOffer, style: 'float:left;'});
        
        var a = $('<a class="ui-icon ui-icon-cancel" >');
        a.attr('title', dict['offer.want'] + ' "' + dict['nechci.do.fondu'] + '"');
        a.attr('href', 'javascript:vdk.offers.wantDoc(' + zaznamOffer + ', false)');
        a.text('chci');
        span.append(a);
        return span;
    },
    actionRemoveDemand: function (code, id, ex) {
        var span = $('<button/>', {class: 'demandexrem', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-cancel" >');
        a.attr('title', 'odstranit z poptávky');
        a.attr('href', 'javascript:vdk.demands.remove("' + code + '", "' + id + '", "' + ex + '")');
        a.text('demand');
        span.append(a);
        return span;
    },
    addAkce: function (row, checks, zaznam, zdroj, code, exemplar) {
        if (vdk.isLogged && vdk.zdrojUser[vdk.user.code] === zdroj) {
            checks.append(this.actionOffer(code, zaznam, exemplar));
        }
        if(vdk.demands.isUserDemand(code, zaznam, exemplar)){
            checks.append(this.actionRemoveDemand(code, zaznam, exemplar));
        }else{
            checks.append(this.actionAddDemand(code, zaznam, exemplar));
        }
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
            //saveWanted(id, identifier);
        });
    },
    checkDifferences: function () {
        //Vypnuto kvuli pridani NKF do zdroje
        //Nutno zpracovat primo z db
        return;
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
}
function Results2() {
}
Results2.prototype = {
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
            //saveWanted(id, identifier);
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
        if (vdk.isLogged !== null) {
            this.addAkce(row, checks, json.id, json.zdroj, code, j.md5);
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
    actionWant: function (code, id, ex) {
        var span = $('<span/>', {class: 'wantexadd', style: 'float:left;'});
        var a = $('<a class="ui-icon ui-icon-check" >');
        a.attr('title', 'reagovat na nabídku');
        a.attr('href', 'javascript:vdk.offer.wantDoc("' + code + '", "' + id + '", "' + ex + '")');
        a.text('chci');
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
    addAkce: function (row, checks, id, zdroj, code, exemplar) {
        if (vdk.zdrojUser[vdk.user.code] === zdroj) {
            checks.append(this.actionOffer(code, id, exemplar));
        }
        checks.append(vdk.results.actionAddDemand(code, id, exemplar));
        if(row.hasClass('isdemand')){
            checks.append(this.actionRemoveDemand(code, id, exemplar));
        }
        if(row.hasClass('isoffer')){
            checks.append(this.actionWant(code, id, exemplar));
        }
    },
    addActions: function (checks, id, zdroj, code, exemplar) {

        if (vdk.isLogged && vdk.isLogged && vdk.zdrojUser[vdk.user.code] !== zdroj) {
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

        if (vdk.isLogged && vdk.zdrojUser[vdk.user.code] === zdroj) {
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

        if(json1.exemplare.length===0){
             $(dest + " table.tex").hide();
        }else{
            for (var e = 0; e < json1.exemplare.length; e++) {
                var json = json1.exemplare[e];
                if (json !== null && json.ex.length > 0) {
                    var exs = $(dest + " table.tex>tbody");
                    for (var i = 0; i < json.ex.length; i++) {
                        var j = json.ex[i];
                        var sig = jsonElement(j, "signatura");
                        if (sig.indexOf("SF") !== 0) {
                            vdk.results.addRow(exs, json, j, field, code);
                        }
                    }
                } else {
                    $(dest + " table.tex").hide();
                }
            }
        }
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
//        $(".collapsed").each(function () {
//            var id = $(this).attr('id');
//            var code = $(jq(id) + ">input.code").val();
//            var url = "inc/collapsed.jsp?q=" + code;
//            $.get(url, function (data) {
//                $(jq(id)).append(data);
//                $(jq(id) + " .u_res").each(function () {
//                    var res = $(this).attr('id');
//                    vdk.results.exMD5($(jq(res) + " .ex"), jq(res), 'ex', code);
//                });
//            });
//        });
        
        $(".nabidka_ext").each(function () {
            var json = $(this).data('nabidka_ext');
            $.each(json, _.bind(function (key, val) {
                if (val.knihovna === vdk.user.code) {
                    this.renderUserOffer(val);
                }
                if(val.fields['245a']){
                    $(this).text(val.fields['245a']);
                }
            }, this));
            
        });
    }
};
