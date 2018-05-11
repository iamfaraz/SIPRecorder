package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.utils.RFC3261;

import java.util.HashMap;

public class SipHeadersTable {

    private HashMap<Character, String> headers;

    /**
     * should be instanciated only once, it was a singleton.
     */
    public SipHeadersTable() {
        headers = new HashMap<Character, String>();
        //RFC 3261 Section 10
        headers.put(RFC3261.COMPACT_HDR_CALLID, RFC3261.HDR_CALLID);
        headers.put(RFC3261.COMPACT_HDR_CONTACT, RFC3261.HDR_CONTACT);
        headers.put(RFC3261.COMPACT_HDR_CONTENT_ENCODING, RFC3261.HDR_CONTENT_ENCODING);
        headers.put(RFC3261.COMPACT_HDR_CONTENT_LENGTH, RFC3261.HDR_CONTENT_LENGTH);
        headers.put(RFC3261.COMPACT_HDR_CONTENT_TYPE, RFC3261.HDR_CONTENT_TYPE);
        headers.put(RFC3261.COMPACT_HDR_FROM, RFC3261.HDR_FROM);
        headers.put(RFC3261.COMPACT_HDR_SUBJECT, RFC3261.HDR_SUBJECT);
        headers.put(RFC3261.COMPACT_HDR_SUPPORTED, RFC3261.HDR_SUPPORTED);
        headers.put(RFC3261.COMPACT_HDR_TO, RFC3261.HDR_TO);
        headers.put(RFC3261.COMPACT_HDR_VIA, RFC3261.HDR_VIA);
    }

    public String getLongForm(char compactForm) {
        return headers.get(compactForm);
    }

}