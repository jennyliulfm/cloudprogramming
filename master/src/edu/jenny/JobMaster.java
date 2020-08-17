package edu.utas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.utas.dao.JobDAO;
import edu.utas.util.Consts;
import edu.utas.util.InstanceInfoComparator;
import edu.utas.util.PropertiesCache;
import edu.utas.util.Utilities;
import edu.utas.vo.InstanceInfo;
import edu.utas.vo.Job;
import org.apache.log4j.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.openstack.OSFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;

public class JobMaster extends Thread{

    private final static Logger logger = Logger.getLogger(JobMaster.class);

   private List<String[]> clientList = null;
   private HashMap<String, String> workerKey;
   private ConcurrentHashMap<Long, String> jobMap;
   private JobDAO jobDAO = null;
   private ConcurrentHashMap<String,ConcurrentHashMap<Long, String>> workerMapForSocket;
   private ConcurrentHashMap<String, InstanceInfo> instanceInfoMap;
   private int schedulingMethod;

    public JobMaster(int schedulingMethod, ConcurrentHashMap<String, ConcurrentHashMap<Long, String>> workerMapForSocket,
                     ConcurrentHashMap<String, InstanceInfo> instanceInfoMap)
    {
        if (clientList==null)
        {
            clientList = new ArrayList<>();
        }

        if(workerKey == null)
        {
            workerKey = new HashMap<>();
        }

        if (jobDAO == null) {
            jobDAO = new JobDAO();
        }

        if(jobMap == null)
        {
            jobMap = new ConcurrentHashMap<>();
        }

        if (workerMapForSocket == null)
        {
            workerMapForSocket = new ConcurrentHashMap<>();
        }
        //load Authentication Information from the config file.
        loadAuthenticationInfo();

        this.schedulingMethod = schedulingMethod;
        this.workerMapForSocket = workerMapForSocket;
        this.instanceInfoMap = instanceInfoMap;
        start();
    }

    @Override
    public void run()
    {
        while(true)
        {
            for (int i = 1; i <= 4; i++) {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (i == 2) {
                    resumeInstance();
                    suspendIdleInstances();
                }
                cancelJobs();
            }
            getNewJobs();
            migrateJobs();
            dispatchJobs();
        }
    }
    //This one is used to push files from master to worker.
    public boolean transferFile(String filePath, String passcode, String sourcefile, String key, String workerid, String path) {
        try {
            //String host = "115.146.86.143";

            String host = workerid;
            String user = "ubuntu";
            //String privateKey = "P:/KIT418/key/jennykey.ppk"; // please provide your ppk file
            String privateKey = key;

            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            Properties config = new Properties();
            jsch.addIdentity(privateKey);
//            System.out.println("identity added ");
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            logger.debug(workerid + "\t" + host +"\t" + privateKey);
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            String fileName = filePath + passcode +"/" + sourcefile;

            //then create a new folder using passcode
            String command = "mkdir -p " + path + File.separator +passcode;
            this.runCommand(host,key,command);
            logger.debug(host + "\t" + command);
            String destFile = path + passcode + File.separator + sourcefile;


            sftpChannel.put(new FileInputStream(fileName), destFile );
            logger.debug("put\t" + host + "\t" + destFile);

            sftpChannel.exit();
            session.disconnect();

        } catch (Exception e) {
            logger.error(e);
        }
        return true;
    }

