package com.speechanalytics.siprecorder.sip;

public class SipHeaderParamName {

    private String name;

    public SipHeaderParamName(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        String objName = ((SipHeaderParamName) obj).getName();
        return name.equalsIgnoreCase(objName);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
