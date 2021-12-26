package com.example.ctssd.model;

public class DailyStat {
    private int contactsCount;
    private int risk;
    private float bluetoothOnTime;

    public DailyStat(int contactsCount, int risk, float bluetoothOnTime) {
        this.contactsCount = contactsCount;
        this.risk = risk;
        this.bluetoothOnTime = bluetoothOnTime;
    }

    public int getContactsCount() {
        return contactsCount;
    }

    public int getRisk() {
        return risk;
    }

    public float getBluetoothOnTime() {
        return bluetoothOnTime;
    }
}
