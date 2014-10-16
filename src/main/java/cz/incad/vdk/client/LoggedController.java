/*
 * Copyright (C) 2013 Pavel Stastny
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
package cz.incad.vdk.client;

import cz.incad.vdk.client.tools.*;
import java.sql.SQLException;
import java.util.Map;
import javax.naming.NamingException;

import javax.servlet.http.HttpServletRequest;



public class LoggedController {

    protected HttpServletRequest req;
    public static String LOG_CONTROL_KEY = "logControl";

    public LoggedController(HttpServletRequest req) {
        this.req = req;
    }
    
    
    public static boolean isLogged(HttpServletRequest req) {
        LoggedController logControl =  (LoggedController) req.getSession().getAttribute(LoggedController.LOG_CONTROL_KEY);
        
        return logControl.isLogged();
    }
    
    
    public static Knihovna knihovna(HttpServletRequest req) {
        LoggedController logControl =  (LoggedController) req.getSession().getAttribute(LoggedController.LOG_CONTROL_KEY);
        if(logControl.isLogged()){
            return (Knihovna) req.getSession().getAttribute("knihovna");
        }else{
            return null;
        }
    }
    
    public boolean isLogged() {
        return req.getSession() != null
                && req.getRemoteUser() != null 
                && !req.getRemoteUser().equals("");
    }
    
    public String getLoggedName() {
        if (!this.isLogged()) return "Login"; 
        else { 
            return req.getRemoteUser();
        }
    }

    public String getUserJSONRepresentation() {
        if (!this.isLogged()) return "{}"; 
        else {
            return "{}";
        }
    }
    
    public void setKnihovna() throws NamingException, SQLException{
        cz.incad.vdk.client.Knihovna kn = new cz.incad.vdk.client.Knihovna(req.getRemoteUser());
        req.getSession().setAttribute("knihovna", kn);
    }
    
}
