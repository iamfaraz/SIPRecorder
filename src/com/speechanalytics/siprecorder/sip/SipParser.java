package com.speechanalytics.siprecorder.sip;

import com.speechanalytics.siprecorder.utils.RFC3261;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SipParser {

    private final static int BUFF_SIZE = 1024;
    private BufferedReader reader;
    private List<SipHeaderFieldName> singleValueHeaders;

    public SipParser() {
        singleValueHeaders = new ArrayList<>();
        singleValueHeaders.add(new SipHeaderFieldName(
                RFC3261.HDR_WWW_AUTHENTICATE));
        singleValueHeaders.add(new SipHeaderFieldName(
                RFC3261.HDR_AUTHORIZATION));
        singleValueHeaders.add(new SipHeaderFieldName(
                RFC3261.HDR_PROXY_AUTHENTICATE));
        singleValueHeaders.add(new SipHeaderFieldName(
                RFC3261.HDR_PROXY_AUTHORIZATION));
        singleValueHeaders.add(new SipHeaderFieldName(
                RFC3261.HDR_SUPPORTED));
        singleValueHeaders.add(new SipHeaderFieldName(
                RFC3261.HDR_SUBJECT));
    }

    public synchronized SipMessage parse(byte[] bytes)
            throws IOException, SipParserException {

        InputStream in = new ByteArrayInputStream(bytes);
        InputStreamReader inputStreamReader = new InputStreamReader(in);
        reader = new BufferedReader(inputStreamReader);

        String startLine = reader.readLine();
        while (startLine == null || startLine.equals("")) {
            startLine = reader.readLine();
        }
        SipMessage sipMessage;
        if (startLine.toUpperCase().startsWith(RFC3261.DEFAULT_SIP_VERSION)) {
            sipMessage = parseSipResponse(startLine);
        } else {
            sipMessage = parseSipRequest(startLine);
        }
        parseHeaders(sipMessage);
        parseBody(sipMessage);
        return sipMessage;
    }

    public synchronized SipMessage parse(InputStream in)
            throws IOException, SipParserException {

        InputStreamReader inputStreamReader = new InputStreamReader(in);
        reader = new BufferedReader(inputStreamReader);

        String startLine = reader.readLine();
        while (startLine == null || startLine.equals("")) {
            startLine = reader.readLine();
        }
        SipMessage sipMessage;
        if (startLine.toUpperCase().startsWith(RFC3261.DEFAULT_SIP_VERSION)) {
            sipMessage = parseSipResponse(startLine);
        } else {
            sipMessage = parseSipRequest(startLine);
        }
        parseHeaders(sipMessage);
        parseBody(sipMessage);
        return sipMessage;
    }

    private SipRequest parseSipRequest(String startLine) throws SipParserException {
        String[] params = startLine.split(" ");
        if (params.length != 3) {
            throw new SipParserException("invalid request line");
        }
        if (!RFC3261.DEFAULT_SIP_VERSION.equals(params[2].toUpperCase())) {
            throw new SipParserException("unsupported SIP version");
        }
        SipURI requestUri;
        try {
            requestUri = new SipURI(params[1]);
        } catch (SipUriSyntaxException e) {
            throw new SipParserException(e);
        }
        return new SipRequest(params[0], requestUri);
    }

    private SipResponse parseSipResponse(String startLine) throws SipParserException {
        String[] params = startLine.split(" ");
        if (params.length < 3) {
            throw new SipParserException("incorrect status line");
        }
        if (!RFC3261.DEFAULT_SIP_VERSION.equals(params[0].toUpperCase())) {
            throw new SipParserException("unsupported SIP version");
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 2; i < params.length; ++i) {
            buf.append(params[i]).append(" ");
        }
        buf.deleteCharAt(buf.length() - 1);
        return new SipResponse(Integer.parseInt(params[1]), buf.toString());
    }

    private void parseHeaders(SipMessage sipMessage) throws IOException, SipParserException {
        SipHeaders sipHeaders = new SipHeaders();
        String headerLine = reader.readLine();

        if (headerLine == null) {
            throw new SipParserException(sipMessage.toString());
        }

        while (!"".equals(headerLine)) {
            String nextLine = reader.readLine();
            if (nextLine != null &&
                    (nextLine.startsWith(" ") || nextLine.startsWith("\t"))) {
                StringBuffer buf = new StringBuffer(headerLine);
                while (nextLine != null &&
                        (nextLine.startsWith(" ") || nextLine.startsWith("\t"))) {
                    buf.append(' ');
                    buf.append(nextLine.trim());
                    nextLine = reader.readLine();
                }
                headerLine = buf.toString();
            }

            if (headerLine == null) {
                continue;//throw new SipParserException(sipMessage.toString());
            }
            int columnPos = headerLine.indexOf(RFC3261.FIELD_NAME_SEPARATOR);
            if (columnPos < 0) {
                throw new SipParserException("Invalid header line");
            }
            SipHeaderFieldName sipHeaderName = new SipHeaderFieldName(
                    headerLine.substring(0, columnPos).trim());
            String value = headerLine.substring(columnPos + 1).trim();
            SipHeaderFieldValue sipHeaderValue;
            if (!singleValueHeaders.contains(sipHeaderName) &&
                    value.indexOf(RFC3261.HEADER_SEPARATOR) > -1) {
                String[] values = value.split(RFC3261.HEADER_SEPARATOR);
                List<SipHeaderFieldValue> list =
                        new ArrayList<SipHeaderFieldValue>();
                for (String s : values) {
                    list.add(new SipHeaderFieldValue(s));
                }
                sipHeaderValue = new SipHeaderFieldMultiValue(list);
            } else {
                sipHeaderValue = new SipHeaderFieldValue(value);
            }
            sipHeaders.add(sipHeaderName, sipHeaderValue);
            headerLine = nextLine;
        }
        sipMessage.setSipHeaders(sipHeaders);
    }

    public void parseBody(SipMessage sipMessage) throws IOException {
        SipHeaderFieldValue contentLengthValue =
                sipMessage.getSipHeaders().get(new SipHeaderFieldName(
                        RFC3261.HDR_CONTENT_LENGTH));
        if (contentLengthValue == null) {
            return;
        }
        int length = Integer.parseInt(contentLengthValue.toString());
        byte[] buff = new byte[BUFF_SIZE];
        int i;
        int count = 0;
        while (count < length && (i = reader.read()) != -1) {
            if (count >= buff.length) {
                byte[] aux = new byte[buff.length + BUFF_SIZE];
                System.arraycopy(buff, 0, aux, 0, buff.length);
                buff = aux;

            }
            buff[count++] = (byte) i;
        }
        if (count != buff.length) {
            byte[] aux = new byte[count];
            System.arraycopy(buff, 0, aux, 0, count);
            buff = aux;
        }
        sipMessage.setBody(buff);
    }
}
