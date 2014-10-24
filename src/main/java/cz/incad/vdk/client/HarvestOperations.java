/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.vdk.client;

import cz.incad.vdkcommon.DbUtils;
import cz.incad.vdkcommon.oai.OAIHarvester;
import java.io.IOException;
import java.io.PrintWriter;
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
public class HarvestOperations extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(HarvestOperations.class.getName());
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
                Actions actionToDo = HarvestOperations.Actions.valueOf(actionNameParam);
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

    enum Actions {
        DISKTODB {
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
                                    OAIHarvester oh = new OAIHarvester(req.getParameter("conf"));
                                    oh.setFromDisk(true);
                                    oh.setSklizen(Integer.parseInt(req.getParameter("sklizen")));
                                    if(req.getParameter("path") != null){
                                        oh.setPathToData(req.getParameter("path"));
                                    }
                                    if(req.getParameter("full") != null){
                                        oh.setFullIndex(true);
                                    }
                                    oh.harvest();

                                    json.put("message", "harvest finished.");
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                            json.put("error", ex.toString());
                        }

                        out.println(json.toString());
                    }
                },

        FULL {
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
                                    OAIHarvester oh = new OAIHarvester(req.getParameter("conf"));
                                    oh.setSaveToDisk(true);
                                    oh.setFullIndex(true);
                                    oh.harvest();

                                    json.put("message", "harvest finished.");
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                            json.put("error", ex.toString());
                        }

                        out.println(json.toString());
                    }
                },

        RESUMPTION {
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
                                    OAIHarvester oh = new OAIHarvester(req.getParameter("conf"));
                                    oh.setSaveToDisk(true);
                                    oh.setResumptionToken(req.getParameter("token"));
                                    oh.harvest();

                                    json.put("message", "harvest finished.");
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                            json.put("error", ex.toString());
                        }

                        out.println(json.toString());
                    }
                },
        UPDATE {
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
                                    OAIHarvester oh = new OAIHarvester(req.getParameter("conf"));
                                    oh.setSaveToDisk(true);
                                    oh.harvest();

                                    json.put("message", "harvest finished.");
                                } else {
                                    json.put("error", "rights.insuficient");
                                }
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
