
package cz.incad.vdk.client;

import cz.incad.vdkcommon.DbUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Knihovna {
    private int id;
    private String code;
    private String nazev;
     private String heslo;
     private ArrayList<String> roles;
     private int priorita;
     private String email;
     private String telefon;
     
    public Knihovna(String code) throws NamingException, SQLException{
        this.code = code;
        Connection conn = getConnection();
        String sql = "select * from KNIHOVNA where code='" + code + "'";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            this.id = rs.getInt("knihovna_id");
            this.nazev = rs.getString("nazev");
            this.roles = getRoles(conn, code);
            this.priorita = rs.getInt("priorita");
            this.telefon = rs.getString("telefon");
            this.email = rs.getString("email");
        }
    }
    
    private ArrayList<String> getRoles(Connection conn, String code) throws SQLException{
        ArrayList<String> r = new ArrayList<String>();
        String sql = "select name from ROLE where code='" + code + "'";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            r.add(rs.getString("name"));
        }
        return r;
    }
    
    private Connection getConnection() throws NamingException, SQLException {
        Context initContext = new InitialContext();
        Context envContext = (Context) initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource) envContext.lookup("jdbc/vdk");
        return ds.getConnection();
    }
    
    public JSONObject getJson() throws JSONException{
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("name", nazev);
        j.put("code", code);
        j.put("priorita", priorita);
        j.put("roles", roles);
        j.put("telefon", telefon);
        j.put("email", email);
        return j;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the nazev
     */
    public String getNazev() {
        return nazev;
    }

    /**
     * @param nazev the nazev to set
     */
    public void setNazev(String nazev) {
        this.nazev = nazev;
    }

    /**
     * @return the heslo
     */
    public String getHeslo() {
        return heslo;
    }

    /**
     * @param heslo the heslo to set
     */
    public void setHeslo(String heslo) {
        this.heslo = heslo;
    }

    /**
     * @return the roles
     */
    public ArrayList<String> getRoles() {
        return roles;
    }
    
    public boolean hasRole(DbUtils.Roles role){
        return roles.contains(role.toString());
    }
    
    public boolean isSourceLib(){
        return hasRole(DbUtils.Roles.SOURCELIB);
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the telefon
     */
    public String getTelefon() {
        return telefon;
    }

    /**
     * @param telefon the telefon to set
     */
    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the priorita
     */
    public int getPriorita() {
        return priorita;
    }

    /**
     * @param priorita the priorita to set
     */
    public void setPriorita(int priorita) {
        this.priorita = priorita;
    }
    
}
