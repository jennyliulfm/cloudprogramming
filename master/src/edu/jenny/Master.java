package edu.utas;

import edu.utas.util.Consts;
import edu.utas.vo.InstanceInfo;
import org.apache.log4j.Logger;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Master extends Thread{
	public static final int PORT = 8085;

	private ConcurrentHashMap<String, ConcurrentHashMap<Long, String>> workerMapForSocket;
	private ConcurrentHashMap<String, String > jobFinishedMap;
	private ConcurrentHashMap<String, InstanceInfo> instanceInfoMap;

	public Master(ConcurrentHashMap<String,ConcurrentHashMap<Long, String>> workerMapForSocket,
				  ConcurrentHashMap<String, String > jobFinishedMap,
				  ConcurrentHashMap<String, InstanceInfo> instanceInfoMap)
	{
		this.workerMapForSocket = workerMapForSocket;
		this.jobFinishedMap = jobFinishedMap;
		this.instanceInfoMap = instanceInfoMap;
		start();
	}

	public void run()
	{
		//Establish socket server
		try {
			ServerSocket ss = new ServerSocket(PORT);
				try 
				{
					//allow unlimited number of client to connect to the server.
					while(true)
					{
						Socket socket = ss.accept();
						EachInstance ei = new EachInstance(socket, workerMapForSocket, jobFinishedMap, instanceInfoMap);
					}
				} finally
				{
					ss.close();
				}	  
			}
		catch(Exception e)
			{
				System.out.println("Could not establish the socket");
			}	
	}
}

class EachInstance extends Thread
{
    private final static Logger logger = Logger.getLogger(EachInstance.class);
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String workerID;
	private ConcurrentHashMap<String, ConcurrentHashMap<Long, String>> workerMapForSocket;
	private ConcurrentHashMap<String, String > jobFinishedMap;
	private ConcurrentHashMap<String, InstanceInfo> instanceInfoMap;

	public EachInstance(Socket ss, ConcurrentHashMap<String,ConcurrentHashMap<Long, String>> workerMapForSocket,
						ConcurrentHashMap<String, String> jobFinishedMap,
						ConcurrentHashMap<String, InstanceInfo> instanceInfoMap) throws IOException
	{
		this.workerMapForSocket = workerMapForSocket;
		this.jobFinishedMap = jobFinishedMap;
		this.instanceInfoMap = instanceInfoMap;

		socket = ss;
	     in = new BufferedReader(
	                 new InputStreamReader(
	                     socket.getInputStream()));
         // Enable auto-flush:
         out = new PrintWriter(
                 new BufferedWriter(
                     new OutputStreamWriter(
                         socket.getOutputStream())), true);
         start(); // Calls run()
	}
	
	 public void run() {
	     try {
			 boolean init = true;
			 while (init) {
				 try {
					 sleep(1);
				 } catch (InterruptedException e) {
					 e.printStackTrace();
				 }
				 while (in.ready()) {
					 while (workerID == null) {
						 workerID = in.readLine();
					 }
				 }
				 if (workerID != null) {
					 init = false;
				 }
			 }
             logger.info(socket.getInetAddress() + "\t" + workerID);

	    	 SendOutput sop = new SendOutput(out, workerID, workerMapForSocket);
             String str = "";
	         while (true) 
	         {
				 try {
					 sleep(1);
				 } catch (InterruptedException e) {
					 e.printStackTrace();
				 }
				 while (in.ready()) {
					 str = in.readLine();
					 if (str != null && !str.trim().isEmpty()) {
//					 	System.out.println("<<<\t"+ workerID + "\t" + str);
						 String[] res = str.split(Consts.SPLIT_COLON);
						 if (str.startsWith(Consts.PREFIX_RESULT)) {
                             logger.debug("<<<\t"+ workerID + "\t" + str);
						 	//RESULT#:1570609250288v1AOGGR:1.234
							 if (res.length == 2) {
							 	//Cancel job
								 this.jobFinishedMap.put(res[1], workerID + Consts.SPLIT_COLON + -1);
							 } else {
								 // PREFIX_RESULT : Passcode : Duration
								 this.jobFinishedMap.put(res[1], workerID + Consts.SPLIT_COLON + res[2]);
							 }
						 }
						 if (str.startsWith(Consts.PREFIX_INFO)) {
							 InstanceInfo info;
							 if (instanceInfoMap.containsKey(workerID)) {
								info = instanceInfoMap.get(workerID);
							 } else {
							 	info = new InstanceInfo();
							 	info.setInstanceId(workerID);
							 }
							 if (res.length == 3) {
							 	//INFO#:12:83.67
								 // PREFIX_INFO : Processes : CPU
							 	info.setTotalTasks(Integer.parseInt(res[1]));
							 	info.setIdlePercentage(Double.parseDouble(res[2]));
							 } else {
							 	//INFO#:300
								 // PREFIX_INFO : Seconds
							 	info.setKeptIdle(Boolean.TRUE);
							 }
							 instanceInfoMap.put(workerID, info);
						 }

					 }
				 }
	         }
	     } catch (IOException e) {
	         logger.error(e);
	     } finally {
	         try {
	             socket.close();
	         } catch(IOException e) {
                 logger.error(e);
             }
	     }
	 }
}
class SendOutput extends Thread
{
    private final static Logger logger = Logger.getLogger(SendOutput.class);
	private String workerID;
	private ConcurrentHashMap<String, ConcurrentHashMap<Long, String>> workerMapForSocket = null;
	public PrintWriter sendOut;
	public SendOutput(PrintWriter pw, String workerID, ConcurrentHashMap<String,ConcurrentHashMap<Long, String>> workerMapForSocket)
	{
		this.workerMapForSocket = workerMapForSocket;
		this.workerID = workerID;
		sendOut = pw;
		start();
	}
	public void run() 
	{
		while(true)
		{
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (workerMapForSocket.containsKey(workerID)) {
				ConcurrentHashMap<Long, String > map = workerMapForSocket.get(workerID);
				List<Long> keys = new ArrayList<>(map.keySet());
				Collections.sort(keys);
				for (Long key : keys) {
                    logger.debug(">>>\t"+ workerID + "\t" + map.get(key));
					sendOut.println(map.get(key));
					map.remove(key);
				}
			}
		}
			
	}
	
}