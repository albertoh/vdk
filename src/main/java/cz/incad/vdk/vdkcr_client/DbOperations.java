package cz.incad.vdk.vdkcr_client;

import cz.incad.vdkcommon.Slouceni;
import cz.incad.vdkcommon.SolrIndexerCommiter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

public class DbOperations extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(DbOperations.class.getName());
    public static final String ACTION_NAME = "action";
    static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    static final int EXPIRATION_DAYS = 35;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param req servlet req
     * @param resp servlet resp
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        try {
            String actionNameParam = req.getParameter(ACTION_NAME);
            if (actionNameParam != null) {
                Actions actionToDo = Actions.valueOf(actionNameParam);
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

    public static int insertOfferPg(Connection conn, String name, int idKnihovna, InputStream uploadedStream) throws Exception {
        int retVal = 0;
        String sql = "insert into OFFER (nazev, bdata, knihovna, update_timestamp, closed) values (?,?,?, NOW(), false) returning offer_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, name);
        if (uploadedStream != null) {
            ps.setBinaryStream(2, uploadedStream, uploadedStream.available());
        } else {
            ps.setNull(2, Types.BINARY);
        }
        ps.setInt(3, idKnihovna);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            return 0;
        }

    }

    public static int closeOffer(Connection conn, int offerid) throws Exception {

        String sql = "update OFFER set closed=? where offer_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setBoolean(1, true);
        ps.setInt(2, offerid);
        return ps.executeUpdate();
    }

    public static int insertOfferOracle(Connection conn, String name, int idKnihovna, InputStream uploadedStream) throws Exception {
        int retVal = 1;
        String sql1 = "select Offer_ID_SQ.nextval from dual";
        ResultSet rs = conn.prepareStatement(sql1).executeQuery();
        if (rs.next()) {
            retVal = rs.getInt(1);
        }

        String sql = "insert into OFFER (nazev, bdata, knihovna, update_timestamp, offer_id, closed) values (?,?,?, NOW(), ?, false) returning offer_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, name);
        if (uploadedStream != null) {
            ps.setBinaryStream(2, uploadedStream, uploadedStream.available());
        } else {
            ps.setNull(2, Types.BINARY);
        }
        ps.setInt(3, idKnihovna);
        ps.setInt(4, retVal);
        ps.executeUpdate();
        return retVal;

    }

    public static void insertNabidka(Connection conn, int idKnihovna, String id, String docCode, int idOffer, String line) throws Exception {

        if (isOracle(conn)) {
            String sql1 = "select Nabidky_ID_SQ.nextval from dual";
            ResultSet rs = conn.prepareStatement(sql1).executeQuery();
            int idW = 1;
            if (rs.next()) {
                idW = rs.getInt(1);
            }

            String sql = "insert into NABIDKY (uniqueCode, zaznam, knihovna, nabidky_id, offer, fields) values (?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, docCode);
            ps.setString(2, id);
            ps.setInt(3, idKnihovna);
            ps.setInt(4, idW);
            ps.setInt(5, idOffer);
            ps.setString(6, line);
            ps.executeUpdate();
        } else {
            String sql = "insert into NABIDKY (uniqueCode, zaznam, knihovna,offer,fields) values (?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, docCode);
            ps.setString(2, id);
            ps.setInt(3, idKnihovna);
            ps.setInt(4, idOffer);
            ps.setString(5, line);
            ps.executeUpdate();
        }
        //indexWeOffer(conn, id, docCode, "md5");
    }

    public static void processStream(Connection conn, InputStream is, int idKnihovna, int idOffer) throws Exception {
        try {
            CSVStrategy strategy = new CSVStrategy('\t', '\"', '#');
            CSVParser parser = new CSVParser(new InputStreamReader(is), strategy);
            String[] parts = parser.getLine();
            while (parts != null) {
                String id = parts[0];
                String ccnb = parts[1];
                String line = "";
                for (String s : parts) {
                    line += s + "\t";
                }

                String docCode;
                String codeType;
                docCode = Slouceni.generateMD5(parts);
                codeType = "ccnb";

                insertNabidka(conn, idKnihovna, id, docCode, idOffer, line);
                //indexWeOffer(conn, id, docCode, codeType);
                parts = parser.getLine();
            }
        } catch (Exception ex) {
            throw new Exception("Not valid cvs file. Separator must be tabulator and line must be 'id ccnb nazev autor mistovydani vydavatel datumvydani'", ex);
        }
    }

    public static JSONObject getOffer(String id) throws Exception {

        Connection conn = null;
        JSONObject json = new JSONObject();
        try {
            conn = DbUtils.getConnection();
            String sql = "select * from nabidky where offer=" + id;
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("nabidkaId", rs.getString("nabidky_id"));
                j.put("zaznam", rs.getString("zaznam"));
                j.put("fields", rs.getString("fields"));

                json.put(id, j);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            json.put("error", ex);
        } finally {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        return json;
    }

    public static JSONObject getOffers() throws Exception {

        Calendar now = Calendar.getInstance();
        Calendar o = Calendar.getInstance();
        Connection conn = null;
        JSONObject json = new JSONObject();
        try {
            conn = DbUtils.getConnection();
            String sql = "select OFFER.*, KNIHOVNA.code from OFFER, KNIHOVNA where OFFER.knihovna=KNIHOVNA.knihovna_id";
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date offerDate = rs.getDate("update_timestamp");
                o.setTime(offerDate);
                o.add(Calendar.DATE, EXPIRATION_DAYS);
                JSONObject j = new JSONObject();
                j.put("offerId", rs.getString("offer_id"));
                j.put("nazev", rs.getString("nazev"));
                j.put("knihovna", rs.getString("code"));
                j.put("closed", rs.getBoolean("closed"));
                j.put("date", sdf.format(offerDate));
                j.put("expires", sdf.format(o.getTime()));
                j.put("expired", !o.after(now));

                json.put(rs.getString("offer_id"), j);
            }
        } catch (Exception ex) {
            json.put("error", ex);
        } finally {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        return json;
    }

    //public static int getIdKnihovna_(String code, Connection conn) throws Exception {
    public static int getIdKnihovna(HttpServletRequest req) throws Exception {
        String user = req.getRemoteUser();
        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
        if (kn != null) {
            return kn.getId();
        }
        return 0;
    }

    public static boolean isOracle(Connection conn) throws SQLException {
        DatabaseMetaData p = conn.getMetaData();
        return p.getDatabaseProductName().toLowerCase().contains("oracle");
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
        String sql = "select knihovna, offer from NABIDKY where zaznam=?";
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

    enum Actions {

        GETWEOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        String id = req.getParameter("id");
                        String exem = req.getParameter("ex");
                        String user = req.getRemoteUser();
                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }

                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();

                            String sql = "select zaznam from NABIDKY where zaznam=? and knihovna=? and exemplar=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setString(1, id);
                            ps.setInt(2, idKnihovna);
                            ps.setString(3, exem);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                out.print("true");
                            } else {
                                out.print("0");
                            }
                        } catch (Exception ex) {
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        DELETEWEOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        String id = req.getParameter("id");
                        String exem = req.getParameter("ex");
                        String docCode = req.getParameter("code");
                        String user = req.getRemoteUser();
                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        try {
                            conn = DbUtils.getConnection();
                            String sql = "delete from NABIDKY where zaznam=? and knihovna=? and exemplar=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setString(1, id);
                            ps.setInt(2, idKnihovna);
                            ps.setString(3, exem);
                            ps.executeUpdate();
                            indexWeOffer(conn, id, docCode, "md5");
                            LOGGER.log(Level.INFO, id + " deleted");
                            out.println(id + " deleted");
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Error updating WEOFFER", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        SAVEWEOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        String id = req.getParameter("id");
                        String docCode = req.getParameter("code");
                        String line = req.getParameter("line");
                        String user = req.getRemoteUser();

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }

                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();
                            insertNabidka(conn, idKnihovna, id, docCode, 0, line);
                            out.println(id + " added");
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Error saving WEOFFER", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        GETWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        String id = req.getParameter("id");
                        String user = req.getRemoteUser();

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();
                            String sql = "select wants from WANTED where zaznam=? and knihovna=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setString(1, id);
                            ps.setInt(2, idKnihovna);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                out.print(rs.getBoolean(1));
                            } else {
                                out.print("0");
                            }
                        } catch (Exception ex) {
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        UPDATEWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        boolean wants = Boolean.parseBoolean(req.getParameter("wanted"));
                        String id = req.getParameter("id");
                        String code = req.getParameter("code");
                        String user = req.getRemoteUser();
                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        try {
                            conn = DbUtils.getConnection();
                            String sql = "update WANTED set wants=? where zaznam=? and knihovna=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setBoolean(1, wants);
                            ps.setString(2, id);
                            ps.setInt(3, idKnihovna);
                            ps.executeUpdate();
                            indexWanted(conn, id, code, "md5");
                            out.println(id + " updated");
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Error updating WANTED", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        SAVEWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        boolean wants = Boolean.parseBoolean(req.getParameter("wanted"));
                        String id = req.getParameter("id");
                        String code = req.getParameter("code");
                        String user = req.getRemoteUser();
                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        try {
                            conn = DbUtils.getConnection();
                            if (isOracle(conn)) {
                                String sql1 = "select Wanted_ID_SQ.nextval from dual";
                                ResultSet rs = conn.prepareStatement(sql1).executeQuery();
                                int idW = 1;
                                if (rs.next()) {
                                    idW = rs.getInt(1);
                                }

                                String sql = "insert into WANTED (zaznam, knihovna, wants, wanted_id) values (?,?,?,?)";
                                PreparedStatement ps = conn.prepareStatement(sql);
                                ps.setString(1, id);
                                ps.setInt(2, idKnihovna);
                                ps.setBoolean(3, wants);
                                ps.setInt(4, idW);
                                ps.executeUpdate();
                            } else {
                                String sql = "insert into WANTED (zaznam, knihovna, wants) values (?,?,?)";
                                PreparedStatement ps = conn.prepareStatement(sql);
                                ps.setString(1, id);
                                ps.setInt(2, idKnihovna);
                                ps.setBoolean(3, wants);
                                ps.executeUpdate();
                            }
                            indexWanted(conn, id, code, "md5");
                            out.println(id + " added");
                        } catch (Exception ex) {
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        SAVEVIEW {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        String query = req.getQueryString().replace("action=SAVEVIEW&", "");
                        String name = req.getParameter("viewName");
                        boolean isGlobal = "on".equals(req.getParameter("viewGlobal"));
                        String user = req.getRemoteUser();

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        try {
                            conn = DbUtils.getConnection();
                            boolean oracle = true;
                            if (isOracle(conn)) {
                                String sql1 = "select Pohled_ID_SQ.nextval from dual";
                                ResultSet rs = conn.prepareStatement(sql1).executeQuery();
                                int idPohled = 1;
                                if (rs.next()) {
                                    idPohled = rs.getInt(1);
                                }

                                String sql = "insert into POHLED (nazev, query, knihovna, isGlobal, pohled_id) values (?,?,?,?,?)";
                                PreparedStatement ps = conn.prepareStatement(sql);
                                ps.setString(1, name);
                                ps.setString(2, query);
                                ps.setInt(3, idKnihovna);
                                ps.setBoolean(4, isGlobal);
                                ps.setInt(4, idPohled);
                                ps.executeUpdate();
                            } else {

                                String sql = "insert into POHLED (nazev, query, knihovna, isGlobal) values (?,?,?,?)";
                                PreparedStatement ps = conn.prepareStatement(sql);
                                ps.setString(1, name);
                                ps.setString(2, query);
                                ps.setInt(3, idKnihovna);
                                ps.setBoolean(4, isGlobal);
                                ps.executeUpdate();
                            }
                            ;
                            out.println("Pohled " + name + " ulozen:");
                            out.println("query " + query);

                        } catch (Exception ex) {
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }

                    }
                },
        LOADVIEWS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();

                            String sql = "select * from POHLED where isGlobal='true' OR knihovna=" + getIdKnihovna(req);
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery();
                            JSONObject json = new JSONObject();
                            JSONArray jarray = new JSONArray();
                            json.put("knihovna", getIdKnihovna(req));
                            json.put("views", jarray);
                            while (rs.next()) {
                                JSONObject o = new JSONObject();
                                o.put("nazev", rs.getString("nazev"));
                                o.put("query", rs.getString("query"));
                                o.put("isGlobal", rs.getBoolean("isGlobal"));
                                o.put("knihovna", rs.getInt("knihovna"));
                                jarray.put(o);
                            }
                            out.println(json.toString());

                        } catch (Exception ex) {
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }

                    }
                },
        LOADOFFERS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();

                            String sql = "select * from OFFER";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery();
                            JSONObject json = new JSONObject();
                            JSONArray jarray = new JSONArray();
                            json.put("views", jarray);
                            while (rs.next()) {
                                JSONObject o = new JSONObject();
                                o.put("id", rs.getString("offer_id"));
                                o.put("nazev", rs.getString("nazev"));
                                jarray.put(o);
                            }
                            out.println(json.toString());

                        } catch (Exception ex) {
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }

                    }
                },
        CLOSEOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        int offerid = Integer.parseInt(req.getParameter("id"));

                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();
                            out.print(closeOffer(conn, offerid));

                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "upload failed", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        NEWOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        String name = req.getParameter("offerName");
                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        try {
                            conn = DbUtils.getConnection();
                            int idOffer = 0;
                            if (isOracle(conn)) {
                                idOffer = insertOfferOracle(conn, name, idKnihovna, null);
                            } else {
                                idOffer = insertOfferPg(conn, name, idKnihovna, null);
                            }
                            
                            Calendar now = Calendar.getInstance();
                            Calendar o = Calendar.getInstance();
                            o.add(Calendar.DATE, EXPIRATION_DAYS);
                            JSONObject j = new JSONObject();
                            j.put("offerId", Integer.toString(idOffer));
                            j.put("nazev", name);
                            j.put("knihovna", idKnihovna);
                            j.put("closed", false);
                            j.put("date", sdf.format(o.getTime()));
                            j.put("expires", sdf.format(o.getTime()));
                            j.put("expired", !o.after(now));
                            
                            out.println(j.toString());

                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "upload failed", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        OFFERTOJSON {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        Map<String, String> parts = new HashMap<String, String>();

                        parts.put("isbn", req.getParameter("isbn"));
//            String pole = parts.get("isbn");
//            pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));            
//            ISBNValidator val =  new ISBNValidator();
//            if(!"".equals(pole)){
//                out.println(val.validate(pole));
//            }

                        parts.put("issn", req.getParameter("issn"));
                        parts.put("ccnb", req.getParameter("ccnb"));
                        parts.put("245a", req.getParameter("titul"));
                        parts.put("245n", req.getParameter("f245n"));
                        parts.put("245p", req.getParameter("f245p"));
                        parts.put("250a", req.getParameter("f250a"));
                        parts.put("100a", req.getParameter("f100a"));
                        parts.put("110a", req.getParameter("f110a"));
                        parts.put("111a", req.getParameter("f111a"));
                        parts.put("260a", req.getParameter("f260"));
                        out.println("Nabidka " + Slouceni.toJSON(parts).toString());

                    }
                },
        ADDTOOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        int idOffer = Integer.parseInt(req.getParameter("idOffer"));
                        String id = req.getParameter("id");
                        String docCode = req.getParameter("docCode");

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }

                        Map<String, String> parts = new HashMap<String, String>();
                        if (docCode == null || "".equals(docCode)) {

                            //String[] parts = new String[11];
                            parts.put("isbn", req.getParameter("isbn"));
                            parts.put("issn", req.getParameter("issn"));
                            parts.put("ccnb", req.getParameter("ccnb"));
                            parts.put("245a", req.getParameter("titul"));
                            parts.put("245n", req.getParameter("f245n"));
                            parts.put("245p", req.getParameter("f245p"));
                            parts.put("250a", req.getParameter("f250a"));
                            parts.put("100a", req.getParameter("f100a"));
                            parts.put("110a", req.getParameter("f110a"));
                            parts.put("111a", req.getParameter("f111a"));
                            parts.put("260a", req.getParameter("f260"));
    //                        parts[1] = req.getParameter("isbn");
                            //                        parts[2] = req.getParameter("issn");
                            //                        parts[2] = req.getParameter("ccnb");
                            //                        parts[3] = req.getParameter("titul");
                            //                        parts[4] = req.getParameter("f245n");
                            //                        parts[5] = req.getParameter("f245p");
                            //                        parts[6] = req.getParameter("f250a");
                            //                        parts[7] = req.getParameter("f100a");
                            //                        parts[8] = req.getParameter("f110a");
                            //                        parts[9] = req.getParameter("f111a");
                            //                        parts[10] = req.getParameter("f260");
                            //                        String line = "";
                            //                        for (String s : parts) {
                            //                            line += s + "\t";
                            //                        }

                            docCode = Slouceni.generateMD5(parts);
                        }
                        try {
                            conn = DbUtils.getConnection();
                            insertNabidka(conn, idKnihovna, id, docCode, idOffer, (new JSONObject(parts)).toString());

                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "upload failed", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        SAVEOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();

                        String name = req.getParameter("offerName");
                        String user = req.getRemoteUser();

                        /// Create a factory for disk-based file items
                        FileItemFactory factory = new DiskFileItemFactory();

                        // Create a new file upload handler
                        ServletFileUpload upload = new ServletFileUpload(factory);

                        // Parse the request
                        List /* FileItem */ items = upload.parseRequest(req);

                        Iterator iter = items.iterator();

                        while (iter.hasNext()) {
                            FileItem item = (FileItem) iter.next();

                            if (item.isFormField()) {
                                if (item.getFieldName().equals("offerName")) {
                                    name = item.getString();
                                }
                            }
                        }
                        iter = items.iterator();
                        while (iter.hasNext()) {
                            FileItem item = (FileItem) iter.next();
                            if (item.isFormField()) {
                                continue;
                            }
                            InputStream uploadedStream = item.getInputStream();
                            byte[] bytes = IOUtils.toByteArray(uploadedStream);
                            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

                            //uploadedStream.mark(uploadedStream.available());
                            Connection conn = null;

                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            int idKnihovna = 0;
                            if (kn != null) {
                                idKnihovna = kn.getId();
                            }
                            try {
                                conn = DbUtils.getConnection();
                                int idOffer = 0;
                                if (isOracle(conn)) {
                                    idOffer = insertOfferOracle(conn, name, idKnihovna, bais);
                                } else {
                                    idOffer = insertOfferPg(conn, name, idKnihovna, bais);
                                }
                                out.println("Nabidka " + name + " ulozena.");
                                bais.reset();
                                processStream(conn, bais, idKnihovna, idOffer);

                            } catch (Exception ex) {
                                LOGGER.log(Level.SEVERE, "upload failed", ex);
                                out.println(ex);
                            } finally {
                                if (conn != null && !conn.isClosed()) {
                                    conn.close();
                                }
                            }
                            uploadedStream.close();
                        }
                    }
                },
        DOWNLOADOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        //resp.setContentType("text/plain");
                        String id = req.getParameter("id");
                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();

                            String sql = "select * from OFFER where offer_id=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setInt(1, new Integer(id));
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                byte[] bytes = rs.getBytes("bdata");
                                resp.getOutputStream().write(bytes);
                            }

                        } catch (Exception ex) {
                            PrintWriter out = resp.getWriter();
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }

                    }
                },
        INFO {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        String id = req.getParameter("id");
                        Connection conn = null;
                        try {
                            PrintWriter out = resp.getWriter();
                            conn = DbUtils.getConnection();
                            DatabaseMetaData p = conn.getMetaData();
                            out.println(p.getDatabaseProductName());

                        } catch (Exception ex) {
                            PrintWriter out = resp.getWriter();
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }

                    }
                },
        GETOFFERS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        try {
                            PrintWriter out = resp.getWriter();
                            out.println(getOffers().toString());

                        } catch (Exception ex) {
                            PrintWriter out = resp.getWriter();
                            out.println(ex);
                        }

                    }
                },
        GETOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        try {
                            PrintWriter out = resp.getWriter();
                            String id = req.getParameter("id");
                            out.println(getOffer(id).toString());

                        } catch (Exception ex) {
                            PrintWriter out = resp.getWriter();
                            out.println(ex);
                        }

                    }
                };

        abstract void doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception;
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
