package com.example.ctssd.Utils;

import java.util.Calendar;

public class DeviceDetailsObject
{
    private Calendar time;
    private String distance;
    private int riskIndex;

    public DeviceDetailsObject(Calendar time, String distance, int riskIndex) {
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
