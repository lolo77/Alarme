package com.alarme.core.conf;

import java.io.Serializable;

public class RecipientInfo implements Serializable {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipientInfo)) return false;

        RecipientInfo that = (RecipientInfo) o;

        if (!email.equals(that.email)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return "RecipientInfo{" +
                "email='" + email + '\'' +
                ", smsFreeUser='" + smsFreeUser + '\'' +
                ", smsFreePass='" + smsFreePass + '\'' +
                '}';
    }
}