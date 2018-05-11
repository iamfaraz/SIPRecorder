package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.utils.RFC3261;

public abstract class SipMessage {

    protected String sipVersion;
    protected SipHeaders sipHeaders;
    protected byte[] body;

    public SipMessage() {
        sipVersion = RFC3261.DEFAULT_SIP_VERSION;
        sipHeaders = new SipHeaders();
    }

    public String getSipVersion() {
        return sipVersion;
    }

    public SipHeaders getSipHeaders() {
        return sipHeaders;
    }

    public void setSipHeaders(SipHeaders sipHeaders) {
        this.sipHeaders = sipHeaders;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        SipHeaderFieldName contentLengthName =
                new SipHeaderFieldName(RFC3261.HDR_CONTENT_LENGTH);
        SipHeaderFieldValue contentLengthValue =
                sipHeaders.get(contentLengthName);
        if (contentLengthValue == null) {
            contentLengthValue = new SipHeaderFieldValue(
                    String.valueOf(body.length));
            sipHeaders.add(contentLengthName, contentLengthValue);
        } else {
            contentLengthValue.setValue(String.valueOf(body.length));
        }
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(sipHeaders.toString());
        buf.append(RFC3261.CRLF);
        if (body != null) {
            buf.append(new String(body));
        }
        return buf.toString();
    }

}
