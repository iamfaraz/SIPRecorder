package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.rtp.RtpPacket;
import com.speechanalytics.siprecorder.utils.RFC3261;

import java.util.ArrayList;
import java.util.List;

public class SipSessions {
    private List<SipSession> sessions;

    public SipSessions() {
        sessions = new ArrayList<>();
    }

    public void addSession(SipSession sipSession) {
        if (!hasSession(sipSession)) {
            sessions.add(sipSession);
        } else {
            sessions.set(sessions.indexOf(getSession(sipSession.getCALL_ID())), sipSession);
        }
    }

    public boolean removeSession(SipSession sipSession) {
        if (hasSession(sipSession)) {
            sessions.remove(sipSession);
            return true;
        }
        return false;
    }

    public boolean hasSession(SipSession sipSession) {
        for (SipSession session : sessions) {
            if (session.getCALL_ID().equals(sipSession.getCALL_ID()))
                return true;
        }
        return false;
    }

    public SipSession getSession(String call_id) {
        for (SipSession session : sessions) {
            if (session.getCALL_ID().equals(call_id))
                return session;
        }
        return null;
    }

    public SipSession getSession(SipPacket packet) {
        String call_id = packet.getHeaderFieldValue(RFC3261.HDR_CALLID);
        if (call_id == null)
            return null;
        return getSession(call_id);
    }

    public SipSession getSession(RtpPacket packet) {
        int src = packet.getSrcPort();
        int dest = packet.getDestPort();

        for (SipSession session : sessions) {
            int rtpPort = session.getRtpPort();
            if (src == rtpPort || dest == rtpPort) {
                return session;
            }
        }
        return null;
    }

    public List<SipSession> getOnGoingSessions() {
        List<SipSession> onGoingSessions = new ArrayList<>();
        for (SipSession session : sessions) {
            if (session.isOnGoing())
                onGoingSessions.add(session);
        }
        return onGoingSessions;
    }
}
