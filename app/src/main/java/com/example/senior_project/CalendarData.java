package com.example.senior_project;

public class CalendarData {
    private String calendarId;
    private String calendarName;

    public CalendarData() {
    }

    public CalendarData(String calendarId, String calendarName) {
        this.calendarId = calendarId;
        this.calendarName = calendarName;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }
}