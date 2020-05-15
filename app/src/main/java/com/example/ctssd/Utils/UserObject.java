package com.example.ctssd.Utils;
public class UserObject
{
    private String phone;
    private String time;
    public UserObject(String phone, String time)
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
