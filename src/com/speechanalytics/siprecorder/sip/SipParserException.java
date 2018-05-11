package com.speechanalytics.siprecorder.sip;

public class SipParserException extends Exception {

    private static final long serialVersionUID = 1L;

    public SipParserException() {
        super();
    }

    public SipParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public SipParserException(String message) {
        super(message);
    }

    public SipParserException(Throwable cause) {
        super(cause);
    }

}
