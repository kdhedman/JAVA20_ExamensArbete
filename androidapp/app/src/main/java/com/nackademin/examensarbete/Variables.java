package com.nackademin.examensarbete;

import java.util.ArrayList;

public class Variables {
    private static final Variables INSTANCE = new Variables();

    private String CARDNR_PREFERENCE = "cardNr";
    private ArrayList<String> idPoints;
    private ArrayList<Object> idPointsListeners;
    private ArrayList<String> targets;
    private ArrayList<Object> targetListeners;
    private ArrayList<String> doorControllers;
    private ArrayList<Object> doorControllerListeners;
    final String BLE_EXIT_CONFIG_MODE = "1";
    final String BLE_IP_CONFIG = "2";
    final String BLE_IP_RESET = "3";
    final String BLE_UPDATE_IDPOINT = "4";
    final String BLE_IDPOINT_CHOICE = "5";
    final String BLE_UPDATE_DOORS = "6";
    final String BLE_DOOR_CHOICE = "7";
    final String BLE_ACCESS_DOOR = "8";
    final String DELIMITER = "|";
    final String DELIMITER_SUB = ";";


    private String username;
    private String password;
    private boolean DHCP;
    private String IP;
    private String gateway;
    private String subnet;
    private String dns;
    private String doorCtrlIP;


    private long paused = 1;
    private long resumed = 1;
    private boolean isAuthenticated = false;

    private Variables(){}

    public static Variables getInstance(){
        return INSTANCE;
    }

    public boolean isAuthenticated(){
        return isAuthenticated;
    }

    public void setAuthenticated(boolean isAuthenticated){
        this.isAuthenticated = isAuthenticated;
    }

    public long getPaused() {
        return paused;
    }

    public void setPaused(long paused) {
        this.paused = paused;
    }

    public long getResumed() {
        return resumed;
    }

    public void setResumed(long resumed) {
        this.resumed = resumed;
    }

    public ArrayList<String> getIdPoints() {
        return idPoints;
    }

    public void setIdPoints(ArrayList<String> idPoints) {
        this.idPoints = idPoints;
    }

    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

    public ArrayList<String> getDoorControllers() {
        return doorControllers;
    }

    public void setDoorControllers(ArrayList<String> doorControllers) {
        this.doorControllers = doorControllers;
    }
}
