/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.vdk.client;

import cz.incad.vdkcommon.SolrIndexerCommiter;
import cz.incad.vdkcommon.solr.IndexerQuery;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
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

    private static void indexWanted(Connection conn, String id, String code, String codeType) throws Exception {
        String sql = "select wants, knihovna, code from WANTED, KNIHOVNA where zaznam=? and knihovna=knihovna_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();

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
        sb.append("<field name=\"chci\" update=\"set\" null=\"true\" />");
        sb.append("<field name=\"nechci\" update=\"set\" null=\"true\" />");
        sb.append("</doc></add>");
        SolrIndexerCommiter.postData(sb.toString());
        sb = new StringBuilder();
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
        while (rs.next()) {
            if (rs.getBoolean(1)) {
                sb.append("<field name=\"chci\" update=\"set\">")
                        .append(rs.getString("code"))
                        .append("</field>");
            } else {
                sb.append("<field name=\"nechci\" update=\"set\">")
                        .append(rs.getString("code"))
                        .append("</field>");
            }
        }
        sb.append("</doc></add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }
    
    private static StringBuilder doIndexOfferXml(int id, ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder();
        String docCode = rs.getString("uniqueCode");
            String codeType = rs.getString("codetype");
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
            nabidka_ext_n.put("zaznam", rs.getString("zaznam"));
            nabidka_ext_n.put("ex", rs.getString("exemplar"));
            nabidka_ext_n.put("fields", rs.getString("fields"));
            nabidka_ext.put(""+id, nabidka_ext_n);
            sb.append("<field name=\"nabidka_ext\" update=\"add\">")
                    .append(nabidka_ext)
                    .append("</field>");
            sb.append("</doc>");
            return sb;
    }
    

    private static void indexAllOffers(Connection conn) throws Exception {
        
        String sql = "SELECT ZaznamOffer.offer, ZaznamOffer.uniqueCode, ZaznamOffer.zaznam, ZaznamOffer.exemplar, ZaznamOffer.fields, zaznam.codetype "
                + "FROM Zaznam "
                + "RIGHT OUTER JOIN zaznamOffer "
                + "ON ZaznamOffer.zaznam=zaznam.identifikator ";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            sb.append(doIndexOfferXml(rs.getInt("offer"), rs));
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static void indexOffer(Connection conn, int id) throws Exception {
        String sql = "SELECT ZaznamOffer.uniqueCode, ZaznamOffer.zaznam, ZaznamOffer.exemplar, ZaznamOffer.fields, zaznam.codetype "
                + "FROM Zaznam "
                + "RIGHT OUTER JOIN zaznamOffer "
                + "ON ZaznamOffer.zaznam=zaznam.identifikator "
                + "where ZaznamOffer.offer=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            sb.append(doIndexOfferXml(id, rs));
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static void removeOffer(Connection conn, int id) throws Exception {
        String sql = "SELECT ZaznamOffer.uniqueCode, ZaznamOffer.zaznam, ZaznamOffer.exemplar, ZaznamOffer.fields, zaznam.codetype "
                + "FROM Zaznam "
                + "RIGHT OUTER JOIN zaznamOffer "
                + "ON ZaznamOffer.zaznam=zaznam.identifikator "
                + "where ZaznamOffer.offer=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            String docCode = rs.getString("uniqueCode");
            String codeType = rs.getString("codetype");
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
            sb.append("<field name=\"nabidka\" update=\"remove\">")
                    .append(id)
                    .append("</field>");
            JSONObject nabidka_ext = new JSONObject();
            JSONObject nabidka_ext_n = new JSONObject();
            nabidka_ext_n.put("zaznam", rs.getString("zaznam"));
            nabidka_ext_n.put("ex", rs.getString("exemplar"));
            nabidka_ext_n.put("fields", rs.getString("fields"));
            nabidka_ext.put(""+id, nabidka_ext_n);
            sb.append("<field name=\"nabidka_ext\" update=\"remove\">")
                    .append(nabidka_ext)
                    .append("</field>");
            
            sb.append("</doc>");
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
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
    
    private static void indexDemand(String knihovna,
            String docCode,
            String zaznam,
            String exemplar) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<add><doc>");
        sb.append("<field name=\"code\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"md5\">")
                .append(docCode)
                .append("</field>");

        sb.append("<field name=\"poptavka\" update=\"add\">")
                .append(knihovna)
                .append("</field>");
        if (zaznam == null) {
            sb.append("<field name=\"poptavka_zaznam\" update=\"add\">")
                    .append(knihovna).append(";").append("none")
                    .append("</field>");
        } else {
            sb.append("<field name=\"poptavka_zaznam\" update=\"add\">")
                    .append(knihovna).append(";").append(zaznam)
                    .append("</field>");
        }
        if (exemplar == null) {
            sb.append("<field name=\"ex_poptavka\" update=\"add\">")
                    .append(knihovna).append(";").append("none")
                    .append("</field>");
        } else {
            sb.append("<field name=\"ex_poptavka\" update=\"add\">")
                    .append(knihovna).append(";").append(exemplar)
                    .append("</field>");
        }
        sb.append("</doc></add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    private static void removeDemand(String knihovna,
            String docCode,
            String zaznam,
            String exemplar) throws Exception {

        StringBuilder sb1 = new StringBuilder();
        sb1.append("<add><doc>");
        sb1.append("<field name=\"code\">")
                .append(docCode)
                .append("</field>");
        sb1.append("<field name=\"md5\">")
                .append(docCode)
                .append("</field>");
        sb1.append("<field name=\"poptavka\" update=\"remove\">")
                .append(knihovna)
                .append("</field>");
        sb1.append("<field name=\"ex_poptavka\" update=\"remove\">")
                .append(knihovna).append(";").append(exemplar)
                .append("</field>");
        sb1.append("<field name=\"poptavka_zaznam\" update=\"remove\">")
                .append(knihovna).append(";").append(zaznam)
                .append("</field>");
        sb1.append("</doc></add>");
        SolrIndexerCommiter.postData(sb1.toString());
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
            sb.append("<field name=\"poptavka_zaznam\" update=\"set\" null=\"true\" />");
            sb.append("<field name=\"ex_poptavka\" update=\"set\" null=\"true\" />");
            sb.append("</doc></add>");
            SolrIndexerCommiter.postData(sb.toString());
            SolrIndexerCommiter.postData("<commit/>");
        }
    }

    enum Actions {

        INDEXALLOFFERS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            indexAllOffers(DbUtils.getConnection());
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
                            removeOffer(DbUtils.getConnection(), Integer.parseInt(req.getParameter("id")));
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
                                json.put("error", "Nejste prihlasen");
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
                                json.put("error", "Nejste prihlasen");
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
        REMOVEALLDEMANDS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn == null) {
                                json.put("error", "Nejste prihlasen");
                            } else {
                                removeAllDemands();
                            }
                        } catch (Exception ex) {
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
