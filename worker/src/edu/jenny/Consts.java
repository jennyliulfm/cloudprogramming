package edu.utas;

public final class Consts {
    public static final String JOB_INIT = "I";
    public static final String JOB_PROCESSING = "P";
    public static final String JOB_COMPLETED = "C";
    public static final String JOB_CANCELLING = "X";
    public static final String JOB_TERMINATED = "T";

    public static final String SPLIT_SEMICOLON = ";";
    public static final String SPLIT_COMMA = ",";
    public static final String SPLIT_COLON = ":";

    public static final String PREFIX_RUN = "RUN#";
    public static final String PREFIX_CANCEL = "CANCEL#";
    public static final String PREFIX_INFO = "INFO#";
    public static final String PREFIX_RESULT = "RESULT#";
	
	
	//Slave
	public static final String MASTER_IP = "115.146.86.114";
	public static final int CONNECTION_PORT = 8085;
	public static final int PROCESSES_MAX = 2;
	public static final String SOURCE_LOCATION = "/home/ubuntu/Sources/";
	public static final String OUTPUT_LOCATION = "/home/ubuntu/Outputs/";
	public static final String OUTPUT_FILENAME = "_Output.txt";
	public static final String KILL_THREAD_SCRIPT = "./killProcess.sh";
	public static final String HOME = "/home/ubuntu/";

}
