package com.speechanalytics.siprecorder.sip;

public class SipUriSyntaxException extends Exception {

    private static final long serialVersionUID = 1L;

    public SipUriSyntaxException() {
        super();
    }

    public SipUriSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public SipUriSyntaxException(String message) {
        super(message);
    }

    public SipUriSyntaxException(Throwable cause) {
        super(cause);
    }


}