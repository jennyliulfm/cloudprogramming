package edu.utas.vo;

public class InstanceInfo {
    private String instanceId;
    private int totalTasks;
    private double idlePercentage;
    private boolean keptIdle;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public double getIdlePercentage() {
        return idlePercentage;
    }

    public void setIdlePercentage(double idlePercentage) {
        this.idlePercentage = idlePercentage;
    }

    public boolean isKeptIdle() {
        return keptIdle;
    }

    public void setKeptIdle(boolean keptIdle) {
        this.keptIdle = keptIdle;
    }
}
