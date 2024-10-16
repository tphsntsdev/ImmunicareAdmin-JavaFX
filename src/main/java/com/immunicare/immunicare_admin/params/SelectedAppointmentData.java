package com.immunicare.immunicare_admin.params;

public class SelectedAppointmentData {

    private String primaryKey;
    private String appointmentID;
    private String childIdentifier;

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getAppointmentID() {
        return appointmentID;
    }

    public void setAppointmentID(String appointmentID) {
        this.appointmentID = appointmentID;
    }

    public String getChildIdentifier() {
        return childIdentifier;
    }

    public void setChildIdentifier(String childIdentifier) {
        this.childIdentifier = childIdentifier;
    }
}
