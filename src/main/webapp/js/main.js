
function jq(myid) {
    return '#' + myid.replace(/(:|\.|\/)/g, '\\$1');
}

function doSearch() {
//    var q = $("#q").val();
//    if(q != ""){
//        window.location = window.location.protocol +
//            window.location.pathname + "?q=" + q;
//    }
    return true;
}

function toggleAdv() {
    $('#avdFields').toggle();
}

function sortBy(sortby, sortdirection) {
    $("#offset").val("0");
    $("#sortby").val(sortby);
    $("#sortdirection").val(sortdirection);
    document.getElementById("searchForm").submit();
}

function collapseBy(field) {
    $("#offset").val("0");
    $("#collapseBy").val(field);
    document.getElementById("searchForm").submit();

}

function gotoOffset(offset) {
    $("#offset").val(offset);
    document.getElementById("searchForm").submit();
}

function filterOffers() {
    var i = $("input.fq").length + 1;
    var input = '<input type="hidden" name="onlyOffers" id="onlyOffers" class="fq" value="yes" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function filterOffer(id) {
    var i = $("input.fq").length + 1;
    var input = '<input type="hidden" name="offer" id="offer' + id + '" class="fq" value="' + id + '" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function filterDemands() {
    var input = '<input type="hidden" name="onlyDemands" id="onlyDemands" class="fq" value="yes" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function filterDemand(id) {
    var input = '<input type="hidden" name="demand" id="demand' + id + '" class="fq" value="' + id + '" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function removeOffer(id) {
    $('#offer' + id).remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function removeDemand(id) {
    $('#demand' + id).remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function addNav(nav) {
    var i = $("input.fq").length + 1;
    var input = '<input type="hidden" name="fq" id="fq' + i + '" class="fq" value="' + nav.replace(/\"/g, "&quot;") + '" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function addRange(field, from, to) {
    var i = $("input.fq").length + 1;
    var input = '<input type="hidden" name="fq" id="fq' + i + '" class="fq" value="' + field + ":[" + from + " TO " + to + ']" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function excludeNav(nav) {
    var i = $("input.exs").length + 1;
    var input = '<input type="hidden" name="exs" id="exs' + i + '" class="exs" value="' + nav.replace(/\"/g, "&quot;") + '" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function zdrojNav(val, obj) {
    if ($(obj).is(":checked")) {
        var i = $("input.zdroj").length + 1;
        var input = '<input type="hidden" name="zdroj" id="zdroj' + i + '" class="zdroj" value="' + val.replace(/\"/g, "&quot;") + '" />';
        $("#searchForm").append(input);
        $("#offset").val("0");
    } else {
        $('input.zdroj[value="' + val + '"]').remove();
    }
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function removeZdroj(i) {
    $('#zdroj' + i).remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}


function toggleNav(nav) {
    $("#nav_" + nav + '>li.more').toggle();
}

function previous() {
    var o = document.getElementById("offset").value;
    var h = document.getElementById("hits").value;
    document.getElementById("offset").value = o - h;
    document.getElementById("searchForm").submit();
}

function next() {
    var o = parseInt(document.getElementById("offset").value);
    var h = parseInt(document.getElementById("hits").value);
    document.getElementById("offset").value = o + h;
    document.getElementById("searchForm").submit();
}

function removeNav(i) {
    $('#fq' + i).remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function removeExs(i) {
    $('#ex' + i).remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function removeQuery() {
    $('#q').val('');
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function addWanted(wants) {

    var input = '<input name="wanted" id="wanted" type="hidden" value="' + wants + '" />';
    $("#searchForm").append(input);
    $("#offset").val("0");
    document.getElementById("searchForm").submit();
}

function removeWanted() {
    $('#wanted').remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();
}

function removeOnlyOffers() {
    $('#onlyOffers').remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();

}

function removeOnlyDemands() {
    $('#onlyDemands').remove();
    document.getElementById("offset").value = 0;
    document.getElementById("searchForm").submit();

}


function removeAdvField(field) {
    $("#" + field).val("");
    document.getElementById("searchForm").submit();
}



function getWanted(id, exemplar) {
    var url = "db?action=GETWANTED&id=" + id + "&ex=" + exemplar;
    var jqid = jq(id) + exemplar;
    $.get(url, function (data) {
        if (data == "0")
            return;
        if (data == "true") {
            $(jqid + "_chci").attr("checked", "checked");
            $(jqid + "_chci").button('refresh');
        } else {
            $(jqid + "_nechci").attr("checked", "checked");
            $(jqid + "_nechci").button('refresh');
        }
        $(jqid + "_chci").addClass("haswant");
    });
}


function saveWanted(id, code, exemplar) {
    var jqid = jq(id) + exemplar;
    var wants = $(jqid + "_chci").is(":checked");
    var action = "SAVEWANTED";
    if ($(jqid + "_chci").hasClass("haswant")) {
        action = "UPDATEWANTED";
    }
    var url = "db?action=" + action + "&id=" + id + "&code=" + code + "&wanted=" + wants + "&ex=" + exemplar;
    $.get(url, function (data) {
        alert(data);
    });
}

function getWeOffer(id, exemplar) {
    var url = "db?action=GETWEOFFER&id=" + id + "&ex=" + exemplar;
    var jqid = jq(id) + exemplar;
    $.get(url, function (data) {
        if (data == "0")
            return;
        if (data == "true") {
            $(jqid + "_nabidnu").attr("checked", "checked");
            $(jqid + "_nabidnu").button('refresh');
        }
        $("#" + id + "_nabidnu").addClass("haswant");
    });
}

function saveWeOffer(id, code, exemplar) {
    var jqid = jq(id) + exemplar;
    var offered = $(jqid + "_nabidnu").is(":checked");
    var action = "SAVEWEOFFER";
    if (!offered) {
        action = "DELETEWEOFFER";
    }
    var url = "db?action=" + action + "&id=" + id + "&code=" + code + "&offered=" + offered + "&ex=" + exemplar;
    $.get(url, function (data) {
        alert(data);
    });
}

//function getOffers() {
//
//
//    var url = "db?action=LOADOFFERS";
//
//    $.getJSON(url, function(data) {
//        $.each(data.views, function(i, item) {
//            $("#nabidky").append('<option value="' + item.id + '">' + item.nazev + '</option>');
//        });
//    });
//}

function selectOffer() {
    var id = $("#nabidky").val();
    var url = "db?action=DOWNLOADOFFER&id=" + id;

    window.open(url);

}

function importOfferShow() {
    $("#importOffer").dialog(
            {position: {my: "right top", at: "right bottom", of: "#offerBox"}}
    );
}

function importOfferDo() {

}



function autocompleteQ() {
    $("#q").autocomplete({
        source: function (request, response) {
            $.ajax({
                url: "suggest.jsp",
                dataType: "json",
                data: {
                    maxRows: 12,
                    q: request.term
                },
                success: function (data) {

//            response( $.map( data.response.docs, function( item ) {
//              return {
//                label: item.title,
//                value: item.title
//              }
//            }));
                    var count = 0;
                    response($.each(data.facet_counts.facet_fields.title_suggest, function (item) {
                        if (count % 2 == 0) {
                            var str = item.toString();
                            return {
                                label: str.substring(str.indexOf("##") + 2),
                                value: item
                            }
                        }
                    }));
                }
            });
        },
        minLength: 2,
        delay: 500,
        select: function (event, ui) {
            log(event);
            log(ui.item ?
                    "Selected: " + ui.item.label :
                    "Nothing selected, input was " + this.value);
        },
        open: function () {
            $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
        },
        close: function () {
            $(this).removeClass("ui-corner-top").addClass("ui-corner-all");
        }
    });
}

function jsonElement(json, el, prefix) {
    var val = json[el];
    if (val) {
        if (prefix && dict[prefix + "." + val]) {
            return dict[prefix + "." + val];
        } else {
            return val;
        }
        return val;
    } else {
        return "";
    }
}

function zdrojIcon(zdroj, isNKF) {
    if (zdroj.indexOf("MZK") !== -1) {
        return "img/icons/zdroj/mzk.gif";
    } else if (zdroj.indexOf("VKOL") !== -1) {
        return "img/icons/zdroj/vkol.gif";
    } else if (zdroj.indexOf("NKF") !== -1) {
        return "img/icons/zdroj/nkf.gif";
    } else if (zdroj.indexOf("UKF") !== -1) {
        if (isNKF) {
            return "img/icons/zdroj/nkf.gif";
        } else {
            return "img/icons/zdroj/ukf.gif";
        }
    } else {
        return "img/icons/zdroj/" + zdroj + ".gif";
    }
}

function Confirm() {

}

Confirm.prototype = {
    open: function (title, f) {
        if ($("#dialog-confirm").length === 0) {
            this.create(title);
        } else {
            $("#dialog-confirm>div").html(title);
        }
        $("#dialog-confirm").dialog({
            resizable: false,
            modal: true,
            title: "Confirm",
            buttons: {
                "OK": function () {
                    if (f)
                        f.apply(null, null);
                    $(this).dialog("close");
                },
                Cancel: function () {
                    $(this).dialog("close");
                }
            }
        });
    },
    create: function (title) {
        $('body').append($('<div id="dialog-confirm"><div>' + title + '</span></div>'));
    }
};


function addButtons(iconButtons, obj) {

    $.each(iconButtons, function (i, v) {

        var $button = $("<button/>").text(this.text);
        $button.button({
            icons: {primary: this.icon},
            text: false
        }).addClass("ui-dialog-titlebar-close")
                .css("float", "right")
                .click(this.click);
        $(obj).append($button);
    });
}