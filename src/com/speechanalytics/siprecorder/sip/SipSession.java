package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.rtp.RtpPacket;
import com.speechanalytics.siprecorder.utils.DBHelper;
import com.speechanalytics.siprecorder.utils.RFC3261;
import com.speechanalytics.siprecorder.utils.RTPConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SipSession {

    private String TO;
    private String FROM;

    private String CALL_ID;

    private List<SipPacket> sipPackets;
    private List<RtpPacket> rtpPackets;

    private String fileName;

    private int rtpPort;

    private int[] udpPorts;

    private String method;

    private boolean sessionStarted;
    private boolean sessionEnded;
    private boolean onGoing;

    public SipSession(SipPacket sipPacket) {
        GetSipInfo(sipPacket);
        sipPackets = new ArrayList<>();
        rtpPackets = new ArrayList<>();
        rtpPort = sipPacket.getRtpPort();
        this.udpPorts = new int[]{sipPacket.getSource(), sipPacket.getDestination()};
        addSipPacket(sipPacket);
    }

    public SipSession() {

    }

    public boolean isOnGoing() {
        onGoing = sessionStarted && !sessionEnded;
        return onGoing;
    }

    public void GetSipInfo(SipPacket sipPacket) {
        SipHeaders headers = sipPacket.getHeaders();

        if (headers != null) {
            SipHeaderFieldName fromName = new SipHeaderFieldName(RFC3261.HDR_FROM);
            SipHeaderFieldValue fromVal = headers.get(fromName);
            if (fromVal != null)
                FROM = fromVal.getValue();

            SipHeaderFieldName toName = new SipHeaderFieldName(RFC3261.HDR_TO);
            SipHeaderFieldValue toVal = headers.get(toName);
            if (toVal != null)
                TO = toVal.getValue();

            SipHeaderFieldName callIdName = new SipHeaderFieldName(RFC3261.HDR_CALLID);
            SipHeaderFieldValue callIdVal = headers.get(callIdName);
            if (callIdVal != null)
                CALL_ID = callIdVal.getValue();

            method = sipPacket.getMethod();

        }

    }

    public SipSession getSession(int[] ports) {
        boolean hasPort1 = false, hasPort2 = false;
        if (this.udpPorts[0] == ports[0] || this.udpPorts[1] == ports[0])
            hasPort1 = true;

        if (this.udpPorts[0] == ports[1] || this.udpPorts[1] == ports[1])
            hasPort2 = true;

        if (hasPort1 && hasPort2)
            return this;
        else
            return null;

    }

    public SipSession getSession(String call_id) {
        if (CALL_ID == call_id)
            return this;
        else
            return null;
    }

    public void setUdpPorts(int port1, int port2) {
        udpPorts[0] = port1;
        udpPorts[1] = port2;
    }

    public int[] getUdpPorts() {
        return udpPorts;
    }

    public void setRtpPort(int rtpPort) {
        this.rtpPort = rtpPort;
    }

    public int getRtpPort() {
        return rtpPort;
    }

    public String getTO() {
        return TO;
    }

    public void setTO(String to) {
        TO = to;
    }

    public String getFROM() {
        return FROM;
    }

    public void setFROM(String from) {
        FROM = from;
    }

    public String getCALL_ID() {
        return CALL_ID;
    }

    public void setCALL_ID(String call_id) {
        CALL_ID = call_id;
    }

    public void addSipPacket(SipPacket sipPacket) {
        if (!hasSipPacket(sipPacket)) {
            sipPackets.add(sipPacket);
            int rtpP = sipPacket.getRtpPort();
            if (getRtpPort() == 0 && rtpP != 0)
                setRtpPort(rtpP);
            if (sipPacket.getMethod().equals(RFC3261.METHOD_BYE)) {
                System.err.println(sipPacket.getMethod());
                sessionEnded = true;
                EndSession();
            }
        }

    }

    public boolean hasSipPacket(SipPacket sipPacket) {
        return sipPackets.contains(sipPackets);
    }

    public List<SipPacket> getSipPackets() {
        return sipPackets;
    }

    public List<RtpPacket> getRtpPackets() {
        return rtpPackets;
    }

    public boolean removeSipPacket(SipPacket sipPacket) {
        if (hasSipPacket(sipPacket)) {
            sipPackets.remove(sipPacket);
            return true;
        }
        return false;
    }

    public void addRtpPacket(RtpPacket rtpPacket) {
        if (!sessionEnded)
            if (!hasRtpPacket(rtpPacket))
                rtpPackets.add(rtpPacket);
    }

    public boolean hasRtpPacket(RtpPacket rtpPacket) {
        return rtpPackets.contains(rtpPacket);
    }

    public boolean removeRtpPacket(RtpPacket rtpPacket) {
        if (hasRtpPacket(rtpPacket)) {
            rtpPackets.remove(rtpPacket);
            return true;
        }
        return false;
    }

    private void EndSession() {
        System.out.println("Session with CALL_ID: " + CALL_ID + " ended.");
        sessionEnded = true;
        Collections.sort(rtpPackets);
        Thread convertRTPPackets = new Thread() {
            @Override
            public void run() {
                RTPConverter converter = new RTPConverter();
                File file = new File("C:\\Tester\\" + CALL_ID + ".wav");
                converter.ConvertRTPToAudio(rtpPackets, file);
                DBHelper dbHelper = new DBHelper();
                dbHelper.InsertData(getFROM(), getTO(), getCALL_ID(), "C:\\Tester\\" + CALL_ID + ".wav");
            }
        };
        convertRTPPackets.start();
    }

    @Override
    public String toString() {
        String s = "Session between " + FROM + " and " + TO +
                "with CALL_ID: " + CALL_ID +
                " on udpPorts " + udpPorts[0] + " & " + udpPorts[1];
        return s;
    }
}
