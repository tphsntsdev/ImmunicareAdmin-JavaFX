package com.immunicare.immunicare_admin.params;

public class VaccineTakenView {

    private String adultVaccine;
    private String childVaccine;
    private String date;
    private String time;
    private String location;
    private String vaccineKey;
    private Boolean child_state;

    public VaccineTakenView(String adultVaccine, String childVaccine, String date, String time, String location, String vaccineKey, Boolean child_state) {

        this.adultVaccine = adultVaccine;
        this.childVaccine = childVaccine;
        this.date = date;
        this.time = time;
        this.location = location;
        this.vaccineKey = vaccineKey;
        this.child_state = child_state;
    }

    @Override
    public String toString() {
        if (child_state) {
            return getFormatForChildVaccine();
        } else {
            return getFormatForAdultVaccine();
        }
    }

    public String getFormatForChildVaccine() {
        return "Vaccine: " + childVaccine + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Vaccine Key: " + vaccineKey + "\n"
                + "Location: " + location;
    }

    public String getFormatForAdultVaccine() {
        return "Vaccine: " + adultVaccine + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Vaccine Key: " + vaccineKey + "\n"
                + "Location: " + location;
    }

    public String getChildVaccine() {
        return childVaccine;
    }

    public void setChildVaccine(String childVaccine) {
        this.childVaccine = childVaccine;
    }

    public String getAdultVaccine() {
        return adultVaccine;
    }

    public void setAdultVaccine(String adultVaccine) {
        this.adultVaccine = adultVaccine;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVaccineKey() {
        return vaccineKey;
    }

    public void setVaccineKey(String vaccineKey) {
        this.vaccineKey = vaccineKey;
    }

    public Boolean getChild_state() {
        return child_state;
    }

    public void setChild_state(Boolean child_state) {
        this.child_state = child_state;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
