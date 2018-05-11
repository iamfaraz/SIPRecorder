package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.utils.RFC3261;

public class SipResponse extends SipMessage {
    protected int statusCode;
    protected String reasonPhrase;

    public SipResponse(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(RFC3261.DEFAULT_SIP_VERSION).append(' ').append(statusCode
        ).append(' ').append(reasonPhrase).append(RFC3261.CRLF);
        buf.append(super.toString());
        return buf.toString();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

}
