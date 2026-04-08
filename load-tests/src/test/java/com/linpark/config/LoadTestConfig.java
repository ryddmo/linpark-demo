package com.linpark.config;

/**
 * Runtime configuration for load tests.
 * These can be overridden via command-line properties: -Dusers=50, -Dduration=120, etc.
 */
public class LoadTestConfig {
    private int virtualUsers;
    private int duration;
    private int rampUp;
    private String targetUrl;
    private int thinkTime;

    public LoadTestConfig() {
    }

    public LoadTestConfig(int virtualUsers, int duration, int rampUp, String targetUrl, int thinkTime) {
        this.virtualUsers = virtualUsers;
        this.duration = duration;
        this.rampUp = rampUp;
        this.targetUrl = targetUrl;
        this.thinkTime = thinkTime;
    }

    public int getVirtualUsers() {
        return virtualUsers;
    }

    public void setVirtualUsers(int virtualUsers) {
        this.virtualUsers = virtualUsers;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getRampUp() {
        return rampUp;
    }

    public void setRampUp(int rampUp) {
        this.rampUp = rampUp;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public int getThinkTime() {
        return thinkTime;
    }

    public void setThinkTime(int thinkTime) {
        this.thinkTime = thinkTime;
    }
}
