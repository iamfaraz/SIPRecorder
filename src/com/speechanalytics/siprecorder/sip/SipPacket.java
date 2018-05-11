package com.speechanalytics.siprecorder.sip;

public class SipPacket {

    private SipHeaders headers;
    private String method;
    private int source;
    private int destination;
    private int rtpPort;
    private boolean isRequest;

    /**
     * SipPacket constructor
     *
     * @param sipMessage  SipMessage got from sip
     * @param source      source port of packet
     * @param destination destination of the packet
     */
    public SipPacket(SipMessage sipMessage, int source, int destination) {
        if (sipMessage instanceof SipRequest) {
            SipRequest request = (SipRequest) sipMessage;
            method = request.getMethod();
            headers = request.getSipHeaders();
            isRequest = true;
        } else {
            SipResponse response = (SipResponse) sipMessage;
            method = response.getReasonPhrase();
            headers = response.getSipHeaders();
            isRequest = false;
        }

        this.source = source;
        this.destination = destination;
    }

    private static void print(Object message) {
        System.out.println(message.toString());
    }

    public SipHeaders getHeaders() {
        return headers;
    }

    public String getHeaderFieldValue(String headerFieldName) {
        for (SipHeader header : headers.getAllHeaders()) {
            if (header.getName().equals(new SipHeaderFieldName(headerFieldName)))
                return header.getValue().getValue();
        }
        return null;
    }

    public String getMethod() {
        return method;
    }

    public int getSource() {
        return source;
    }

    public int getDestination() {
        return destination;
    }

    public int getRtpPort() {
        return rtpPort;
    }

    public void setRtpPort(int rtpPort) {
        this.rtpPort = rtpPort;
    }

    public boolean isRequest() {
        return isRequest;
    }

    @Override
    public String toString() {
        String message = "";
        for (SipHeader header : headers.getAllHeaders()) {
            message += header.getName().getName() + ": " + header.getValue().getValue() + "\n";
        }
        return message + "\n";
    }


}
