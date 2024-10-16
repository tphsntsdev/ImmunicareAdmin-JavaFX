package com.immunicare.immunicare_admin.params;

public class AppointmentDetails {

    private String adultVaccine;
    private String childVaccine;
    private String date;
    private String time;
    private String location;
    private final String appointmentKey;
    private String appointmentKeyforList;

    public AppointmentDetails(String adultVaccine, String childVaccine, String date, String time, String location, String appointmentKey, String appointmentKeyforList) {
        this.appointmentKey = appointmentKey;
        this.adultVaccine = adultVaccine;
        this.childVaccine = childVaccine;
        this.date = date;
        this.time = time;
        this.location = location;
        this.appointmentKeyforList = appointmentKeyforList;
    }

    public String getIdentifier() {
        return appointmentKey;
    }

    public String getAdultVaccine() {
        return adultVaccine;
    }

    public void setAdultVaccine(String adultVaccine) {
        this.adultVaccine = adultVaccine;
    }

    public String getChildVaccine() {
        return childVaccine;
    }

    public void setChildVaccine(String childVaccine) {
        this.childVaccine = childVaccine;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAppointmentKey() {
        return appointmentKeyforList;
    }

    public void setAppointmentKey(String appointmentKey) {
        this.appointmentKeyforList = appointmentKey;
    }

    @Override
    public String toString() {
        return "Adult Vaccine: " + adultVaccine + "\n"
                + "Child Vaccine: " + childVaccine + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Location: " + location;
    }
}
