package com.immunicare.immunicare_admin.params;

public class ClientAccounts {

    private String username;
    private String firstname;
    private String lastname;
    private String clientIdentifier;
    private String imageURL;
    private String accountStatus;

    public ClientAccounts(String username, String firstname, String lastname, String clientIdentifier, String imageURL, String accountStatus) {
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.clientIdentifier = clientIdentifier;
        this.imageURL = imageURL;
        this.accountStatus = accountStatus;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
}
