package com.example.ctssd.model;

import java.util.Calendar;

public class DeviceDetails
{
    private Calendar time;
    private String distance;
    private int riskIndex;

    public DeviceDetails(Calendar time, String distance, int riskIndex) {
        this.time = time;
        this.distance = distance;
        this.riskIndex = riskIndex;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public int getRiskIndex() {
        return riskIndex;
    }

    public void setRiskIndex(int riskIndex) {
        this.riskIndex = riskIndex;
    }
}
