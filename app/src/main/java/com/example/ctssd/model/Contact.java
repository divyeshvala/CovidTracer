package com.example.ctssd.model;

public class Contact
{
    private String phone;
    private String time;
    private int risk;
    private String location;

    public Contact(String phone, String time, int risk, String location) {
        this.phone = phone;
        this.time = time;
        this.risk = risk;
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public String getTime() {
        return time;
    }

    public int getRisk() {
        return risk;
    }

    public String getLocation() {
        return location;
    }
}

