

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
    /** 
    * Handlers 
    * <pre><code>
    *  K5.eventsHandler
    * </code></pre>
    * @member
    */
   this.eventsHandler =  new ApplicationEvents();
   
    this.user = null;
    
    this.isLogged = false;
    this.zdrojUser = {
        'NKP': 'NKC-VDK',
        'MZK': 'MZK',
        'VKOL': 'VKOLOAI'};
    this.setUser = function(name){
        $.getJSON("user.vm", _.bind(function(data){
            this.user = data;
            this.isLogged = true;
        }, this));
        
    };
    this.init = function () {
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

    }
    this.userOpts = function () {
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
    this.showOriginal = function(id){
        window.open("original?id="+id, "original");
    };
    this.showCSV = function (csv) {
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

function Nabidka() {
    this.loaded = false;
}
function Export() {
    this.loaded = false;
}

Export.prototype = {
    open: function () {
        var url = "csv/export.vm" + window.location.search + "&rows=10000";
        window.open(url, "export");
    }
};



var vdk = new VDK();