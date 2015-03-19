/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.vdk.client;

import cz.incad.vdkcommon.DbUtils;
import cz.incad.vdkcommon.SolrIndexerCommiter;
import cz.incad.vdkcommon.solr.Indexer;
import cz.incad.vdkcommon.solr.IndexerQuery;
import cz.incad.vdkcommon.solr.Storage;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class IndexOperations extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(IndexOperations.class.getName());
    public static final String ACTION_NAME = "action";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param req servlet request
     * @param resp servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String actionNameParam = req.getParameter(ACTION_NAME);
            if (actionNameParam != null) {
                Actions actionToDo = IndexOperations.Actions.valueOf(actionNameParam);
                actionToDo.doPerform(req, resp);
            } else {
                PrintWriter out = resp.getWriter();
                out.print("actionNameParam -> " + actionNameParam);
            }
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            PrintWriter out = resp.getWriter();
            out.print(e1.toString());
        } catch (SecurityException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = resp.getWriter();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            out.print(e1.toString());
        }
    }

    private static void removeWanted(String knihovna, String code, String codeType) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<add><doc>");
        sb.append("<field name=\"code\">")
                .append(code)
                .append("</field>");
        sb.append("<field name=\"md5\">")
                .append(code)
                .append("</field>");
        sb.append("<field name=\"code_type\">")
                .append(codeType)
                .append("</field>");
        sb.append("<field name=\"chci\" update=\"remove\">").append(knihovna).append("</field>");
        sb.append("<field name=\"nechci\" update=\"remove\">").append(knihovna).append("</field>");
        sb.append("</doc></add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static void indexWanted(Connection conn, int wanted_id) throws Exception {
        String sql = "select w.wants, zo.knihovna, k.code, zo.uniquecode from WANTED w, KNIHOVNA k, ZAZNAMOFFER zo "
                + "where w.wanted_id=? "
                + "and w.knihovna=k.knihovna_id and zo.zaznamoffer_id=w.zaznamoffer";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, wanted_id);

        ResultSet rs = ps.executeQuery();

        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            sb.append("<doc>");
            String uniquecode = rs.getString("uniquecode");
            sb.append("<field name=\"code\">")
                    .append(uniquecode)
                    .append("</field>");
            sb.append("<field name=\"md5\">")
                    .append(uniquecode)
                    .append("</field>");
            if (rs.getBoolean(1)) {
                sb.append("<field name=\"chci\" update=\"add\">")
                        .append(rs.getString("code"))
                        .append("</field>");
            } else {
                sb.append("<field name=\"nechci\" update=\"add\">")
                        .append(rs.getString("code"))
                        .append("</field>");
            }
            sb.append("</doc>");
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static StringBuilder doIndexOfferXml(int id, String docCode, String codeType, ResultSet rs) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<doc>");
        sb.append("<field name=\"code\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"md5\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"code_type\">")
                .append(codeType)
                .append("</field>");
        sb.append("<field name=\"nabidka\" update=\"add\">")
                .append(id)
                .append("</field>");
        JSONObject nabidka_ext = new JSONObject();
        JSONObject nabidka_ext_n = new JSONObject();
        nabidka_ext_n.put("zaznamOffer", rs.getInt("zaznamoffer_id"));
        nabidka_ext_n.put("code", docCode);
        nabidka_ext_n.put("zaznam", rs.getString("zaznam"));
        nabidka_ext_n.put("ex", rs.getString("exemplar"));
        nabidka_ext_n.put("fields", new JSONObject(rs.getString("fields")));
        nabidka_ext.put("" + id, nabidka_ext_n);
        sb.append("<field name=\"nabidka_ext\" update=\"add\">")
                .append(nabidka_ext)
                .append("</field>");
        sb.append("</doc>");
        return sb;
    }

    private static void indexOffer(Connection conn, int id) throws Exception {
        String sql = "SELECT ZaznamOffer.zaznamoffer_id, ZaznamOffer.uniqueCode, "
                + "ZaznamOffer.zaznam, ZaznamOffer.exemplar, ZaznamOffer.fields "
                + "FROM zaznamOffer "
                + "ON ZaznamOffer.zaznam=zaznam.identifikator "
                + "where ZaznamOffer.offer=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            SolrDocument sdoc = Storage.getDoc(rs.getString("zaznam"));
            if (sdoc != null) {
                sb.append(doIndexOfferXml(rs.getInt("offer"),
                        (String) sdoc.getFieldValue("code"),
                        (String) sdoc.getFieldValue("code_type"),
                        rs));
            }
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    

    private static void removeAllWanted() throws Exception {
        SolrQuery query = new SolrQuery("chci:[* TO *]");
        query.addField("code");
        SolrDocumentList docs = IndexerQuery.query(query);
        Iterator<SolrDocument> iter = docs.iterator();
        while (iter.hasNext()) {
            StringBuilder sb = new StringBuilder();

            SolrDocument resultDoc = iter.next();
            String docCode = (String) resultDoc.getFieldValue("code");
            sb.append("<add><doc>");
            sb.append("<field name=\"code\">")
                    .append(docCode)
                    .append("</field>");
            sb.append("<field name=\"md5\">")
                    .append(docCode)
                    .append("</field>");

            sb.append("<field name=\"chci\" update=\"set\" null=\"true\" />");
            sb.append("</doc></add>");
            SolrIndexerCommiter.postData(sb.toString());
            SolrIndexerCommiter.postData("<commit/>");
        }
        query.setQuery("nechci:[* TO *]");
        query.addField("code");
        docs = IndexerQuery.query(query);
        docs.iterator();
        while (iter.hasNext()) {
            StringBuilder sb = new StringBuilder();

            SolrDocument resultDoc = iter.next();
            String docCode = (String) resultDoc.getFieldValue("code");
            sb.append("<add><doc>");
            sb.append("<field name=\"code\">")
                    .append(docCode)
                    .append("</field>");
            sb.append("<field name=\"md5\">")
                    .append(docCode)
                    .append("</field>");

            sb.append("<field name=\"chci\" update=\"set\" null=\"true\" />");
            sb.append("</doc></add>");
            SolrIndexerCommiter.postData(sb.toString());
            SolrIndexerCommiter.postData("<commit/>");
        }
    }

    private static void removeAllOffers() throws Exception {
        SolrQuery query = new SolrQuery("nabidka:[* TO *]");
        query.addField("code");
        SolrDocumentList docs = IndexerQuery.query(query);
        Iterator<SolrDocument> iter = docs.iterator();
        while (iter.hasNext()) {
            StringBuilder sb = new StringBuilder();

            SolrDocument resultDoc = iter.next();
            String docCode = (String) resultDoc.getFieldValue("code");
            sb.append("<add><doc>");
            sb.append("<field name=\"code\">")
                    .append(docCode)
                    .append("</field>");
            sb.append("<field name=\"md5\">")
                    .append(docCode)
                    .append("</field>");

            sb.append("<field name=\"nabidka\" update=\"set\" null=\"true\" />");
            sb.append("<field name=\"nabidka_ext\" update=\"set\" null=\"true\" />");
            sb.append("</doc></add>");
            SolrIndexerCommiter.postData(sb.toString());
            SolrIndexerCommiter.postData("<commit/>");
        }
    }

    private static StringBuilder doIndexDemandXml(String knihovna,
            String docCode,
            String zaznam,
            String exemplar, String update) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<doc>");
        sb.append("<field name=\"code\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"md5\">")
                .append(docCode)
                .append("</field>");

        sb.append("<field name=\"poptavka\" update=\"").append(update).append("\">")
                .append(knihovna)
                .append("</field>");
        JSONObject j = new JSONObject();
        j.put("knihovna", knihovna);
        j.put("code", docCode);
        j.put("zaznam", zaznam);
        j.put("exemplar", exemplar);
        sb.append("<field name=\"poptavka_ext\" update=\"").append(update).append("\">")
                .append(j)
                .append("</field>");

        sb.append("</doc>");
        return sb;
    }

    

    private static void indexDemand(String knihovna,
            String docCode,
            String zaznam,
            String exemplar) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        sb.append(doIndexDemandXml(knihovna,
                docCode,
                zaznam,
                exemplar,
                "add"));
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static void removeDemand(String knihovna,
            String docCode,
            String zaznam,
            String exemplar) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        sb.append(doIndexDemandXml(knihovna,
                docCode,
                zaznam,
                exemplar,
                "remove"));
        sb.append("</add>");
        LOGGER.log(Level.INFO, sb.toString());
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static void removeAllDemands() throws Exception {
        SolrQuery query = new SolrQuery("poptavka:[* TO *]");
        query.addField("code");
        SolrDocumentList docs = IndexerQuery.query(query);
        Iterator<SolrDocument> iter = docs.iterator();
        while (iter.hasNext()) {
            StringBuilder sb = new StringBuilder();

            SolrDocument resultDoc = iter.next();
            String docCode = (String) resultDoc.getFieldValue("code");
            sb.append("<add><doc>");
            sb.append("<field name=\"code\">")
                    .append(docCode)
                    .append("</field>");
            sb.append("<field name=\"md5\">")
                    .append(docCode)
                    .append("</field>");

            sb.append("<field name=\"poptavka\" update=\"set\" null=\"true\" />");
            sb.append("<field name=\"poptavka_ext\" update=\"set\" null=\"true\" />");
            sb.append("</doc></add>");
            SolrIndexerCommiter.postData(sb.toString());
            SolrIndexerCommiter.postData("<commit/>");
        }
    }

    enum Actions {

        INDEXWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            indexWanted(DbUtils.getConnection(), Integer.parseInt(req.getParameter("id")));
                            json.put("message", "Reakce pridana.");
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEALLWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            removeAllWanted();
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        INDEXALLWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {

                            Indexer indexer = new Indexer();
                            indexer.indexAllWanted();
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        INDEXALLOFFERS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Indexer indexer = new Indexer();
                            indexer.indexAllOffers();
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        INDEXOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            indexOffer(DbUtils.getConnection(), Integer.parseInt(req.getParameter("id")));
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Indexer indexer = new Indexer();
                            indexer.removeOffer(Integer.parseInt(req.getParameter("id")));
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEALLOFFERS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            removeAllOffers();
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        ADDDEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {
                                indexDemand(kn.getCode(),
                                        req.getParameter("docCode"),
                                        req.getParameter("zaznam"),
                                        req.getParameter("ex"));
                            }
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEDEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {
                                removeDemand(kn.getCode(),
                                        req.getParameter("docCode"),
                                        req.getParameter("zaznam"),
                                        req.getParameter("ex"));
                            }
                        } catch (Exception ex) {
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        INDEXALLDEMANDS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {
                                Indexer indexer = new Indexer();
                                indexer.indexAllDemands();
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEALLDEMANDS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {
                                removeAllDemands();
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REINDEXDOCS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {

                                if (kn.hasRole(DbUtils.Roles.ADMIN)) {
                                    Indexer indexer = new Indexer();
                                    indexer.reindex();
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REINDEX {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {

                                if (kn.hasRole(DbUtils.Roles.ADMIN)) {
                                    Indexer indexer = new Indexer();
                                    indexer.reindex();
//                                    indexAllOffers(DbUtils.getConnection());
//                                    indexAllDemands(DbUtils.getConnection());
//                                    indexAllWanted(DbUtils.getConnection());
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                },
        REINDEXDOC {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "rights.notlogged");
                            } else {

                                if (kn.hasRole(DbUtils.Roles.ADMIN)) {
                                    Indexer indexer = new Indexer();
                                    indexer.reindexDoc(req.getParameter("code"));
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            json.put("error", ex.toString());
                        }
                        out.println(json.toString());
                    }
                };

        abstract void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
