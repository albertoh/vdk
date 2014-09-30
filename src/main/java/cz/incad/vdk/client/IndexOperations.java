/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.vdk.client;

import cz.incad.vdkcommon.SolrIndexerCommiter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private static void indexWeOffer(Connection conn, String id, String docCode, String codeType) throws Exception {
        String sql = "select knihovna, offer from ZaznamOffer where zaznam=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();

        StringBuilder sb = new StringBuilder();
        sb.append("<add><doc>");
        sb.append("<field name=\"code\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"md5\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"code_type\">")
                .append(codeType)
                .append("</field>");
        sb.append("<field name=\"nabidka\" update=\"set\" null=\"true\" />");
        sb.append("</doc></add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
        sb = new StringBuilder();
        sb.append("<add><doc>");
        sb.append("<field name=\"code\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"md5\">")
                .append(docCode)
                .append("</field>");
        sb.append("<field name=\"code_type\">")
                .append(codeType)
                .append("</field>");
        while (rs.next()) {
            sb.append("<field name=\"nabidka\" update=\"add\">")
                    .append(rs.getInt("offer"))
                    .append("</field>");
        }
        sb.append("</doc></add>");
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

        ArrayList<String> codes = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            String docCode = rs.getString("uniqueCode");
            String codeType = rs.getString("codetype");
            if (!codes.contains(docCode)) {
                StringBuilder sb1 = new StringBuilder();
                sb1.append("<add><doc>");
                sb1.append("<field name=\"code\">")
                        .append(docCode)
                        .append("</field>");
                sb1.append("<field name=\"md5\">")
                        .append(docCode)
                        .append("</field>");
                sb1.append("<field name=\"code_type\">")
                        .append(codeType)
                        .append("</field>");
                sb1.append("<field name=\"nabidka\" update=\"set\" null=\"true\" />");
                sb1.append("<field name=\"nabidka_zaznam\" update=\"set\" null=\"true\" />");
                sb1.append("</doc></add>");
                SolrIndexerCommiter.postData(sb1.toString());
                SolrIndexerCommiter.postData("<commit/>");
                codes.add(docCode);
            }
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
            if(rs.getString("zaznam") == null){
                sb.append("<field name=\"export_json\" update=\"add\">")
                        .append(rs.getString("fields"))
                        .append("</field>");
                sb.append("<field name=\"nabidka_zaznam\" update=\"add\">")
                        .append("none")
                        .append("</field>");
            }else{
                sb.append("<field name=\"nabidka_zaznam\" update=\"add\">")
                        .append(rs.getString("zaznam"))
                        .append("</field>");
            }
            if(rs.getString("exemplar") == null){
                sb.append("<field name=\"ex_nabidka\" update=\"add\">")
                        .append("none")
                        .append("</field>");
            }else{
                sb.append("<field name=\"ex_nabidka\" update=\"add\">")
                        .append(rs.getString("exemplar"))
                        .append("</field>");
            }
            sb.append("</doc>");
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }
    
    private static void indexDemand(Connection conn, int id) throws Exception {
        String sql = "SELECT ZaznamDemand.uniqueCode, ZaznamDemand.zaznam, ZaznamDemand.exemplar, ZaznamDemand.fields, zaznam.codetype "
                + "FROM Zaznam "
                + "RIGHT OUTER JOIN ZaznamDemand "
                + "ON ZaznamDemand.zaznam=zaznam.identifikator "
                + "where ZaznamDemand.demand=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        ArrayList<String> codes = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        sb.append("<add>");
        while (rs.next()) {
            String docCode = rs.getString("uniqueCode");
            String codeType = rs.getString("codetype");
            if (!codes.contains(docCode)) {
                StringBuilder sb1 = new StringBuilder();
                sb1.append("<add><doc>");
                sb1.append("<field name=\"code\">")
                        .append(docCode)
                        .append("</field>");
                sb1.append("<field name=\"md5\">")
                        .append(docCode)
                        .append("</field>");
                sb1.append("<field name=\"code_type\">")
                        .append(codeType)
                        .append("</field>");
                sb1.append("<field name=\"poptavka\" update=\"set\" null=\"true\" />");
                sb1.append("<field name=\"poptavka_zaznam\" update=\"set\" null=\"true\" />");
                sb1.append("</doc></add>");
                SolrIndexerCommiter.postData(sb1.toString());
                SolrIndexerCommiter.postData("<commit/>");
                codes.add(docCode);
            }
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
            sb.append("<field name=\"poptavka\" update=\"add\">")
                    .append(id)
                    .append("</field>");
            if(rs.getString("zaznam") == null){
                sb.append("<field name=\"export_json\" update=\"add\">")
                        .append(rs.getString("fields"))
                        .append("</field>");
                sb.append("<field name=\"poptavka_zaznam\" update=\"add\">")
                        .append("none")
                        .append("</field>");
            }else{
                sb.append("<field name=\"poptavka_zaznam\" update=\"add\">")
                        .append(rs.getString("zaznam"))
                        .append("</field>");
            }
            if(rs.getString("exemplar") == null){
                sb.append("<field name=\"ex_poptavka\" update=\"add\">")
                        .append("none")
                        .append("</field>");
            }else{
                sb.append("<field name=\"ex_poptavka\" update=\"add\">")
                        .append(rs.getString("exemplar"))
                        .append("</field>");
            }
            sb.append("</doc>");
        }
        sb.append("</add>");
        SolrIndexerCommiter.postData(sb.toString());
        SolrIndexerCommiter.postData("<commit/>");
    }

    enum Actions {

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

        INDEXDEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        JSONObject json = new JSONObject();
                        try {
                            indexDemand(DbUtils.getConnection(), Integer.parseInt(req.getParameter("id")));
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
