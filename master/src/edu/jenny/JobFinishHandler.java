package edu.utas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.utas.dao.JobDAO;
import edu.utas.util.Consts;
import edu.utas.util.PropertiesCache;
import edu.utas.util.Utilities;
import org.apache.log4j.Logger;
import org.openstack4j.model.compute.Server;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;


public class JobFinishHandler extends Thread {
    private final static Logger logger = Logger.getLogger(JobFinishHandler.class);

    private ConcurrentHashMap<String, String > jobFinishedMap = null;
    private HashMap<String, String> workerKey;
    private JobDAO jobDAO = null;
    private List<String[]> clientList = null;

    private void loadAuthenticationInfo()
    {
        List<String> stringList = (Arrays.asList(PropertiesCache.getInstance().getProperty("OS_CLIENT").split(Consts.SPLIT_COMMA)));
        for (String s : stringList) {
            clientList.add(s.split(Consts.SPLIT_SEMICOLON));
        }
    }

    public JobFinishHandler(ConcurrentHashMap<String, String > jobFinishedMap)
    {
        if( jobDAO  == null)
        {
            jobDAO = new JobDAO();
        }
        if (clientList==null)
        {
            clientList = new ArrayList<String[]>();
        }
        if (this.jobFinishedMap==null)
        {
            this.jobFinishedMap = jobFinishedMap;
        }
        if(workerKey == null)
        {
            workerKey = new HashMap<String, String>();
        }

        loadAuthenticationInfo();
        start();
    }
    @Override
    public void run()
    {
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateFinishedJobs();
        }
    }

    public String getServerIDByIP(String ip)
    {
        ArrayList<Server> activerWorkers = this.getActiveWorkers();
        String id = null;
        for(Server s : activerWorkers)
        {
            if(Utilities.getServerIPAddress(s).equals(ip))
            {
                id = s.getId();
            }
        }

        return id;
    }

    public ArrayList<Server> getActiveWorkers()
    {
        ArrayList<Server> allServers = Utilities.getWorkers(this.clientList);

        ArrayList<Server> activeServers = new ArrayList<Server>();
        for (Server s : allServers)
        {
            if(s.getStatus() == Server.Status.ACTIVE)
            {
                //Get active server and their key
                activeServers.add(s);
                workerKey.put(s.getId(), s.getKeyName());
            }
        }
        return activeServers;
    }


    public void getJobResults(String workip, String passcode)
    {
        try {
            String host = workip;
            String user = "ubuntu";

            String keyPath = PropertiesCache.getInstance().getProperty("SFTP_KEY");
            String sourcePath = PropertiesCache.getInstance().getProperty("WORKER_OUT");

            //First get server's id, then using hashmap to get its key
            String workid = this.getServerIDByIP(workip);
            String keyname = workerKey.get(workid) + ".ppk";
            String privateKey = keyPath + keyname ; //please provide your ppk file

            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            Properties config = new Properties();
            //session.setPassword("KIT418@utas"); ////if password is empty please comment it
            jsch.addIdentity(privateKey);
            System.out.println("identity added ");
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            //to get the file in the host
            String sourceFile = sourcePath + passcode + Consts.OUTPUT_FILENAME;

            //output file
            String outputFile = PropertiesCache.getInstance().getProperty("SFTP_SOURCE") + passcode + File.separator +
                    passcode + Consts.OUTPUT_FILENAME;
            //create file for output.

            Process process = Runtime.getRuntime().exec("touch " + outputFile);
            sftpChannel.get(sourceFile, outputFile);

            sftpChannel.exit();
            session.disconnect();
        }
        catch(Exception e){
            logger.error(e);
        }
    }

    public void updateDatabase(String code, Double d, Double c)
    {
        if (! Consts.JOB_PROCESSING.equals(jobDAO.getJobStatusByPasscode(code))) {
            jobDAO.updateFinishedJobsByCode(code, Consts.JOB_TERMINATED, d, c, code + Consts.OUTPUT_FILENAME);
        } else {
            jobDAO.updateFinishedJobsByCode(code, Consts.JOB_COMPLETED, d, c, code + Consts.OUTPUT_FILENAME);
        }
    }

    public void updateFinishedJobs()
    {
        Iterator<Map.Entry<String, String>> jobIterator = jobFinishedMap.entrySet().iterator();
        Map.Entry<String, String> pair = null;

        while(jobIterator.hasNext()) {
            pair = jobIterator.next();
            String code  = pair.getKey();
            String[] jobInfo = pair.getValue().split(Consts.SPLIT_COLON);
            logger.debug(code + "\t" + pair.getValue());

            String workid = jobInfo[0];
            if (Double.valueOf(jobInfo[1]) > 0) {
                Double d = new BigDecimal(jobInfo[1]).divide(new BigDecimal(1000), 2).setScale(2, RoundingMode.HALF_UP).doubleValue();
                Double c = new BigDecimal(d).multiply(new BigDecimal(PropertiesCache.getInstance().getProperty("JOB_COST"))).divide(new BigDecimal(60),2).setScale(2, RoundingMode.HALF_UP).doubleValue();

                //first update db with duration and cost using passcode
                this.updateDatabase(code, d, c);

                //then pull the output files from the worker.
                String workerip = Utilities.getServerIPByID(workid, this.clientList);
                this.getJobResults(workerip, code);
            } else {
                jobDAO.updateStatusByCode(workid, Consts.JOB_TERMINATED);
            }
            jobIterator.remove();
        }

    }
}
