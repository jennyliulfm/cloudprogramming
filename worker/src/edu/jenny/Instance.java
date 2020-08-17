package edu.utas;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Instance {
	private final static Logger logger = Logger.getLogger(Instance.class);
    public static void main( String[] args ) throws IOException {
    	String instanceId;
    	String masterIp;
		int connectionPort;

		if (args.length > 0) {
			instanceId = args[0];
			if (args.length == 3) {
				masterIp = args[1];
				connectionPort = Integer.parseInt(args[2]);
			} else {
				masterIp = Consts.MASTER_IP;
				connectionPort = Consts.CONNECTION_PORT;
			}

			Socket socket = new Socket(masterIp, connectionPort);
			try {
				try {
					HashMap<String, EachSlave> processList = new HashMap<>();
					ConcurrentHashMap<Integer, String> pendingProcess = new ConcurrentHashMap<>();
					logger.info(socket);
					BufferedReader in =
							new BufferedReader(
									new InputStreamReader(
											socket.getInputStream()));
					// Output is automatically flushed
					// by PrintWriter:
					PrintWriter out =
							new PrintWriter(
									new BufferedWriter(
											new OutputStreamWriter(
													socket.getOutputStream())), true);

					out.println(instanceId);

					CountProcess cp = new CountProcess();
					CheckIdling ci = new CheckIdling(cp, out);
					String str;
					Scanner scanner = new Scanner(System.in);
					boolean flag = false;
					int i = 1;
					while (true) {
						Thread.sleep(10);
					    while (in.ready()) {
                            str = in.readLine();
                            if (str != null && !str.trim().isEmpty()) {
                                logger.debug(str);
                                if (str.startsWith(Consts.PREFIX_RUN)) {
                                    logger.debug("Adding Pending" + str);
                                    pendingProcess.put(i, str);
                                    i++;
                                    if (!flag) {
                                        RunQueue rq = new RunQueue(pendingProcess, cp, out, processList);
                                        flag = true;
                                    }
                                }
                                if (str.startsWith(Consts.PREFIX_CANCEL)) {
                                    str = str.split(Consts.SPLIT_COLON)[1];
                                    if (killProcess(str)) {
                                        out.println(Consts.PREFIX_RESULT + Consts.SPLIT_COLON + str);
                                        cp.removeProcess();
                                        processList.remove(str);
                                        logger.debug("Remove Successful: " + str);
                                    }
                                }
                            }
                        }
					}
				} catch (Exception e) {
					logger.error(e);
				}
			} finally {
				socket.close();
			}
		}
    }

    //https://www.mkyong.com/java/how-to-execute-shell-command-from-java/
	public static boolean killProcess(String fileName)
	{
		StringBuilder output = new StringBuilder();
		try {
			String[] cmd = {Consts.KILL_THREAD_SCRIPT, fileName};
			Process process = Runtime.getRuntime().exec(cmd ,null, new File(Consts.HOME));
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line);
				break;
			}
		}catch (IOException e) {
			logger.error(e);
		}
		return Boolean.valueOf(String.valueOf(output));
	}
}


class RunQueue extends Thread
{
	private final static Logger logger = Logger.getLogger(RunQueue.class);

	public HashMap<String, EachSlave> ProcessLst;
	public CountProcess cp1;
	public PrintWriter sendText;
	public ConcurrentHashMap<Integer, String> pendingList; 
	public RunQueue(ConcurrentHashMap<Integer, String> pending, CountProcess cp, PrintWriter pw, HashMap<String, EachSlave> ProLst)
	{
		 ProcessLst = ProLst;
		 pendingList = pending;
		 cp1 = cp;
		 sendText = pw;
		 start();
	}
	public void run()
	{
		int i = 1;
		while(true)
		{
			try {
			Thread.sleep(1000);
			} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}

			if(cp1.getProcessCount() < Consts.PROCESSES_MAX)
			{
				if(pendingList.size() > 0)
					{
					 String str = pendingList.get(i);
					 logger.debug("Execute Now: " + str);
					 EachSlave eSlave = new EachSlave(str, cp1, sendText);
					 String[] files = str.split(Consts.SPLIT_COLON);
					 ProcessLst.put(files[1],eSlave);

					 pendingList.remove(i);
					 i++;
					}
			}
		}
	}
}

