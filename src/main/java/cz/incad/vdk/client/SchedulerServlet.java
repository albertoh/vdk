/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.vdk.client;

import cz.incad.vdkcommon.VDKScheduler;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

/**
 *
 * @author alberto
 */
public class SchedulerServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(SchedulerServlet.class.getName());
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

    enum Actions {

        STARTJOB {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        try {
                            PrintWriter out = resp.getWriter();
                            JSONObject json = new JSONObject();
                            Scheduler scheduler = VDKScheduler.getInstance().getScheduler();
                            String[] key = req.getParameter("key").split("\\.");
                            scheduler.triggerJob(new JobKey(key[1],key[0]));
                            json.put("message", "Job started");
                            out.println(json.toString());
                        } catch (SchedulerException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            PrintWriter out = resp.getWriter();
                            JSONObject json = new JSONObject();
                            json.put("error", ex.toString());
                            out.println(json.toString());
                        }
                    }
                },

        STOPJOB {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        try {
                            PrintWriter out = resp.getWriter();
                            JSONObject json = new JSONObject();
                            Scheduler scheduler = VDKScheduler.getInstance().getScheduler();
                            String[] key = req.getParameter("key").split("\\.");
                            scheduler.interrupt(new JobKey(key[1],key[0]));
                            json.put("message", "Job stopped");
                            out.println(json.toString());
                        } catch (SchedulerException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            PrintWriter out = resp.getWriter();
                            JSONObject json = new JSONObject();
                            json.put("error", ex.toString());
                            out.println(json.toString());
                        }
                    }
                },
        GETJOBS {
                    @Override
                    void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                        resp.setContentType("application/json");
                        try {
                            PrintWriter out = resp.getWriter();
                            JSONObject ret = new JSONObject();
                            Scheduler scheduler = VDKScheduler.getInstance().getScheduler();

                            for (String groupName : scheduler.getJobGroupNames()) {
                                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
                                        .jobGroupEquals(groupName))) {

                                    JSONObject json = new JSONObject();
                                    json.put("jobKey", jobKey);

                                    JobDetail jd = scheduler.getJobDetail(jobKey);

                                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                                    json.put("nextFireTime", triggers.get(0).getNextFireTime());
                                    json.put("state", "waiting");
                                    ret.put(jobKey.toString(), json);
                                }
                            }
                            
                            for (JobExecutionContext jec : scheduler.getCurrentlyExecutingJobs()) {
                                String jobKey = jec.getJobDetail().getKey().toString();
                                JSONObject json = ret.getJSONObject(jobKey);
                                json.put("fireTime", jec.getFireTime());
                                json.put("state", "running");
                                
                            }
                            
                            out.println(ret.toString());
                        } catch (SchedulerException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            PrintWriter out = resp.getWriter();
                            JSONObject json = new JSONObject();
                            json.put("error", ex.toString());
                            out.println(json.toString());
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
