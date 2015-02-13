/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.vdk.client;

import static cz.incad.vdk.client.DbOperations.LOGGER;
import cz.incad.vdkcommon.DbUtils;
import cz.incad.vdkcommon.VDKScheduler;
import cz.incad.vdkcommon.oai.HarvesterJob;
import cz.incad.vdkcommon.oai.HarvesterJobData;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.core.jmx.JobDataMapSupport;

/**
 *
 * @author alberto
 */
public class InitServlet extends HttpServlet {

    Scheduler sched;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }

    @Override
    public void init() throws ServletException {
        try {
            sched = VDKScheduler.getInstance().getScheduler();
            setCron();
            sched.start();
        } catch (SQLException ex) {
            Logger.getLogger(InitServlet.class.getName()).log(Level.SEVERE, null, ex);
        }catch (SchedulerException ex) {
            Logger.getLogger(InitServlet.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    private void setCron() throws SQLException, SchedulerException{
        Connection conn = null;
            
            try {
                conn = DbUtils.getConnection();

                String sql = "select * from Zdroj";
                PreparedStatement ps = conn.prepareStatement(sql);

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    
                    String cronVal = rs.getString("cron");
                    if (cronVal != null) {
                        addJob(rs.getString("nazev"), 
                                cronVal, 
                                rs.getString("parametry"));
                    }

                    
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } finally {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
            
            
    }
    
    private void addJob(String name, String cronVal, 
            String conf) throws SchedulerException, Exception {

        
        Map<String, Object> map = new HashMap<String, Object>();
        HarvesterJobData jobdata = new HarvesterJobData(name, conf);
        map.put("jobdata", jobdata);
        JobDataMap data = JobDataMapSupport.newJobDataMap(map);

        JobDetail job = JobBuilder.newJob(HarvesterJob.class)
                .withIdentity("job_" + name, "Zdroj")
                .setJobData(data)
                .build();
        if (sched.checkExists(job.getKey())) {
            sched.deleteJob(job.getKey());
        }
        if(cronVal.equals("")){
            LOGGER.log(Level.INFO, "Cron for {0} cleared ", name);
        }else{
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_" + name, "Zdroj")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronVal))
                    .build();
            sched.scheduleJob(job, trigger);
            LOGGER.log(Level.INFO, "Cron for {0} scheduled with {1}", new Object[]{name, cronVal});
        }
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