class EachSlave extends Thread
{
	public CountProcess cp1;
	public PrintWriter sendText;
	public String receivedString; 
	public RunProcess runPro;
	public EachSlave(String rStr, CountProcess cp, PrintWriter pw)
	{
		 receivedString = rStr;
		 cp1 = cp;
		 sendText = pw;
		 start();
	}
	public void run()
	{
		 String[] files = receivedString.split(Consts.SPLIT_COLON);
		 String passCode = files[1];
		 String fileName = files[2];
		 String InputFile = "";
		 if(files.length == 4)
		 {
			InputFile = files[3];
			
		 }
		 
		if(true)
   		//if(moveFile(fileName))
   		{
				RunProcess rp = new RunProcess(fileName, passCode, InputFile, cp1, sendText);
   		}
	}
}

class CheckIdling extends Thread
{
	private final static Logger logger = Logger.getLogger(CheckIdling.class);

	public PrintWriter out;
	public int waitingTime = 300;
	public int count = 0;
	public CountProcess cp1;
	public CheckIdling(CountProcess cp, PrintWriter pw)
	{
		out = pw;
		cp1 = cp;
		start();
	}
	public void run()
	{
		while(true)
		{
			try {
				//Sleep for 1 second
				Thread.sleep(1000);
				if(cp1.getProcessCount() == 0)
				{
					waitingTime --;
				} else {
					resetTiming();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String msg = Consts.PREFIX_INFO + Consts.SPLIT_COLON + cp1.getProcessCount() + Consts.SPLIT_COLON + getCPUStatus();
			out.println(msg);
			count++;
			if (count == 10) {
				logger.debug(msg);
				count = 0;
			}

			if (waitingTime == 0) {
				resetTiming();
				out.println(Consts.PREFIX_INFO + Consts.SPLIT_COLON + waitingTime);
				logger.info("Server has been idling for 5mn now!");
			}
		}
	}
	

    //https://www.mkyong.com/java/how-to-execute-shell-command-from-java/
    public StringBuilder getCPUStatus()
    {
    	StringBuilder output = new StringBuilder();    	
    	try {
				String[] cmd = {
				"/bin/sh",
				"-c",
				"sar -u 1 1 | tail -n 1 | awk '{print $NF}'"
				};
	    		//Run a shell command
	    		Process process = Runtime.getRuntime().exec(cmd);	 	    		
	    		
	    		BufferedReader reader = new BufferedReader(
	    				new InputStreamReader(process.getInputStream()));
	    		String line;
	    		while ((line = reader.readLine()) != null) {
	    			output.append(line + "\n");
	    		}

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return output;
    }
	
	/**
	 * Reset the timing again, if detecting user interactivity.
	 */
	public void resetTiming()
	{
		waitingTime = 300;
	}
}

class CountProcess
{
	public int countP = 0;
	
	public void addProcess()
	{
		countP++;
	}
	public void removeProcess()
	{
		countP--;
	}
	public int getProcessCount()
	{
		return countP;
	}
}



class RunProcess extends Thread
{
	private final static Logger logger = Logger.getLogger(RunProcess.class);

	public String fileName = "";
	public CountProcess cp1;
	public String passCode;
	public PrintWriter sendText;
	public String inputFile;
	public RunProcess(String fName, String pc, String inputF, CountProcess cp, PrintWriter pw)
	{
		inputFile = inputF;
		sendText = pw;
		cp1 = cp;
		fileName = fName;
		passCode = pc;
		start();
	}
	public void run()
	{
		execCommend(fileName, passCode, inputFile);
	}
	
	
	
    //https://www.mkyong.com/java/how-to-execute-shell-command-from-java/
    public void execCommend(String fileName, String passCode, String inputFile)
    {
    	cp1.addProcess();
		long start_time = System.nanoTime();
		List<String> command = new ArrayList<>();
		PrintWriter writer = null;

		if (fileName.toLowerCase().endsWith(".jar")) {
			command.add("java");
			command.add("-jar");
		} else {
			command.add("python");
		}
		command.add(".." + File.separator + passCode + File.separator + fileName);
		if (inputFile != null && !inputFile.isEmpty()) {
			command.add(inputFile);
		}

		try {
			Process process = Runtime.getRuntime().exec(command.stream().toArray(String[]::new),
					null, new File(Consts.SOURCE_LOCATION + passCode));
			writer = new PrintWriter(Consts.OUTPUT_LOCATION + passCode + Consts.OUTPUT_FILENAME, "UTF-8");

			BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			lineReader.lines().forEach(writer::println);

			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			errorReader.lines().forEach(writer::println);

			int exitVal1 = process.waitFor();
			long end_time = System.nanoTime();
			double difference = (end_time - start_time) / 1e6;
			writer.close();
			sendText.println(Consts.PREFIX_RESULT + Consts.SPLIT_COLON + passCode + Consts.SPLIT_COLON + difference);
		} catch (Exception e) {
			logger.error(e);
		} finally {
			if (writer != null) {
				writer.close();
			}
			cp1.removeProcess();
		}
    }
}
