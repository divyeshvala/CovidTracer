package com.example.ctssd.model;
public class User
{
    private String phone;
    private String time;
    public User(String phone, String time)
    {
        this.phone = phone;
        this.time = time;
    }
    public String getPhone() {
        return phone;
    }
    public String getTime() {
        return time;
    }
}
