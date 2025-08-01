package com.pixserve.model;

// Inner static classes
public class TakenInfo {
    private int year;
    private int month;
    private int day;
    private String dateTime;

    public TakenInfo() {
    }

    public TakenInfo(int year, int month, int day, String dateTime) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.dateTime = dateTime;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
