
function Offers() {
    this.loaded = false;
}

Offers.prototype = {
    init: function(){
        this.importDialog = null;
        this.retrieve();
    },
    openForm: function () {
        this.formDialog = $("<div/>", {title: dict['select.offerForm']});
        $("body").append(this.formDialog);
        this.formDialog.load("forms/add_to_offer.vm");
        this.formDialog.dialog({
            modal: true,
            width: 580,
            height: 500
        });
    },
    openImportDialog: function () {
        if(this.importDialog === null){
            this.importDialog = $("<div/>", {title: dict['select.offerImport']});
            $("body").append(this.importDialog);
            this.importDialog.load("forms/import_offer.vm", _.bind(function () {
                $("#importOfferFormId").val(this.activeid);
            }, this));
        }else{
            $("#importOfferFormId").val(this.activeid);
        }
        
        this.importDialog.dialog({
            modal: true,
            width: 580,
            height: 500
        });
    },
    open: function () {
        if (!this.loaded) {
            this.dialog = $("<div/>", {title: dict['select.offer']});
            $("body").append(this.dialog);
            this.dialog.load("offers.vm", _.bind(function () {
                this.parseUser();
                var bs = [
                    {
                        text: "Přídat do nabídky z katalogu",
                        icon: "ui-icon-search",
                        click: function (e) {
                            alert("hledat");
                        }
                    },
                    {
                        text: "Přídat do nabídky ze sablony",
                        icon: "ui-icon-contact",
                        click: function (e) {
                            vdk.offers.openForm();
                        }
                    },
                    {
                        text: "Přídat do nabídky ze souboru",
                        icon: "ui-icon-folder-open",
                        click: function (e) {
                            vdk.offers.openImportDialog();
                        }
                    },
                    {
                        text: "View report",
                        icon: "ui-icon-note",
                        click: function (e) {
                            window.open("reports/offer.vm?id=" + vdk.offers.activeid, "report");
                        }
                    }
                ];
                addButtons(bs, "#useroffer>div.buttons");
            }, this));
            this.loaded = true;
        }
        this.dialog.dialog({
            modal: true,
            width: 750,
            height: 600,
            iconButtons: [
                {
                    text: "Refresh",
                    icon: "ui-icon-refresh",
                    click: function (e) {
                        vdk.offers.getActive();
                    }
                },
                {
                    text: "Přídat nabídku",
                    icon: "ui-icon-plusthick",
                    click: function (e) {
                        vdk.offers.add();
                    }
                }
            ],
            create: function () {
                var $titlebar = $(this).parent().find(".ui-dialog-titlebar");
                $.each($(this).dialog('option', 'iconButtons'), function (i, v) {

                    var $button = $("<button/>").text(this.text),
                            right = $titlebar.find("[role='button']:last")
                            .css("right");

                    $button.button({
                        icons: {primary: this.icon},
                        text: false
                    }).addClass("ui-dialog-titlebar-close")
                            .css("right", (parseInt(right) + 22) + "px")
                            .click(this.click);

                    $titlebar.append($button);

                });
            }
        });
    },
    add: function () {
        var nazev = prompt("Nazev nabidky", "");
        if (nazev !== null && nazev !== "") {
            $.getJSON("db", {offerName: nazev, action: 'NEWOFFER'}, _.bind(function (data) {
                this.json[data.offerId] = data;
                this.renderUserOffer(data);

                this.setActiveOffer(data.offerId);
                $('#activeOffers').val(data.offerId);
            }, this));
        }
    },
    isWanted: function(zaznamoffer, knihovna){
        for(var i=0; i<this.wanted.length; i++){
            if(this.wanted[i].zaznamoffer === zaznamoffer
                    && this.wanted[i].knihovna === knihovna){
                return this.wanted[i].wanted;
            }
        }
        return null;
    },
    retrieve: function(){
        this.retrieveWanted();
    },
    retrieveWanted: function(){
        this.wanted = [];
        $.getJSON("db?action=GETWANTED", _.bind(function (json) {
            if(json.error){
                alert(vdk.translate(json.error));
            }else{
                this.wanted = json;
                this.retrieveOffers();
            }
        }, this));
    },
    retrieveOffers: function () {
        this.activeid = -1;
        $("#offers li.offer").remove();
        this.json = {};
        $.getJSON("db?action=GETOFFERS", _.bind(function (json) {
            this.json = json;
            $("#nav_nabidka li.offer").each(function () {
                var id = $(this).data("offer");
                var label = json[id].nazev + ' (' + json[id].knihovna + ')';
                $(this).find("a").text(label);
            });

            $(".offer>div").each(function () {
                var id = $(this).data("id");
                if (json.hasOwnProperty(id)) {
                    var val = json[id];
                    var text = '<label>' + val.nazev + " (" + val.knihovna + ")</label>";
                    $(this).html(text);
                }
            });
            this.parseUser();            
            
            $(".nabidka>div").each(function () {
                var offerId = $(this).data("offer");
                if (json.hasOwnProperty(offerId)) {
                    var val = json[offerId];
                    var expired = val.expired ? " expired" : "";
                    var label = $('<label class="' + expired + '">');
                    var text = val.knihovna + ' in offer ' + val.nazev;
                    var zaznamOffer = $(this).data("offer_ext")[offerId].zaznamOffer;

                    if ($(this).data("zaznam")) {
                        var zaznam = $(this).data("zaznam");
                        $("tr[data-zaznam~='" + zaznam + "']");
                    } else {
                        //je to nabidka zvenku, nemame zaznam.
                        
                    }
                    // pridame cenu jestli ma
                    if($(this).data("offer_ext")[offerId].fields.hasOwnProperty('cena'))
                        text += ' (' + $(this).data("offer_ext")[offerId].fields.cena + ')';
                    label.text(text);
                    $(this).append(label);
                    
                    
                    if(vdk.isLogged && vdk.user.code !== val.knihovna){
                        var wanted = vdk.offers.isWanted(zaznamOffer, vdk.user.code);
                        if(wanted == null){
                            $(this).append(vdk.results.actionWant(zaznamOffer));
                            $(this).append(vdk.results.actionDontWant(zaznamOffer));
                            $(this).attr('title', dict['offer.want.unknown']);
                        }else if(wanted){
                            $(this).addClass('wanted');
                            $(this).append(vdk.results.actionDontWant(zaznamOffer));
                            $(this).attr('title', dict['chci.do.fondu']);
                        }else{
                            $(this).append(vdk.results.actionWant(zaznamOffer));
                            $(this).addClass('nowanted');
                            $(this).attr('title', dict['nechci.do.fondu']);
                        }
                    }
                    
                    if (!$(this).data("offer_ext")[offerId].hasOwnProperty('ex')) {
                        //je to nabidka na cely zaznam.
                    } else {
                        var ex = $(this).data("offer_ext")[offerId].ex;
                        var tr = $("tr[data-md5~='" + ex + "']");
                        tr.addClass("nabidka");
                        tr.find(".offerex, .demandexadd").remove();
                        if(vdk.isLogged && vdk.user.code !== val.knihovna){
                            tr.find("td.actions").append(vdk.results.actionWant(zaznamOffer));
                            tr.find("td.actions").append(vdk.results.actionDontWant(zaznamOffer));
                        }
                        $(this).mouseenter(function () {
                            tr.addClass("nabidka_over");
                        });
                        $(this).mouseleave(function () {
                            tr.removeClass("nabidka_over");
                        });

                    }
                }
            });

        }, this));
    },
    parseUser: function () {
        $("#activeOffers>option").remove();
        $("#useroffers li.offer").remove();
        $.each(this.json, _.bind(function (key, val) {
            if (vdk.isLogged && val.knihovna === vdk.user.code) {
                this.renderUserOffer(val);
            }
        }, this));

        if (this.activeid === -1) {
            if ($("#useroffers li.offer").first()) {
                var offerid = $("#useroffers li.offer").first().data("offer");
                if (offerid !== null) {
                    this.clickUser(offerid);
                }
            }
        }

    },
    renderUserOffer: function (val) {
        var label = val.nazev;
        $("#activeOffers").append('<option value="' + val.id + '">' + label + '</option>');
        var li = $("<li/>", {"data-offer": val.id});
        li.addClass("offer");
        li.data("offer", val.id);
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
            span.attr("title", "nabidka je zavrena");
        } else {
            span.addClass("ui-icon-unlocked");
            span.attr("title", "zavrit nabidku");
            span.click(_.bind(function (e) {
                this.close(val.id);
            }, this));
        }
        li.append(span);
        var a = $("<a/>");
        a.text(label);
        a.attr("href", "javascript:vdk.offers.clickUser('" + val.id + "');");
        li.append(a);
        $("#useroffers>ul").append(li);
    },
    renderDoc: function (val, closed) {
        var doc = $('<li/>', {class: 'offer', 'data-zaznamofferid': val.ZaznamOffer_id});
        doc.data("zaznamofferid", val.ZaznamOffer_id);

        var label = $('<div/>', {class: 'label'});
        if (val.hasOwnProperty('title')) {
            label.html(val.title + " (" + val.fields.comment + ")");
        } else {
            var html = "";
            if(val.hasOwnProperty('fields')){
                if (val.fields.hasOwnProperty('245a')) {
                    html += val.fields['245a'];
                }
                if (val.fields.hasOwnProperty('comment')) {
                    html += val.fields['comment'];
                }
            }
            label.html(html);
        }
        doc.append(label);
        if(!closed){
            var iconButtons = [{
                    text: "Remove",
                    icon: "ui-icon-close",
                    click: function (e) {
                        vdk.offers.removeDoc(val.ZaznamOffer_id);
                    }
                }];
            addButtons(iconButtons, doc);
        }
        if(val.hasOwnProperty('wanted')){
            $.each(val.wanted, function(key, wanted){
                doc.append('<span class="'+ (wanted.wanted ? '':'no') +'wanted">' + wanted.knihovna + "</span>" );
            });
        }
        $('#useroffer>ul').append(doc);
    },
    getActive: function () {
        $('#useroffer>ul>li').remove();
        $.getJSON("db?action=GETOFFER&id=" + this.activeid, _.bind(function (json) {
            this.active = json;
            var closed = this.json[this.activeid].closed;
            $.each(json, _.bind(function (key, val) {
                this.renderDoc(val, closed);

            }, this));
        }, this));

    },
    setActive: function (id) {
        this.activeid = id;
        $("#useroffers li.offer").removeClass("active");
        $("#useroffers li.offer").each(function () {
            if (id === $(this).data("offer")) {
                $(this).addClass("active");
                return;
            }
        });
        $("#importOfferForm input[name~='id']").val(this.activeid);
        $("#addToOfferForm input[name~='id']").val(this.activeid);
        this.getActive();
        $("#useroffer").show();
    },
    clickUser: function (id) {
        this.setActive(id);
        $('#activeOffers').val(id);
    },
    selectActive: function () {
        var id = $('#activeOffers').val();
        this.setActive(id);
    },
    remove: function (code, id, ex) {
        $.getJSON("db", {action: "REMOVEOFFER", docCode: code, zaznam: id, ex: ex}, function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
                return;
            }
            $.getJSON("index", {action: "REMOVEOFFER", docCode: code, zaznam: id, ex: ex}, _.bind(function (resp) {
                if (resp.error) {
                    alert("error ocurred: " + vdk.translate(resp.error));
                } else {
                    alert("Nabidka uspesne odstranena");
                }
            }, this));
        });

    },
    removeDoc: function (ZaznamOffer_id) {
        $.getJSON("db", {action: "REMOVEZAZNAMOFFER", ZaznamOffer_id: ZaznamOffer_id}, function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                $("#useroffer li.offer[data-zaznamofferid~='" + ZaznamOffer_id + "']").remove();
                ;
                alert(data.message);
            }
        });

    },
    close: function (id) {
        if(this.json[id].closed) return;
        new Confirm().open("opravdu chcete zavrit nabidku <b>"+this.json[id].nazev+"</b>?", _.bind(function () {
            $.post("db", {action: "CLOSEOFFER", id: id}, _.bind(function (resp) {
                if (resp.trim() === "1") {

                    //indexujeme
                    $.getJSON("index", {action: "INDEXOFFER", id: id}, _.bind(function (resp) {
                        if (resp.error) {
                            alert("error ocurred: " + vdk.translate(resp.error));
                        } else {

                            this.json[id].closed = true;
                            $("#useroffers li.offer").each(function () {
                                if ($(this).data("offer") === id) {
                                    $(this).find("span.closed").removeClass("ui-icon-unlocked").addClass("ui-icon-locked");
                                }
                            });

                            alert("Nabidka uspesne indexovana");
                        }
                    }, this));
                }

            }, this));
        }, this));

    },
    wantDoc: function (zaznam_offer, wanted) {
        $.getJSON("db", {action: "WANTOFFER", zaznam_offer: zaznam_offer, wanted: wanted}, function (data) {
            if (data.error) {
                alert("error ocurred: " + vdk.translate(data.error));
            } else {
                //indexujeme
                    $.getJSON("index", {action: "INDEXWANTED", id: data.id}, _.bind(function (resp) {
                        if (resp.error) {
                            alert("error ocurred: " + vdk.translate(resp.error));
                        } else {
                            $(".wanteddoc[data-wanted~='" + zaznam_offer + "']").remove();
                            alert("Reakce uspesne zpracovana");
                        }
                    }, this));
                
                
            }

        });

    },
    addToActive: function (code, zaznam, ex) {
        new PriceAndComment().open(function(data){
            var opts = {
                action: "ADDDOCTOOFFER", 
                id: vdk.offers.activeid, 
                docCode: code, 
                comment: data.comment, 
                cena: data.price
            };
            if(zaznam){
                opts.zaznam = zaznam;
            }
            if(ex){
                opts.exemplar = ex;
            }
            $.getJSON("db", opts, function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    alert(data.message);
                }

            });
        });

    },
    addForm: function () {
        if (this.activeid === -1 || this.activeid === null) {
            alert("Neni zadna nabidka activni");
        } else {
            $("#addToOfferForm input[name~='id']").val(this.activeid);
            $.getJSON("db", $("#addToOfferForm").serialize(), function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    vdk.offers.renderDoc(data);
                    alert("Pridano!");
                }
            });
        }
    },
    import: function () {
        if (this.activeid === -1 || this.activeid === null) {
            alert("Neni zadna nabidka activni");
        } else {
            $("#importOfferFormId").val(this.activeid);
            $.getJSON("db", $("#importOfferForm").serialize(), function (data) {
                if (data.error) {
                    alert("error ocurred: " + vdk.translate(data.error));
                } else {
                    vdk.offers.renderDoc(data);
                    alert("Pridano!");
                }
            });
        }
    }
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
                $("#nav_nabidka").append('<li class="nabidka' + expired + '" data-offer="' + val.offerId + '"><a href="javascript:filterOffer(' + val.offerId + ');">' + label + '</a></li>');
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
        if (vdk.isLogged && val.knihovna === vdk.user.code) {
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
                            alert("error ocurred: " + vdk.translate(resp.error));
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

 