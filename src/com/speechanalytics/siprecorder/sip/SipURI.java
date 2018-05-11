package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.utils.RFC3261;

import java.util.Hashtable;

public class SipURI {

    public final static int DEFAULT_PORT = -1;

    private String stringRepresentation;
    /**
     * telephone-subscriber and optional port are not managed
     */
    private String userinfo;
    private String host;
    private int port = DEFAULT_PORT;
    /**
     * Use empty strings in value if the parameter has no value
     */
    private Hashtable<String, String> uriParameters;
    //headers not implemented
    //private Hashtable<String, String> headers;

    public SipURI(String sipUri)
            throws SipUriSyntaxException {
        stringRepresentation = sipUri;
        StringBuffer buf = new StringBuffer(sipUri);
        String scheme = RFC3261.SIP_SCHEME + RFC3261.SCHEME_SEPARATOR;
        if (!sipUri.startsWith(scheme)) {
            throw new SipUriSyntaxException("SIP URI must start with " + scheme);
        }
        buf.delete(0, scheme.length());
        int atPos = buf.indexOf("@");
        if (atPos == 0) {
            throw new SipUriSyntaxException("userinfo cannot start with a '@'");
        }
        if (atPos > 0) {
            userinfo = buf.substring(0, atPos);
            buf.delete(0, atPos + 1);
        }
        int endHostport = buf.indexOf(";");
        if (endHostport == 0) {
            throw new SipUriSyntaxException("hostport not present or it cannot start with ';'");
        }
        if (endHostport < 0) {
            endHostport = buf.length();
        }
        String hostport = buf.substring(0, endHostport);
        buf.delete(0, endHostport);
        int colonPos = hostport.indexOf(':');
        if (colonPos > -1) {
            if (colonPos == hostport.length() - 1) {
                throw new SipUriSyntaxException("hostport cannot terminate with a ':'");
            }
            port = Integer.parseInt(hostport.substring(colonPos + 1));
        } else {
            colonPos = hostport.length();
        }
        host = hostport.substring(0, colonPos);
        if (buf.length() == 1) {
            //if there is only one ';' at the end of the uri => do not
            //parse uri-parameters and headers
            buf.deleteCharAt(0);
        }
        if (buf.length() <= 0) {
            return;
        }
        uriParameters = new Hashtable<String, String>();
        while (buf.length() > 0) {
            buf.deleteCharAt(0);//delete the first ';'
            int nextSemicolon = buf.indexOf(";");
            if (nextSemicolon < 0) {
                nextSemicolon = buf.length();
            }
            int nextEquals = buf.indexOf("=");
            if (nextEquals < 0) {
                nextEquals = nextSemicolon;
            }
            if (nextEquals > nextSemicolon) {
                nextEquals = nextSemicolon;
            }
            int afterEquals;
            if (nextEquals + 1 > nextSemicolon) {
                afterEquals = nextSemicolon;
            } else {
                afterEquals = nextEquals + 1;
            }
            uriParameters.put(buf.substring(0, nextEquals), buf.substring(afterEquals, nextSemicolon));
            buf.delete(0, nextSemicolon);
        }
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Hashtable<String, String> getUriParameters() {
        return uriParameters;
    }

    public String getUserinfo() {
        return userinfo;
    }

}
