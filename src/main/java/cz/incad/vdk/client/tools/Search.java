/*
 * Copyright (C) 2013 Alberto Hernandez
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.vdk.client.tools;

import cz.incad.vdk.client.LoggedController;
import cz.incad.vdkcommon.Options;
import cz.incad.vdkcommon.solr.IndexerQuery;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;

import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.view.ViewToolContext;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@DefaultKey("search")
public class Search {

    public static final Logger LOGGER = Logger.getLogger(Search.class.getName());

    private HttpServletRequest req;
    private Options opts;
    private boolean hasFilters = false;

    public void configure(Map props) {
        try {
            req = (HttpServletRequest) props.get("request");
            ViewToolContext vc = (ViewToolContext) props.get("velocityContext");
            opts = Options.getInstance();
            //host = opts.getString("app.host", "");
            //facets = "&facet.mincount=1&facet.field=a";

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public String getAsXML() throws JSONException {

        try {

            String q = req.getParameter("q");
            SolrQuery query = new SolrQuery();
            if (q == null || q.equals("")) {
                q = "*:*";
                query.setSort("_version_", SolrQuery.ORDER.desc);
            }
            query.setQuery(q);
            query.setFacet(true);
            query.setStart(getStart());
            query.setRows(getRows());
            
            if(LoggedController.isLogged(req)){
                query.addFacetField(opts.getStrings("user_facets"));
            }
            query.addFacetField(opts.getStrings("facets"));
            
            query.setFacetMinCount(1);
            
            JSONObject others  = opts.getJSONObject("otherParams");
            Iterator keys = others.keys();
            while (keys.hasNext() ) {
                String key = (String) keys.next();
                Object val = others.get(key);
                if(val instanceof Integer){
                    query.set(key, (Integer)val);
                }else if(val instanceof String){
                    query.set(key, (String)val);
                }else if(val instanceof Boolean){
                    query.set(key, (Boolean)val);
                }
                
            }
            addFilters(query);

            return IndexerQuery.xml(query);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void addFilters(SolrQuery query) {
        if (req.getParameterValues("zdroj") != null) {
            for (String zdroj : req.getParameterValues("zdroj")) {
                if(zdroj.startsWith("-")){
                    query.addFilterQuery("-zdroj:\"" + zdroj.substring(1) + "\"");
                }else{
                    query.addFilterQuery("zdroj:\"" + zdroj + "\"");
                }
            }
            hasFilters = true;
        }

        if (req.getParameterValues("exs") != null) {
            for (String ex : req.getParameterValues("exs")) {
                query.addFilterQuery("-" + ex);
            }
            hasFilters = true;
        }
        if (req.getParameterValues("fq") != null) {
            for (String fq : req.getParameterValues("fq")) {
                query.addFilterQuery(fq);
                if (req.getParameterValues("zdroj") != null && fq.contains("pocet_exemplaru")) {
                    String[] parts = fq.split(":");
                    for (String zdroj : req.getParameterValues("zdroj")) {
                        if (!zdroj.startsWith("-")) {
                            query.addFilterQuery("pocet_ex_" + zdroj + ":" + parts[1]);
                        }
                    }
                }
            }
            hasFilters = true;
        }
        
        if(req.getParameter("onlyOffers") != null){
            query.addFilterQuery("nabidka:[* TO *]");
            hasFilters = true;
        }

        if (req.getParameterValues("offer") != null) {
            for (String offer : req.getParameterValues("offer")) {
                query.addFilterQuery("nabidka:" + offer);
            }
            hasFilters = true;
        }
        
        if(req.getParameter("onlyDemands") != null){
            query.addFilterQuery("poptavka:[* TO *]");
            hasFilters = true;
        }
        
        if (req.getParameterValues("demand") != null) {
            for (String demand : req.getParameterValues("demand")) {
                query.addFilterQuery("poptavka:" + demand);
            }
            hasFilters = true;
        }
        
        if(req.getParameter("title") != null && !req.getParameter("title").equals("")){
            query.addFilterQuery("title:" + req.getParameter("title"));
            hasFilters = true;
        }
        
        if(req.getParameter("author") != null && !req.getParameter("author").equals("")){
            query.addFilterQuery("author:" + req.getParameter("author"));
            hasFilters = true;
        }
        
        if(req.getParameter("rok") != null && !req.getParameter("rok").equals("")){
            query.addFilterQuery("rokvydani:" + req.getParameter("rok"));
            hasFilters = true;
        }
        
        if(req.getParameter("isbn") != null && !req.getParameter("isbn").equals("")){
            query.addFilterQuery("isbn:\"" + req.getParameter("isbn") + "\"");
            hasFilters = true;
        }
        
        if(req.getParameter("issn") != null && !req.getParameter("issn").equals("")){
            query.addFilterQuery("issn:\"" + req.getParameter("issn") + "\"");
            hasFilters = true;
        }
        
        if(req.getParameter("ccnb") != null && !req.getParameter("ccnb").equals("")){
            query.addFilterQuery("ccnb:\"" + req.getParameter("ccnb") + "\"");
            hasFilters = true;
        }
        
        if(req.getParameter("vydavatel") != null && !req.getParameter("vydavatel").equals("")){
            query.addFilterQuery("vydavatel:" + req.getParameter("vydavatel"));
            hasFilters = true;
        }
 
    }
    
    public boolean getHasFilters(){
        return hasFilters;
    }

    private int getStart() throws UnsupportedEncodingException {
        String start = req.getParameter("offset");
        if (start == null || start.equals("")) {
            start = "0";
        }
        return Integer.parseInt(start);
    }

    private int getRows() throws UnsupportedEncodingException {
        String rows = req.getParameter("hits");
        if (rows == null || rows.equals("")) {
            rows = "40";
        }
        return Integer.parseInt(rows);
    }

}
