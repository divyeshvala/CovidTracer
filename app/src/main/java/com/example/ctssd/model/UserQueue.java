package com.example.ctssd.model;

import java.util.Calendar;

public class UserQueue
{
    private String phone;
    private Calendar calendar;

    public UserQueue(String phone, Calendar calendar) {
        this.phone = phone;
        this.calendar = calendar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }
}
