package com.example.ctssd.Utils;

import java.util.Calendar;

public class UserQueueObject
{
    private String phone;
    private Calendar calendar;

    public UserQueueObject(String phone, Calendar calendar) {
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
