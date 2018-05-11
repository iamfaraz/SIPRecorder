package com.speechanalytics.siprecorder.rtp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RtpPacket implements Comparable {
    private int version;
    private boolean padding;
    private boolean extension;
    private int csrcCount;
    private boolean marker;
    private int payloadType;
    private int sequenceNumber;
    private long timestamp;
    private long ssrc;
    private long[] csrcList;
    private byte[] data;
    private boolean incrementTimeStamp = true;
    private int srcPort = 0;
    private int destPort = 0;

    public RtpPacket() {

    }

    public RtpPacket(byte[] packet) {
        if (packet.length < 12) {
            System.err.println("Error Decoding: packet too short!");
            return;
        }
        int b = packet[0] & 0xff;
        setVersion((b & 0xc0) >> 6);
        setPadding((b & 0x20) != 0);
        setExtension((b & 0x10) != 0);
        setCsrcCount(b & 0x0f);
        b = packet[1] & 0xff;
//        rtpPacket.setMarker((b & 0x80) != 0);
        setMarker((b & 0x80) == 1);
        setPayloadType(b & 0x7f);
        b = packet[2] & 0xff;
        setSequenceNumber(b * 256 + (packet[3] & 0xff));
        b = packet[4] & 0xff;
        setTimestamp(b * 256 * 256 * 256
                + (packet[5] & 0xff) * 256 * 256
                + (packet[6] & 0xff) * 256
                + (packet[7] & 0xff));
        b = packet[8] & 0xff;
        setSsrc(b * 256 * 256 * 256
                + (packet[9] & 0xff) * 256 * 256
                + (packet[10] & 0xff) * 256
                + (packet[11] & 0xff));
        long[] csrcList = new long[getCsrcCount()];
        for (int i = 0; i < csrcList.length; ++i)
            csrcList[i] = (packet[12 + i] & 0xff) << 24
                    + (packet[12 + i + 1] & 0xff) << 16
                    + (packet[12 + i + 2] & 0xff) << 8
                    + (packet[12 + i + 3] & 0xff);
        setCsrcList(csrcList);
        int dataOffset = 12 + csrcList.length * 4;
        int dataLength = packet.length - dataOffset;
        byte[] data = new byte[dataLength];
        System.arraycopy(packet, dataOffset, data, 0, dataLength);
        setData(data);
    }

    @Override
    public int compareTo(Object rtpPacket) {
        int compareSequenceNumber = ((RtpPacket) rtpPacket).getSequenceNumber();
        /* For Ascending order*/
        int result = this.sequenceNumber - compareSequenceNumber;
        return result;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss dd/MM/yy");
        String time = sdf.format(new Date(timestamp).getTime());
        String printStatment = "[RTP-Header]\n\tVersion: " + version
                + "\n\tPadding: " + padding
                + "\n\tExtension: " + extension
                + "\n\tCC: " + csrcCount
                + "\n\tMarker: " + marker
                + "\n\tPayloadType: " + payloadType
                + "\n\tSequenceNumber: " + sequenceNumber
                + "\n\tTimeStamp: " + timestamp
                + "\n\tData: " + byteArrayToHex(data) + "\n";
        return printStatment;
    }

    private String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }

    public boolean isExtension() {
        return extension;
    }

    public void setExtension(boolean extension) {
        this.extension = extension;
    }

    public int getCsrcCount() {
        return csrcCount;
    }

    public void setCsrcCount(int csrcCount) {
        this.csrcCount = csrcCount;
    }

    public boolean isMarker() {
        return marker;
    }

    public void setMarker(boolean marker) {
        this.marker = marker;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    public long[] getCsrcList() {
        return csrcList;
    }

    public void setCsrcList(long[] csrcList) {
        this.csrcList = csrcList;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isIncrementTimeStamp() {
        return incrementTimeStamp;
    }

    public void setIncrementTimeStamp(boolean incrementTimeStamp) {
        this.incrementTimeStamp = incrementTimeStamp;
    }

    public RtpPacket decode(byte[] packet) {
        if (packet.length < 12) {
            System.err.println("Error Decoding: packet too short!");
            return null;
        }
        RtpPacket rtpPacket = new RtpPacket();
        int b = packet[0] & 0xff;
        rtpPacket.setVersion((b & 0xc0) >> 6);
        rtpPacket.setPadding((b & 0x20) != 0);
        rtpPacket.setExtension((b & 0x10) != 0);
        rtpPacket.setCsrcCount(b & 0x0f);
        b = packet[1] & 0xff;
//        rtpPacket.setMarker((b & 0x80) != 0);
        rtpPacket.setMarker((b & 0x80) == 1);
        rtpPacket.setPayloadType(b & 0x7f);
        b = packet[2] & 0xff;
        rtpPacket.setSequenceNumber(b * 256 + (packet[3] & 0xff));
        b = packet[4] & 0xff;
        rtpPacket.setTimestamp(b * 256 * 256 * 256
                + (packet[5] & 0xff) * 256 * 256
                + (packet[6] & 0xff) * 256
                + (packet[7] & 0xff));
        b = packet[8] & 0xff;
        rtpPacket.setSsrc(b * 256 * 256 * 256
                + (packet[9] & 0xff) * 256 * 256
                + (packet[10] & 0xff) * 256
                + (packet[11] & 0xff));
        long[] csrcList = new long[rtpPacket.getCsrcCount()];
        for (int i = 0; i < csrcList.length; ++i)
            csrcList[i] = (packet[12 + i] & 0xff) << 24
                    + (packet[12 + i + 1] & 0xff) << 16
                    + (packet[12 + i + 2] & 0xff) << 8
                    + (packet[12 + i + 3] & 0xff);
        rtpPacket.setCsrcList(csrcList);
        int dataOffset = 12 + csrcList.length * 4;
        int dataLength = packet.length - dataOffset;
        byte[] data = new byte[dataLength];
        System.arraycopy(packet, dataOffset, data, 0, dataLength);
        rtpPacket.setData(data);
        return rtpPacket;
    }

    //--------------------------
    //print headers without the SSRC
    //--------------------------
    public void printheader() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss dd/MM/yy");
        String time = sdf.format(new Date(timestamp).getTime());
        System.out.println("[RTP-Header] ");
        System.out.println("\tVersion: " + version
                + "\n\tPadding: " + padding
                + "\n\tExtension: " + extension
                + "\n\tCC: " + csrcCount
                + "\n\tMarker: " + marker
                + "\n\tPayloadType: " + payloadType
                + "\n\tSequenceNumber: " + sequenceNumber
                + "\n\tTimeStamp: " + time);

    }
}

