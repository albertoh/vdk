

function VDK() {
    this.user = null;
    this.isLogged = false;
    this.results = new Results();
    this.offers = new Offers();
    this.demands = new Demand();
    this.nabidka = new Nabidka();
    this.export = new Export();
    this.views = new Views();
    this.zdrojUser = {
        'NKP': 'NKC-VDK',
        'MZK': 'MZK',
        'VKOL': 'VKOLOAI'};
    this.setUser = function(name){
        this.user = name;
        this.isLogged = true;
    }
    this.init = function () {
        this.activeofferid = -1;
        this.getViews();
        this.results.init();
        this.offers.init();
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
        if (this.zdrojUser[vdk.user]) {
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