package cz.incad.vdk.client;

import cz.incad.vdkcommon.Slouceni;
import static cz.incad.vdkcommon.Slouceni.csvToMap;
import static cz.incad.vdkcommon.Slouceni.toJSON;
import cz.incad.vdkcommon.solr.IndexerQuery;
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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONException;

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

    public static int insertDemandPg(Connection conn, String name, int idKnihovna) throws Exception {
        int retVal = 0;
        String sql = "insert into DEMAND (nazev, knihovna, update_timestamp, closed) values (?,?, NOW(), false) returning demand_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, name);
        ps.setInt(2, idKnihovna);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            return 0;
        }

    }

    public static int closeDemand(Connection conn, int id) throws Exception {

        String sql = "update DEMAND set closed=? where demand_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setBoolean(1, true);
        ps.setInt(2, id);
        return ps.executeUpdate();
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

        String sql = "insert into OFFER (nazev, knihovna, update_timestamp, offer_id, closed) values (?,?, sysdate, ?, 0)";
        LOGGER.log(Level.INFO, "executing " + sql + "\nparams: {0}, {1}, {2}", new Object[]{name, idKnihovna, retVal});
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, name);
//        if (uploadedStream != null) {
//            ps.setBinaryStream(2, uploadedStream, uploadedStream.available());
//        } else {
//            ps.setNull(2, Types.BLOB);
//        }
        ps.setInt(2, idKnihovna);
        ps.setInt(3, retVal);
        ps.executeUpdate();
        return retVal;

    }

    public static int insertWantOffer(Connection conn, int zaznam_offer, int knihovna, boolean wanted) throws Exception {

        if (isOracle(conn)) {
            String sql1 = "select Wanted_ID_SQ.nextval from dual";
            ResultSet rs = conn.prepareStatement(sql1).executeQuery();
            int newid = 1;
            if (rs.next()) {
                newid = rs.getInt(1);
            }

            String sql = "insert into WANTED (ZaznamOffer, wants, knihovna, wanted_id, update_timestamp) values (?,?,?,?,sysdate)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, zaznam_offer);
            ps.setInt(2, knihovna);
            ps.setBoolean(3, wanted);
            ps.setInt(4, newid);
            ps.executeUpdate();
            return newid;
        } else {
            String sql = "insert into WANTED (ZaznamOffer, knihovna, wants,update_timestamp) values (?,?,?,NOW()) returning wanted_id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, zaznam_offer);
            ps.setInt(2, knihovna);
            ps.setBoolean(3, wanted);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public static int insertNabidka(Connection conn, int idKnihovna, String zaznam_id, String exemplar_id, String docCode, int idOffer, String line) throws Exception {

        if (isOracle(conn)) {
            String sql1 = "select ZaznamOffer_ID_SQ.nextval from dual";
            ResultSet rs = conn.prepareStatement(sql1).executeQuery();
            int idW = 1;
            if (rs.next()) {
                idW = rs.getInt(1);
            }

            String sql = "insert into ZaznamOffer (uniqueCode, zaznam, exemplar, knihovna, ZaznamOffer_id, offer, fields) values (?,?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, docCode);
            ps.setString(2, zaznam_id);
            if (exemplar_id == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, exemplar_id);
            }
            ps.setInt(4, idKnihovna);
            ps.setInt(5, idW);
            ps.setInt(6, idOffer);
            ps.setString(7, line);
            ps.executeUpdate();
            return idW;
        } else {
            String sql = "insert into ZaznamOffer (uniqueCode, zaznam, exemplar, knihovna,offer,fields) values (?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, docCode);
            ps.setString(2, zaznam_id);
            if (exemplar_id == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, exemplar_id);
            }
            ps.setInt(4, idKnihovna);
            ps.setInt(5, idOffer);
            ps.setString(6, line);
            int idW = ps.executeUpdate();
            return idW;
        }
        //indexWeOffer(conn, id, docCode, "md5");
    }

    public static void removeZaznamOffer(Connection conn,
            int idKnihovna,
            int ZaznamOffer_id) throws Exception {

        String sql = "delete from ZaznamOffer where ZaznamOffer_id=? and knihovna=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, ZaznamOffer_id);
        ps.setInt(2, idKnihovna);
        ps.executeUpdate();
    }

    public static void removeDemand(Connection conn,
            int idKnihovna,
            String zaznam_id,
            String exemplar_id,
            String docCode) throws Exception {

        String sql = "delete from ZaznamDemand where uniqueCode=? and zaznam=? and exemplar=? and knihovna=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, docCode);
        ps.setString(2, zaznam_id);
        if (exemplar_id == null) {
            ps.setNull(3, Types.VARCHAR);
        } else {
            ps.setString(3, exemplar_id);
        }
        ps.setInt(4, idKnihovna);
        ps.executeUpdate();

    }

    public static void insertToDemand(Connection conn,
            int idKnihovna,
            String zaznam_id,
            String exemplar_id,
            String docCode,
            String line) throws Exception {

        if (isOracle(conn)) {
            String sql1 = "select ZaznamDemand_ID_SQ.nextval from dual";
            ResultSet rs = conn.prepareStatement(sql1).executeQuery();
            int idW = 1;
            if (rs.next()) {
                idW = rs.getInt(1);
            }

            String sql = "insert into ZaznamDemand (uniqueCode, zaznam, exemplar, knihovna, ZaznamDemand_id, fields) values (?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, docCode);
            ps.setString(2, zaznam_id);
            if (exemplar_id == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, exemplar_id);
            }
            ps.setInt(4, idKnihovna);
            ps.setInt(5, idW);
            ps.setString(6, line);
            ps.executeUpdate();
        } else {
            String sql = "insert into ZaznamDemand (uniqueCode, zaznam, exemplar, knihovna,fields) values (?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, docCode);
            ps.setString(2, zaznam_id);
            if (exemplar_id == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, exemplar_id);
            }
            ps.setInt(4, idKnihovna);
            ps.setString(5, line);
            ps.executeUpdate();
        }
        //indexWeOffer(conn, id, docCode, "md5");
    }

    public static int insertDemandOracle(Connection conn, String name, int idKnihovna) throws Exception {
        int retVal = 1;
        String sql1 = "select Demand_ID_SQ.nextval from dual";
        ResultSet rs = conn.prepareStatement(sql1).executeQuery();
        if (rs.next()) {
            retVal = rs.getInt(1);
        }

        String sql = "insert into DEMAND (nazev, knihovna, demand_id, update_timestamp, closed) values (?,?, ?, sysdate, 0)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, name);
        ps.setInt(2, idKnihovna);
        ps.setInt(3, retVal);
        ps.executeUpdate();
        return retVal;

    }

    public static void processStream(Connection conn, InputStream is, int idKnihovna, int idOffer, JSONObject json) throws Exception {
        try {
            CSVStrategy strategy = new CSVStrategy('\t', '\"', '#');
            CSVParser parser = new CSVParser(new InputStreamReader(is), strategy);
            int lines = parser.getLineNumber();
            String[] parts = parser.getLine();
            while (parts != null) {
                String docCode;
                docCode = Slouceni.generateMD5(parts);
                insertNabidka(conn, idKnihovna, null, null, docCode, idOffer, Slouceni.toJSON(csvToMap(parts)).toString());
                parts = parser.getLine();
            }
            json.put("message", "imported " + lines + " lines to offer: " + idOffer);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new Exception("Not valid cvs file. Separator must be tabulator and line must be ", ex);
        }
    }

    private static JSONArray wantedJSON(ResultSet rs) throws Exception{
        JSONArray ja = new JSONArray();
        while (rs.next()) {
            JSONObject j = new JSONObject();
            j.put("wanted_id", rs.getInt("wanted_id"));
            j.put("zaznamoffer", rs.getInt("zaznamoffer"));
            j.put("wanted", rs.getBoolean("wants"));
            j.put("knihovna", rs.getString("code"));
            j.put("date", rs.getString("update_timestamp"));
            ja.put(j);
        }
        return ja;
    }
    
    private static JSONArray getWantedById(Connection conn, int ZaznamOffer_id) throws Exception{
        String sql = "select w.zaznamoffer, w.wanted_id, w.wants, w.update_timestamp, k.code from WANTED w, KNIHOVNA k, ZAZNAMOFFER zo "
                + "where zo.ZaznamOffer_id=? "
                + "and w.knihovna=k.knihovna_id and zo.zaznamoffer_id=w.zaznamoffer";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, ZaznamOffer_id);

        ResultSet rs = ps.executeQuery();
        JSONArray ja = wantedJSON(rs);
        rs.close();
        return ja;
    }
    
    private static JSONArray getWanted(Connection conn) throws Exception{
        String sql = "select w.zaznamoffer, w.wanted_id, w.wants, w.update_timestamp, k.code from WANTED w, KNIHOVNA k, ZAZNAMOFFER zo "
                + "where w.knihovna=k.knihovna_id and zo.zaznamoffer_id=w.zaznamoffer";
        PreparedStatement ps = conn.prepareStatement(sql);

        ResultSet rs = ps.executeQuery();
        JSONArray ja = wantedJSON(rs);
        rs.close();
        return ja;
    }
    
    private static JSONArray getLibraryWantedByCode(Connection conn, String docCode, int idKnihovna) throws Exception{
        String sql = "select w.zaznamoffer, w.wanted_id, w.wants, w.update_timestamp, k.code from WANTED w, KNIHOVNA k, ZAZNAMOFFER zo "
                + "where zo.uniquecode=? and w.knihovna=? "
                + "and w.knihovna=k.knihovna_id and zo.zaznamoffer_id=w.zaznamoffer";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, docCode);
        ps.setInt(2, idKnihovna);

        ResultSet rs = ps.executeQuery();
        JSONArray ja = wantedJSON(rs);
        rs.close();
        return ja;
    }
    
    private static JSONArray getLibraryWanted(Connection conn, int idKnihovna) throws Exception{
        String sql = "select w.zaznamoffer, w.wanted_id, w.wants, w.update_timestamp, k.code from WANTED w, KNIHOVNA k, ZAZNAMOFFER zo "
                + "where k.knihovna_id=? "
                + "and w.knihovna=k.knihovna_id and zo.zaznamoffer_id=w.zaznamoffer";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idKnihovna);

        ResultSet rs = ps.executeQuery();
        JSONArray ja = wantedJSON(rs);
        rs.close();
        return ja;
    }
    
    private static JSONArray getWantedByCode(Connection conn, String docCode) throws Exception{
        String sql = "select w.zaznamoffer, w.wanted_id, w.wants, w.update_timestamp, k.code from WANTED w, KNIHOVNA k, ZAZNAMOFFER zo "
                + "where zo.uniquecode=? "
                + "and w.knihovna=k.knihovna_id and zo.zaznamoffer_id=w.zaznamoffer";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, docCode);

        ResultSet rs = ps.executeQuery();
        JSONArray ja = wantedJSON(rs);
        rs.close();
        return ja;
    }
    
    private static JSONObject jsonZaznamOffer(int ZaznamOffer_id,
            String uniqueCode,
            String title,
            String zaznam,
            String exemplar,
            String knihovna,
            JSONObject fields) throws Exception {

        JSONObject j = new JSONObject();
        j.put("ZaznamOffer_id", ZaznamOffer_id);
        j.put("uniqueCode", uniqueCode);
        if (title == null) {
            SolrQuery query = new SolrQuery("code:" + uniqueCode);
            query.addField("title");
            SolrDocumentList docs = IndexerQuery.query(query);
            Iterator<SolrDocument> iter = docs.iterator();
            if (iter.hasNext()) {
                SolrDocument resultDoc = iter.next();
                j.put("title", resultDoc.getFirstValue("title"));
            }
        } else {
            j.put("title", title);
        }
        if (zaznam != null) {
            j.put("zaznam", zaznam);
        }

        j.put("exemplar", exemplar);
        j.put("knihovna", knihovna);
        j.put("fields", fields);
        return j;
    }

    public static JSONObject getOffer(String id) throws Exception {

        Connection conn = null;
        JSONObject json = new JSONObject();
        try {
            conn = DbUtils.getConnection();

            String sql = "select * from ZaznamOffer where offer=" + id;
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                int zoId = rs.getInt("ZaznamOffer_id");
                String zaznam = rs.getString("zaznam");
                JSONObject j = jsonZaznamOffer(zoId,
                        rs.getString("uniqueCode"),
                        null,
                        zaznam,
                        rs.getString("exemplar"),
                        rs.getString("knihovna"),
                        new JSONObject(rs.getString("fields")));

                j.put("wanted", getWantedById(conn, zoId));
                json.put(Integer.toString(zoId), j);
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

    public static JSONObject getDemands() throws Exception {

        Calendar now = Calendar.getInstance();
        Calendar o = Calendar.getInstance();
        Connection conn = null;
        JSONObject json = new JSONObject();
        try {
            conn = DbUtils.getConnection();
            String sql = "select DEMAND.*, KNIHOVNA.code from DEMAND, KNIHOVNA where DEMAND.knihovna=KNIHOVNA.knihovna_id";
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date offerDate = rs.getDate("update_timestamp");
                o.setTime(offerDate);
                o.add(Calendar.DATE, EXPIRATION_DAYS);
                JSONObject j = new JSONObject();
                j.put("id", rs.getString("demand_id"));
                j.put("nazev", rs.getString("nazev"));
                j.put("knihovna", rs.getString("code"));
                j.put("closed", rs.getBoolean("closed"));
                j.put("date", sdf.format(offerDate));
                j.put("expires", sdf.format(o.getTime()));
                j.put("expired", !o.after(now));

                json.put(rs.getString("demand_id"), j);
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

    private static JSONObject offerJSON(Date offerDate,
            String id,
            String nazev,
            String knihovna,
            boolean closed) throws JSONException {
        Calendar now = Calendar.getInstance();
        Calendar o = Calendar.getInstance();
        o.setTime(offerDate);
        o.add(Calendar.DATE, EXPIRATION_DAYS);
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("nazev", nazev);
        j.put("knihovna", knihovna);
        j.put("closed", closed);
        j.put("date", sdf.format(offerDate));
        j.put("expires", sdf.format(o.getTime()));
        j.put("expired", !o.after(now));
        
        return j;
    }

    public static JSONObject getOffers() throws Exception {

        Connection conn = null;
        JSONObject json = new JSONObject();
        try {
            conn = DbUtils.getConnection();
            String sql = "select OFFER.*, KNIHOVNA.code from OFFER, KNIHOVNA where OFFER.knihovna=KNIHOVNA.knihovna_id";
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date offerDate = rs.getDate("update_timestamp");
                JSONObject j = offerJSON(offerDate,
                        rs.getString("offer_id"),
                        rs.getString("nazev"),
                        rs.getString("code"),
                        rs.getBoolean("closed"));

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

    enum Actions {

        GETLIBRARYWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();
                        
                        Connection conn = null;

                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn != null) {
                                conn = DbUtils.getConnection();
                                JSONArray ja = getLibraryWanted(conn, kn.getId());
                                json.put(kn.getCode(), ja);
                            } else {
                                json.put("error", "nejste prihlasen");
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "get wanted failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },

        GETLIBRARYWANTEDBYCODE {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();
                        
                        Connection conn = null;

                        try {
                            Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            if (kn != null) {
                                conn = DbUtils.getConnection();
                                String code = req.getParameter("code");
                                JSONArray ja = getLibraryWantedByCode(conn, code, kn.getId());
                                json.put(code, ja);
                            } else {
                                json.put("error", "nejste prihlasen");
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "get wanted failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },

        GETWANTEDBYCODE {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();
                        
                        Connection conn = null;

                        try {
                            //Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                            //if (kn != null) {
                                conn = DbUtils.getConnection();
                                String code = req.getParameter("code");
                                JSONArray ja = getWantedByCode(conn, code);
                                json.put(code, ja);
//                            } else {
//                                json.put("error", "nejste prihlasen");
//                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "get wanted failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },

        GETWANTED {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        PrintWriter out = resp.getWriter();
                        
                        Connection conn = null;

                        try {
                                conn = DbUtils.getConnection();
                                JSONArray ja = getWanted(conn);
                                out.println(ja.toString());
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "get wanted failed", ex);
                            JSONObject json = new JSONObject();
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },

        WANTOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        int zaznam_offer = Integer.parseInt(req.getParameter("zaznam_offer"));
                        boolean wanted = Boolean.parseBoolean(req.getParameter("wanted"));

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");

                        try {
                            if (kn != null) {
                                conn = DbUtils.getConnection();
                                
                                int newid = insertWantOffer(conn, zaznam_offer, kn.getId(), wanted);
                                json.put("message", "Reakce pridana. Id: " + newid);
                                json.put("id",newid);
                            } else {
                                json.put("error", "nejste prihlasen");
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "want offer failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
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

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        } else {
                            out.println("Operation not allowed. Not logged.");
                            return;
                        }
                        try {
                            conn = DbUtils.getConnection();
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
                                ps.setInt(5, idPohled);
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
        CLOSEDEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        int id = Integer.parseInt(req.getParameter("id"));

                        Connection conn = null;
                        try {
                            conn = DbUtils.getConnection();
                            out.print(closeDemand(conn, id));

                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "action failed", ex);
                            out.println(ex);
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                    }
                },
        NEWDEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        String name = req.getParameter("name");
                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        }
                        try {
                            conn = DbUtils.getConnection();
                            int id = 0;
                            if (isOracle(conn)) {
                                id = insertDemandOracle(conn, name, idKnihovna);
                            } else {
                                id = insertDemandPg(conn, name, idKnihovna);
                            }

                            Calendar now = Calendar.getInstance();
                            Calendar o = Calendar.getInstance();
                            o.add(Calendar.DATE, EXPIRATION_DAYS);
                            JSONObject j = new JSONObject();
                            j.put("id", Integer.toString(id));
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
        NEWOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        String name = req.getParameter("offerName");
                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        try {
                            if (kn != null) {
                                idKnihovna = kn.getId();
                                conn = DbUtils.getConnection();
                                int idOffer = 0;
                                if (isOracle(conn)) {
                                    idOffer = insertOfferOracle(conn, name, idKnihovna, null);
                                } else {
                                    idOffer = insertOfferPg(conn, name, idKnihovna, null);
                                }

                                Calendar now = Calendar.getInstance();
                                JSONObject j = offerJSON(now.getTime(),
                                        Integer.toString(idOffer),
                                        name,
                                        kn.getCode(),
                                        false);

                                out.println(j.toString());
                            }

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
        IMPORTTOOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        /// Create a factory for disk-based file items
                        FileItemFactory factory = new DiskFileItemFactory();

                        // Create a new file upload handler
                        ServletFileUpload upload = new ServletFileUpload(factory);

                        // Parse the request
                        List /* FileItem */ items = upload.parseRequest(req);

                        Iterator iter = items.iterator();

                        int idOffer = 0;
                        while (iter.hasNext()) {
                            FileItem item = (FileItem) iter.next();

                            if (item.isFormField()) {
                                LOGGER.log(Level.INFO, "------ {0} param value : {1}", new Object[]{item.getFieldName(), item.getString()});
                                if (item.getFieldName().equals("id")) {
                                    idOffer = Integer.parseInt(item.getString());
                                }
                            }
                        }
                        if (idOffer == 0) {
                            json.put("error", "nabidka ne platna");
                        } else {
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
                                try {
                                    if (kn != null) {

                                        idKnihovna = kn.getId();
                                        conn = DbUtils.getConnection();
                                        processStream(conn, bais, idKnihovna, idOffer, json);

                                    } else {
                                        json.put("error", "nejste prihlasen");
                                    }
                                } catch (Exception ex) {
                                    LOGGER.log(Level.SEVERE, "import to offer failed", ex);
                                    json.put("error", ex.toString());
                                } finally {
                                    if (conn != null && !conn.isClosed()) {
                                        conn.close();
                                    }
                                }
                                uploadedStream.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },
        ADDFORMTOOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        int idOffer = Integer.parseInt(req.getParameter("id"));

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");

                        try {
                            if (kn != null) {
                                int idKnihovna = kn.getId();

                                conn = DbUtils.getConnection();
                                Map<String, String> parts = new HashMap<String, String>();
                                boolean exists = false;

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
                                parts.put("cena", req.getParameter("cena"));
                                parts.put("comment", req.getParameter("comment"));

                                String docCode = Slouceni.generateMD5(parts);
                                String sql = "select * from ZAZNAM where uniquecode='" + docCode + "'";
                                PreparedStatement ps = conn.prepareStatement(sql);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    exists = true;
                                }

                                JSONObject fields = new JSONObject(parts);
                                int newid = insertNabidka(conn, idKnihovna, null, null, docCode, idOffer, fields.toString());

                                json = jsonZaznamOffer(newid,
                                        docCode,
                                        req.getParameter("titul"),
                                        null,
                                        null,
                                        kn.getCode(),
                                        fields);
                                if (exists) {

                                    json.put("message", "Nabidka pridana. Generovany kod: " + docCode + " uz existuje");
                                } else {
                                    json.put("message", "Nabidka pridana. Kod: " + docCode);
                                }
                            } else {
                                json.put("error", "nejste prihlasen");
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "add to offer failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },
        ADDDOCTOOFFER {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        int idOffer = Integer.parseInt(req.getParameter("id"));
                        String zaznam_id = req.getParameter("zaznam");
                        String exemplar_id = req.getParameter("ex");
                        String docCode = req.getParameter("docCode");

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");

                        try {
                            if (kn != null) {
                                int idKnihovna = kn.getId();

                                conn = DbUtils.getConnection();
                                Map<String, String> parts = new HashMap<String, String>();
                                boolean exists = false;

                                parts.put("comment", req.getParameter("comment"));

                                JSONObject fields = new JSONObject(parts);
                                int newid = insertNabidka(conn, idKnihovna, zaznam_id, exemplar_id, docCode, idOffer, fields.toString());

                                json = jsonZaznamOffer(newid,
                                        docCode,
                                        req.getParameter("titul"),
                                        zaznam_id,
                                        exemplar_id,
                                        kn.getCode(),
                                        fields);
                                if (exists) {

                                    json.put("message", "Nabidka pridana. Generovany kod: " + docCode + " uz existuje");
                                } else {
                                    json.put("message", "Nabidka pridana. Kod: " + docCode);
                                }
                            } else {
                                json.put("error", "nejste prihlasen");
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "add to offer failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },
        ADDTODEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        String zaznam_id = req.getParameter("zaznam");
                        String exemplar_id = req.getParameter("ex");
                        String docCode = req.getParameter("docCode");

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        } else {
                            json.put("error", "Nejste prihlasen");
                            return;
                        }

                        try {
                            conn = DbUtils.getConnection();
                            Map<String, String> parts = new HashMap<String, String>();
                            boolean exists = false;
                            parts.put("comment", req.getParameter("comment"));

                            insertToDemand(conn, idKnihovna, zaznam_id, exemplar_id, docCode, (new JSONObject(parts)).toString());
                            if (exists) {
                                json.put("message", "Poptavka pridana. Generovany kod: " + docCode + " uz existuje");
                            } else {
                                json.put("message", "Poptavka pridana. Kod: " + docCode);
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "add to demand failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEZAZNAMOFFER {

                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        } else {
                            json.put("error", "Nejste prihlasen");
                            return;
                        }
                        try {
                            int ZaznamOffer_id = Integer.parseInt(req.getParameter("ZaznamOffer_id"));
                            conn = DbUtils.getConnection();
                            removeZaznamOffer(conn, idKnihovna, ZaznamOffer_id);
                            json.put("message", "Zaznam z nabidky odstranen");
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "remove offer failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
                    }
                },
        REMOVEDEMAND {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        JSONObject json = new JSONObject();
                        PrintWriter out = resp.getWriter();

                        String zaznam_id = req.getParameter("zaznam");
                        String exemplar_id = req.getParameter("ex");
                        String docCode = req.getParameter("docCode");

                        Connection conn = null;

                        Knihovna kn = (Knihovna) req.getSession().getAttribute("knihovna");
                        int idKnihovna = 0;
                        if (kn != null) {
                            idKnihovna = kn.getId();
                        } else {
                            json.put("error", "Nejste prihlasen");
                            return;
                        }

                        try {
                            conn = DbUtils.getConnection();
                            removeDemand(conn, idKnihovna, zaznam_id, exemplar_id, docCode);
                            json.put("message", "Poptavka odstranena");
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "remove demand failed", ex);
                            json.put("error", ex.toString());
                        } finally {
                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }
                        }
                        out.println(json.toString());
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
        GETDEMANDS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("text/plain");
                        try {
                            PrintWriter out = resp.getWriter();
                            out.println(getDemands().toString());

                        } catch (Exception ex) {
                            PrintWriter out = resp.getWriter();
                            out.println(ex);
                        }
                    }
                },
        GETOFFERS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.setContentType("application/json");
                        try {
                            PrintWriter out = resp.getWriter();
                            out.println(getOffers().toString());

                        } catch (Exception ex) {
                            PrintWriter out = resp.getWriter();
                            JSONObject json = new JSONObject();
                            json.put("error", ex.toString());
                            out.println(json.toString());
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
