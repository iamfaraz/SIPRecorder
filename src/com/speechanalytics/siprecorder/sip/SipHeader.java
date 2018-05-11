package com.speechanalytics.siprecorder.sip;

public class SipHeader {

    private SipHeaderFieldName name;
    private SipHeaderFieldValue value;

    SipHeader(SipHeaderFieldName name, SipHeaderFieldValue value) {
        super();
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SipHeader) {
            SipHeader objHdr = (SipHeader) obj;
            return name.equals(objHdr.name);
        }
        return false;
    }

    public SipHeaderFieldName getName() {
        return name;
    }

    public SipHeaderFieldValue getValue() {
        return value;
    }

    public void setValue(SipHeaderFieldValue value) {
        this.value = value;
    }

}
