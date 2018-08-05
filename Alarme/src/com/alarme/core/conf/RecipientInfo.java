package com.alarme.core.conf;

public class RecipientInfo {

    private String email;
    private String smsFreeUser;
    private String smsFreePass;

    public RecipientInfo(String email, String smsFreeUser, String smsFreePass) {
        this.email = email;
        this.smsFreeUser = smsFreeUser;
        this.smsFreePass = smsFreePass;
    }

    public String getEmail() {
        return email;
    }

    public String getSmsFreeUser() {
        return smsFreeUser;
    }

    public String getSmsFreePass() {
        return smsFreePass;
    }

}