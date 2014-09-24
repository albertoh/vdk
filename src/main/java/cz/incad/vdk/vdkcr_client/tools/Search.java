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
package cz.incad.vdk.vdkcr_client.tools;

import cz.incad.vdkcommon.Options;
import cz.incad.vdkcommon.solr.IndexerQuery;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;

import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.view.ViewToolContext;
import org.json.JSONArray;
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
    private LanguageTool i18n;
    private String host;
    private Options opts;

    private String facets;
    private final String groupedParams = "&group.field=root_pid&group.type=normal&group.threshold=1"
            + "&group.facet=false&group=true&group.truncate=true&group.ngroups=true";
    private final String hlParams = "&hl=true&hl.fl=text_ocr&hl.mergeContiguous=true&hl.snippets=2";

    public void configure(Map props) {
        try {
            req = (HttpServletRequest) props.get("request");
            ViewToolContext vc = (ViewToolContext) props.get("velocityContext");
            opts = Options.getInstance();
            host = opts.getString("app.host", "");
            facets = "&facet.mincount=1&facet.field=a";
            

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    
    public String getAsXML(){
        
        try {

            String q = req.getParameter("q");
            if (q == null || q.equals("")) {
                q = "*:*";
            } else {
                q = URLEncoder.encode(q, "UTF-8");
            }
            
            SolrQuery query = new SolrQuery(q);
            query.setFacet(true);
            query.setStart(getStart());
            query.setRows(getRows());
            
            query.addFacetField(opts.getStrings("facets"));
            
            
            return IndexerQuery.xml(query);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        } 
        
        
    }
    

    private void usedFilter(Map<String, String> map, String param) {
        String p = req.getParameter(param);
        if (p != null && !p.equals("")) {
            map.put(param, p);
        }
    }

    private void usedFilter(Map<String, String> map, String param, String field) {
        String p = req.getParameter(param);
        if (p != null && !p.equals("")) {
            map.put(param, p);
        }
    }

    public Map<String, String> getUsedFilters() {
        Map<String, String> map = new HashMap<String, String>();
        usedFilter(map, "author");
        usedFilter(map, "udc", "mdt");
        usedFilter(map, "ddc", "ddt");
        usedFilter(map, "rok");
        usedFilter(map, "keywords");
        usedFilter(map, "collection");
        usedFilter(map, "dostupnost", "dostupnost");
        usedFilter(map, "issn");
        return map;
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

    private String getSort() throws UnsupportedEncodingException {
        String res;
        String sort = req.getParameter("sort");
        String asis = req.getParameter("asis");
        String q = req.getParameter("q");
        boolean fieldedSearch = false;
        if (sort != null && !sort.equals("") && asis != null) {
            res = sort;
        } else if (sort != null && !sort.equals("")) {
            res = sort;
        } else if (sort != null && !sort.equals("")) {
            res = "level asc, " + sort;
        } else if ((q == null || q.equals(""))) {
            res = "title_sort asc";
        } else if (q != null && !q.equals("")) {
            res = "score desc, title_sort asc";
        } else if (fieldedSearch) {
            res = "level asc, title_sort asc, score desc";
        } else if (q == null || q.equals("")) {
            res = "level asc, title_sort asc, score desc";
        } else {
            res = "score desc";
        }

        return "&sort=" + URLEncoder.encode(res, "UTF-8");
    }
}