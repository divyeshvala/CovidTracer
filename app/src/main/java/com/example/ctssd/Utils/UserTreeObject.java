package com.example.ctssd.Utils;

public class UserTreeObject
{
    int year;
    int month;
    int day;
    String phone;
    String time;
    int level;

    public UserTreeObject(int year, int month, int day, String phone, String time, int level) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.phone = phone;
        this.time = time;
        this.level = level;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public String getPhone() {
        return phone;
    }

    public String getTime() {
        return time;
    }

    public int getLevel() { return level; }
}
