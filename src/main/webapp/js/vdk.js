

/** 
 * Simple event handler used in application 
 * @constructor 
 */
function ApplicationEvents() {}

ApplicationEvents.prototype = {

        handlerEnabled:true,        

        enableHandler:function() {
                this.handlerEnabled = true;
        },
        disableHandler:function() {
                this.handlerEnabled = false;
        },        

        /** contains event handlers*/
        handlers: [],
        
        

        /** 
         * Trigger event 
         * @method
         */
        trigger:function(type, data) {
                console.log("trigger event:"+type);
                if (!this.handlerEnabled) {
                        console.log("handler disabled. Discarding event "+type);
                        return;
                }
                        
                $.each(this.handlers,function(idx,obj) { 
                        obj.apply(null, [type,data]);
                });
        },

        /** add new handler 
         *@method
         */
        addHandler: function(handler) {
                this.handlers.push(handler);
        },

        /** remove handler 
         * @method
         */
        removeHandler:function(handler) {
                /*
                var index = this.handlers.indexOf(handler);
                var nhandlers = [];
                if (index >=0)  {
                        for (var i=0;i<index;i++) {
                                 nhandlers.push(this.handlers[i]);
                        }
                        for (var i=index+1;i<this.handlers.length;i++) {
                                 nhandlers.push(this.handlers[i]);
                        }
                }
                this.handlers = nhandlers;
                */
        }
}

function VDK() {
    
   this.eventsHandler =  new ApplicationEvents();
   
    this.user = null;
    
    this.isLogged = false;
    this.zdrojUser = {
        'NKF': 'NKF',
        'UKF': 'UKF',
        'MZK': 'MZK',
        'VKOL': 'VKOLOAI'};
}
VDK.prototype = {
    
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
        a.attr('href', 'javascript:void(0)');
        a.click(function(){
            vdk.offers.addToActive(code, id,ex);
        });
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
    setUser: function(){
        $.getJSON("user.vm", _.bind(function(data){
            this.user = data;
            this.isLogged = true;
        }, this));
        
    },
    changeLanguage : function(lang){
        $("#searchForm").append('<input name="language" value="'+lang+'" type="hidden" />');
        document.getElementById("searchForm").submit();
    },
    init : function () {
        this.demands = new Demand();
        this.results = new Results();
        this.offers = new Offers();
        this.nabidka = new Nabidka();
        this.export = new Export();
        this.views = new Views();
        this.activeofferid = -1;
        this.getViews();
        //this.getOffers();
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

    },
    translate : function(key){
        if(dict.hasOwnProperty(key)){
            return dict[key];
        }else{
            return key;
        }
    },
    userOpts: function () {
        if (this.isLogged && this.zdrojUser[vdk.user.code]) {
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
    },
    selectView : function () {
        this.views.select();
    },
    getViews: function () {
        this.views.get();
    },
    saveView: function () {
        this.views.save();
    },
    openView: function () {
        this.views.open();
    },
    addToNabidka : function (id) {
        this.nabidka.add(id);
    },
    openNabidka: function () {
        this.nabidka.open();
    },
    openExport: function () {
        this.export.open();
    },
    showOriginal: function(id){
        window.open("original?id="+id, "original");
    },
    showCSV: function (csv) {
        if (!this.csv) {
            this.csvdialog = $('<div title="CSV format" class="csv1" ></div>');
            this.csv = $('<input style="width:100%;" type="text" value=""/>');
            this.csvdialog.append(this.csv);
            $("body").prepend(this.csvdialog);
            $(this.csv).focus(function () {
                $(this).select();
            });
        }
        this.csv.val(csv);
        this.csvdialog.dialog({modal: true, width: 700});
    },
    filterOnlyMatches: function() {
        var i = $("input.fq").length + 1;
        var input = '<input type="hidden" name="onlyMatches" id="onlyMatches" class="fq" value="yes" />';
        $("#searchForm").append(input);
        $("#offset").val("0");
        document.getElementById("searchForm").submit();
    }
};

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

function Nabidka() {
    this.loaded = false;
}
function Export() {
    this.loaded = false;
}

Export.prototype = {
    open: function () {
        var url = "csv/export.vm" + window.location.search + "&export=true";
        window.open(url, "export");
    }
};



var vdk = new VDK();