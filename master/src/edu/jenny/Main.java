package edu.utas;

import edu.utas.util.Consts;
import edu.utas.vo.InstanceInfo;

import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) {
        ConcurrentHashMap<String,ConcurrentHashMap<Long, String>> workerMapForSocket = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> jobFinishedMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, InstanceInfo> instanceInfoMap = new ConcurrentHashMap<>();

        int schedulingMethod;

        if (args.length != 1) {
            schedulingMethod = Consts.SCHEDULING_PRIORITY;
        } else {
            schedulingMethod = Integer.parseInt(args[0]);
            if (schedulingMethod != Consts.SCHEDULING_ROUND_ROBIN) {
                schedulingMethod = Consts.SCHEDULING_PRIORITY;
            }
        }
        JobMaster jobMaster = new JobMaster(schedulingMethod, workerMapForSocket, instanceInfoMap);
        JobFinishHandler jobFinishHandler = new JobFinishHandler(jobFinishedMap);
        Master master = new Master(workerMapForSocket, jobFinishedMap, instanceInfoMap);
    }
}
