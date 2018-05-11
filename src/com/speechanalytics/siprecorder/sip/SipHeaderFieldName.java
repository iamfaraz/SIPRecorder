package com.speechanalytics.siprecorder.sip;

public class SipHeaderFieldName {

    private final static SipHeadersTable SIP_HEADER_TABLE =
            new SipHeadersTable();

    private String name;

    public SipHeaderFieldName(String name) {
        super();
        if (name.length() == 1) {
            this.name = SIP_HEADER_TABLE.getLongForm(name.charAt(0));
        } else {
            this.name = name;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        String objName = ((SipHeaderFieldName) obj).getName();
        return name.equalsIgnoreCase(objName);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
