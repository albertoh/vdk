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
package cz.incad.vdk.vdkcr_client.tools;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;


/**
 * Controls whether current session is authenticated
 * 
 * @author pavels
 */
public class LoggedController {

    protected HttpServletRequest req;

    public void configure(Map props) {
        req = (HttpServletRequest) props.get("request");
    }

    /**
     * Returns true if the current session is authenticated
     * @return
     */
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
    
    
}