    //Run command for worker.
    public void runCommand(String host, String privateKey, String command) {

        try {

            String user = "ubuntu";
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            Properties config = new Properties();
            jsch.addIdentity(privateKey);
            logger.debug(host +"\t" + privateKey);
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            //channel.setInputStream(null);
            //((ChannelExec)channel).setErrStream(System.err);
            channel.setInputStream(System.in);
            channel.connect();

            InputStream input = channel.getInputStream();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(input));
                String line;
                while ((line = br.readLine()) != null) {
                    logger.info(line);
                }

            } catch (IOException io) {
                logger.error(io);

            } catch (Exception e) {
                logger.error(e);

            }
        }catch(Exception e){
            logger.error(e);
        }
    }

    //Get worker's for the client.
    public ArrayList<Server> getServersByClient(OSClientV3 os)
    {
        return new ArrayList<Server>( os.compute().servers().list());
    }

    //thiso ne is used to get server by client and server name
    public Server getServerByName(OSClientV3 os,String name)
    {
        List<? extends Server> servers = os.compute().servers().list();

        for(int i=0; i<servers.size();i++)
        {
            Server server = servers.get(i);
            if(server.getName().equals(name))
            {
                return server;
            }
        }
        return null;
    }

    public ArrayList<Server> getActiveWorkers()
    {
        ArrayList<Server> allServers = Utilities.getWorkers(this.clientList);

        ArrayList<Server> activeServers = new ArrayList<Server>();
        for (Server s : allServers)
        {
            if(this.getWorkStatus(s) == Server.Status.ACTIVE)
            {
                //Get active server and their key
                activeServers.add(s);
                if (!instanceInfoMap.containsKey(s.getId())) {
                    InstanceInfo info = new InstanceInfo();
                    info.setInstanceId(s.getId());
                    instanceInfoMap.put(s.getId(), info);
                }
                workerKey.put(s.getId(), s.getKeyName());
            }
        }
        return activeServers;
    }

    public Server.Status getWorkStatus(Server s) {
        return s.getStatus();

    }

   private void loadAuthenticationInfo()
    {
        List<String> stringList = (Arrays.asList(PropertiesCache.getInstance().getProperty("OS_CLIENT").split(Consts.SPLIT_COMMA)));
        for (String s : stringList) {
            clientList.add(s.split(Consts.SPLIT_SEMICOLON));
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

    public Long getJobPriority(Job j)
    {
        return j.getCreated().getTime() + j.getExpectation() * 1000 * 60;
    }

    public void  getNewJobs()
    {
        String jobStatus = Consts.JOB_INIT;
        ArrayList<Job> newJobs = jobDAO.getJobsByStatus(jobStatus);

        for(Job j : newJobs)
        {
            Long key = this.getJobPriority(j) + j.getId();
            String value = j.getId() + Consts.SPLIT_COLON + j.getPasscode() + Consts.SPLIT_COLON +j.getFilename()+ Consts.SPLIT_COLON+ j.getInput();
            logger.debug("new job\t"+ key + "\t" + value);
            jobMap.put(key, value);
        }
    }

    public void dispatchJobs()
    {
        List<Long> jobByPriority = new ArrayList<Long>(jobMap.keySet());
        logger.info("To do jobs\t" + jobByPriority.size());
        Collections.sort(jobByPriority);

        int index = 0;
        for (Long jobMapKey : jobByPriority)
        {
            if (!jobMap.containsKey(jobMapKey)) {
                continue;
            }
            String jobMapValue = jobMap.get(jobMapKey);
            String[] jobInfo = jobMapValue.split(Consts.SPLIT_COLON);

            ArrayList<Server> workerList = this.getActiveWorkers();
            logger.info("Active worker\t" + workerList.size());
            Server worker = workerList.get(0);
            if (this.schedulingMethod == Consts.SCHEDULING_ROUND_ROBIN) {
                int workerCount = workerList.size();
                worker = workerList.get(index % workerCount);
            } else {
                List<InstanceInfo> instances = new ArrayList<>(instanceInfoMap.values());
                Collections.sort(instances, new InstanceInfoComparator());
                outer: for (InstanceInfo info : instances) {
                    for (Server s : workerList) {
                        if (info.getInstanceId().equals(s.getId())) {
                            worker = s;
                            break outer;
                        }
                    }
                }
            }


            String jobpath = PropertiesCache.getInstance().getProperty("SFTP_SOURCE");
            int jobID =  Integer.parseInt(jobInfo[0]);
            String passcode = jobInfo[1];
            String filename = jobInfo[2];
            String input = "";
            if(jobInfo.length == 4)
            {
                input = jobInfo[3];
            }

            String key = workerKey.get(worker.getId());
            String keypath = PropertiesCache.getInstance().getProperty("SFTP_KEY") + key + ".ppk";

            String workerip = Utilities.getServerIPAddress(worker);
            System.out.println("worker ip:" + workerip);
            System.out.println("key" + keypath);

            transferFile(jobpath,passcode,filename,keypath,workerip,PropertiesCache.getInstance().getProperty("SFTP_DEST"));

            //dispatch input file
            if(!input.isEmpty())
            {
                transferFile(jobpath,passcode,input,keypath,workerip,PropertiesCache.getInstance().getProperty("SFTP_DEST"));
            }


            //Tell socket which worker has to start which job, save it in hashmap with two layers.
//            String v = Consts.PREFIX_RUN + Consts.SPLIT_COLON + jobMapValue;
            String v = Consts.PREFIX_RUN + jobMapValue.substring(jobMapValue.indexOf(Consts.SPLIT_COLON));
            if (workerMapForSocket.containsKey(worker.getId())) {
                workerMapForSocket.get(worker.getId()).put(jobMapKey, v);
            } else {
                ConcurrentHashMap<Long, String > tmp = new ConcurrentHashMap<Long, String >();
                tmp.put(jobMapKey, v);
                workerMapForSocket.put(worker.getId(), tmp);
            }

            //update job status in the DB
            jobDAO.updateJobStatusByID(jobID,Consts.JOB_PROCESSING,worker.getId());

            //remove job from hashmap
            jobMap.remove(jobMapKey);

            System.out.println("finished job dispatching");
            index++;
        }

    }

    public ArrayList<OSClientV3> getAllClients()
    {

        ArrayList<OSClientV3> clientsList = new ArrayList<OSClientV3>();
        for (String[] info : this.clientList) {
            clientsList.add(
                    OSFactory.builderV3()
                    .endpoint(PropertiesCache.getInstance().getProperty("OS_ENDPOINT"))
                    .credentials(info[0], info[1], Identifier.byName("Default"))
                    .scopeToProject(Identifier.byId(info[2]))
                    .authenticate());
        }
        return clientsList;
    }

    public ArrayList<OSClientV3> getAvaliableClient()
    {
        ArrayList<OSClientV3> allcients = getAllClients();
        ArrayList<OSClientV3> freeClients = new ArrayList<OSClientV3>();
        for(OSClientV3 os :allcients)
        {
            ArrayList<Server> servers  = this.getServersByClient(os);
            if(servers.size()<2)
            {
                freeClients.add(os);
            }
        }

        return freeClients;
    }

     public String getImageIDBySnapshotName(String spname)
    {
        List<String> stringList = (Arrays.asList(PropertiesCache.getInstance().getProperty("OS_SNAPSHOT").split(Consts.SPLIT_COMMA)));
        for (String s : stringList) {
           String[] snapInfo = s.split(Consts.SPLIT_SEMICOLON);
                   if(snapInfo[0].equals(spname))
                       return snapInfo[1];
        }
        return null;
    }

     public String[] getClientInfoByImageID(String name)
    {
        for(String[] info : this.clientList)
        {
            if(info[0].equals(name))
            {
                return info;
            }
        }
        return null;
    }


     public void createServerBySnapShot(String clientname, String imageName, String flavorID,String key) {

        String[] clientInfo = this.getClientInfoByImageID(imageName);

        OSClientV3 os = OSFactory.builderV3()
                .endpoint(PropertiesCache.getInstance().getProperty("OS_ENDPOINT"))
                .credentials(clientInfo[0], clientInfo[1], Identifier.byName("Default"))
                .scopeToProject(Identifier.byId(clientInfo[2]))
                .authenticate();
        String imgid = this.getImageIDBySnapshotName(imageName);

        ServerCreate server = Builders.server().name(clientname).flavor(flavorID).image(imgid).keypairName(key).build();
        os.compute().servers().boot(server);
    }


    public ArrayList<Server> getInactiveWorker()
    {
        ArrayList<Server> servers = Utilities.getWorkers(this.clientList);
        ArrayList<Server> inactiveWorkers = new ArrayList<Server>();
        for(Server s : servers)
        {
            if(this.getWorkStatus(s)!=Server.Status.ACTIVE)
            {
                inactiveWorkers.add(s);
            }
        }
        return inactiveWorkers;
    }

    public void setMigratedJobs(String workerid, String status)
    {
        for(Job j : jobDAO.getMigratedJobs(workerid, status))
        {
            Long key = this.getJobPriority(j) + j.getId();
            String value = j.getId() + Consts.SPLIT_COLON + j.getPasscode() + Consts.SPLIT_COLON +j.getFilename()+ Consts.SPLIT_COLON+ j.getInput();
            logger.debug("migrate job\t" + workerid + "\t" + key + "\t" + value);
            jobMap.put(key, value);
        }
    }

    public void migrateJobs()
    {
        ArrayList<Server> inactiveWorkers = this.getInactiveWorker();
        for(Server s : inactiveWorkers)
        {
            this.setMigratedJobs(s.getId(),Consts.JOB_PROCESSING);
        }
    }

    public HashMap<String,String> getWorkerKey()
    {
        return workerKey;
    }

    public void cancelJobs() {
        for(Job job : jobDAO.getJobsByStatus(Consts.JOB_CANCELLING)) {
            logger.debug("cancel job\t" + job.getPasscode() + "\t" + job.getWorker());
            if (job.getWorker() == null) {
                jobMap.remove(getJobPriority(job) + job.getId());
                jobDAO.updateJobStatusByID(job.getId(), Consts.JOB_TERMINATED);
            } else {
                String v = Consts.PREFIX_CANCEL + Consts.SPLIT_COLON + job.getPasscode();
                if (workerMapForSocket.containsKey(job.getWorker())) {
                    workerMapForSocket.get(job.getWorker()).put(job.getId().longValue(), v);
                } else {
                    ConcurrentHashMap<Long, String> tmp = new ConcurrentHashMap<Long, String>();
                    tmp.put(job.getId().longValue(), v);
                    workerMapForSocket.put(job.getWorker(), tmp);
                }
                jobDAO.updateJobStatusByID(job.getId(), Consts.JOB_KILLING);
            }
        }
    }

    public void resumeInstance() {
        int count = 0;
        for (Server s : this.getActiveWorkers()) {
            if (instanceInfoMap.get(s.getId()).getIdlePercentage() > 40) {
                count++;
            }
        }

        if (count == 0) {
            for (Server s : Utilities.getWorkers(this.clientList)) {
                if(this.getWorkStatus(s) == Server.Status.SUSPENDED)
                {
                    logger.info(s.getId() + "\t" + Action.RESUME);
                    setServerAction(s.getId(), Action.RESUME);
                }
            }
        }
    }

    public void suspendIdleInstances() {
        ArrayList<String> idleInstances = new ArrayList<>();
        for (String id : instanceInfoMap.keySet()) {
            if (instanceInfoMap.get(id).isKeptIdle()) {
                idleInstances.add(id);
            }
        }

        if (!idleInstances.isEmpty()) {
            int i = this.getActiveWorkers().size();
            for (String id : idleInstances) {
                if (i <= 2) {
                    break;
                }
                logger.info(id + "\t" + Action.SUSPEND);
                setServerAction(id, Action.SUSPEND);
                i--;
            }
        }
    }

    public String[] getClientInfoByInstanceId(String instanceId) {
        String tenantId = null;
        ArrayList<Server> servers = Utilities.getWorkers(this.clientList);
        for(Server s : servers)
        {
            if(s.getId().equals(instanceId))
            {
                tenantId = s.getTenantId();
            }
        }

        for(String[] info : this.clientList)
        {
            if(info[2].equals(tenantId))
            {
                return info;
            }
        }
        return null;
    }

    public void setServerAction(String serverID, Action action)
    {
        String[] clientInfo = this.getClientInfoByInstanceId(serverID);

        if(clientInfo != null)
        {
            OSClientV3 os = OSFactory.builderV3()
                    .endpoint(PropertiesCache.getInstance().getProperty("OS_ENDPOINT"))
                    .credentials(clientInfo[0], clientInfo[1], Identifier.byName("Default"))
                    .scopeToProject(Identifier.byId(clientInfo[2]))
                    .authenticate();

            if(os!=null)
            {

                instanceInfoMap.get(serverID).setKeptIdle(Boolean.FALSE);
                os.compute().servers().action(serverID, action);
            }

        }
    }
}
