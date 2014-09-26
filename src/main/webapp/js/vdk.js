

function VDK() {
    this.nabidka = new Nabidka();
    this.export = new Export();
    this.views = new Views();
    this.init = function(){
        this.user = null;
        this.getViews();
        this.getOffers();
        $(document).tooltip({
            items: "div.diff, [title]", 
            content: function() {

                var element = $( this );
                if ( element.is( "div.diff" ) ) {
                  return $(this).children(" div.titles").html();
                }
                if ( element.is( "[title]" ) ) {
                  return element.attr( "title" );
                }
            }
        });
        
    }
    this.userOpts = function() {
        if(zdrojUser[vdk.user]){
            //Prihlaseny uzivatel je NKP, MZK nebo VKOL
            $(".offerdoc").hide();
            $(".offerex").show();
        };
        //vdk.getUserOffers();
        $("li.res").each(function(){
            var id = $(this).find("input.groupid").val();
        });
    };
    this.selectView = function() {
        this.views.select();
    };
    this.getViews = function() {
        this.views.get();
    };
    this.saveView = function() {
        this.views.save();
    }
    this.openView = function() {
        this.views.open();
    }
    this.addToNabidka = function(id) {
        this.nabidka.add(id);
    };
    this.openNabidka = function() {
        this.nabidka.open();
    };
    this.openExport = function() {
        this.export.open();
    };
    this.showCSV = function(elem) {
        if (!this.csv) {
            this.csvdialog = $('<div title="CSV format" class="csv1" ></div>');
            this.csv = $('<input style="width:100%;" type="text" value=""/>');
            this.csvdialog.append(this.csv);
            $("body").prepend(this.csvdialog);
            $(this.csv).focus(function() {
                $(this).select();
            });
        }
        this.csv.val($(elem).data("csv"));
        this.csvdialog.dialog({modal: true, width: 700});
    };
    this.getOffers = function() {
        $("#offers li.nabidka").remove();
        vdk.offers = {};
        $.getJSON("db?action=GETOFFERS", function(json) {
            vdk.offers = json;
            $.each(json, function(key, val) {
                var label = val.nazev + ' (' + val.knihovna + ' do ' + val.expires + ')';
                var expired = val.expired ? " expired" : "";
                if(val.closed){
                    $("#offers").append('<li class="nabidka' + expired + '" data-offer="' + val.offerId + '"><a href="javascript:filterOffer(' + val.offerId + ');">' + label + '</a></li>');
                }
            });
            $(".nabidka>div").each(function() {
                var offerid = $(this).data("id");
                if (json.hasOwnProperty(offerid)) {
                    var val = json[offerid];
                    var expired = val.expired ? " expired" : "";
                    var text = '<label class="' + expired + '">' + val.nazev + " (" + val.knihovna + ")</label>";
                    $(this).html(text);
                }
            });
            vdk.getUserOffers();
        });
    };
    this.getUserOffers = function() {
        $("#activeOffers>option").remove();
        $("#useroffers li.nabidka").remove();
        $.each(vdk.offers, function(key, val) {
            if(val.knihovna === vdk.user){
                vdk.renderUserOffer(val);
            }
        });
        
    };
    this.renderUserOffer = function(val){
                var label = val.nazev + ' (do ' + val.expires + ')';
        $("#activeOffers").append('<option value="' + val.offerId + '">' + label + '</option>');
                var expired = val.expired ? " expired" : "";
                var li = $("<li/>");
                li.addClass("nabidka");
                li.data("offer", val.offerId);
                var span1 = $('<span class="ui-icon" style="float:left;" />');
                if(val.expired){
                    span1.addClass("ui-icon-clock");
                    span1.attr("title", "expired");
                }else{
                    span1.addClass("ui-icon-check");
                }
                li.append(span1);
                var span = $('<span class="closed ui-icon" style="float:left;" />');
                if(val.closed){
                    span.addClass("ui-icon-locked");
                    span.attr("title", "nabidka je zavrena");
                }else{
                    span.addClass("ui-icon-unlocked");
                    span.attr("title", "zavrit nabidku");
                    span.click(function(){
                        $.post("db", {action: "CLOSEOFFER", id:val.offerId}, function(resp){
                           if(resp.trim()==="1"){
                               vdk.offers[offerId].closed = true;
                               $("#useroffers li.nabidka").each(function(){
                                   if($(this).data("offer")===val.offerId){
                                       $(this).find("span.closed").removeClass("ui-icon-unlocked").addClass("ui-icon-locked");
                                   }
                               });
//                               $("#useroffers li.nabidka[data-offer~='"+val.offerId+"']").
//                                       find("span.closed").removeClass("ui-icon-locked").addClass("ui-icon-locked");
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
    this.getActiveOffer = function() {
        
        $.getJSON("db?action=GETOFFER&id="+vdk.activeofferid, function(json) {
            console.log(json);
            vdk.activeoffer = json;
            $.each(json, function(key, val) {
                
                
            });
        });
        
    };
    this.setActiveOffer = function(offerid) {
        vdk.activeofferid = offerid;
        $("#useroffers li.nabidka").removeClass("active");
        $("#useroffers li.nabidka").each(function() {
            if(offerid === $(this).data("offer")){
                $(this).addClass("active");
                return;
            }
        });
        $("#importOfferForm input[name~='offerid']").val(vdk.activeofferid);
        $("#addToOfferForm input[name~='offerid']").val(vdk.activeofferid);
        this.getActiveOffer();
        $("#useroffer").show();
    };
    this.clickUserOffer = function(offerid) {
        this.setActiveOffer(offerid);
        $('#activeOffers').val(offerid);
    };
    this.selectActiveOffer = function() {
        var offerid = $('#activeOffers').val();
        this.setActiveOffer(offerid);
    };
    this.addOffer = function(){
        $.post("db", $("#addOfferForm").serialize(), function(data){
            var json = jQuery.parseJSON( data );
            vdk.offers[json.offerId] = json;
            vdk.renderUserOffer(json);
        });
    };
    this.addFormToOffer = function(){
        $.post("db", $("#addToOfferForm").serialize(), function(data){
            $("#message").html("<font color='green'>"+data+"</font>");
        });
    };
    this.addToOffer = function(code, id){
        $.post("db", {action:"ADDTOOFFER", idOffer: vdk.activeofferid, docCode: code, id: id}, function(data){
            $("#message").html("<font color='green'>"+data+"</font>");
        });
    };
    
    
}
;

function LoginDialog() {

}
LoginDialog.prototype = {
    _init: function() {


    }
};

function Views() {

}
Views.prototype = {
    _init: function() {


    },
    get: function() {
        var url = "db?action=LOADVIEWS";
        $.getJSON(url, function(data) {
            $.each(data.views, function(i, item) {
                $("#saved_views").append('<option value="' + item.query + '">' + item.nazev + '</option>');
            });
        });
    },
    select: function() {
        var query = $("#saved_views").val();
        window.location.href = "?" + query;
    },
    open: function() {
        
        if (!this.loaded) {
            this.dialog = $("<div/>", {title: dict['select.view']});
            $("body").append(this.dialog);
            this.dialog.load("forms/view.vm");
            this.loaded = true;
        }
        this.dialog.dialog({modal: true, width: 400, height: 300});
    },
    save: function() {
        if ($("#viewName").val() === ""){
            return;
        }
        var url = "db?action=SAVEVIEW&" + $('#viewForm').serialize() + "&" + $('#searchForm').serialize();
        $.get(url, function(data) {
            alert(data);
        });
    }
};


function Nabidka() {
    this.loaded = false;
}

Nabidka.prototype = {
    open: function() {
        if (!this.loaded) {
            this.dialog = $("<div/>", {title: dict['select.offer']});
            $("body").append(this.dialog);
            this.dialog.load("nabidka.vm", function(){
                vdk.getUserOffers();
            });
            this.loaded = true;
        }
        this.dialog.dialog({modal: true, width: 680, height: 500});
        //vdk.getUserOffers();
    },
    add: function(code){
        alert(code);
    },
    getAll: function(){
        $.getJSON("db?action=GETOFFERS", function(){
            
        })
    }
};




function Export() {
    this.loaded = false;
}

Export.prototype = {
    open: function() {
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