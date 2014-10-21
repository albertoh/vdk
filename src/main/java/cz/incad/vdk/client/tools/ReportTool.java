
package cz.incad.vdk.client.tools;

import cz.incad.vdk.client.DbOperations;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ReportTool {
private static final Logger logger = Logger.getLogger(ReportTool.class.getName());
    protected HttpServletRequest req;
    private String type;

    public void configure(Map props) {
        req = (HttpServletRequest) props.get("request");
        type = req.getParameter("type");
    }
    
    public JSONObject getOffer(String id){
        try{
            return DbOperations.getOffer(Integer.parseInt(id)).getJSONObject(id);
        }catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public JSONObject getOfferContent(String id){
        try{
            return DbOperations.getOfferRows(id);
        }catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
}
