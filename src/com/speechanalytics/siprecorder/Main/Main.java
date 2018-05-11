package com.speechanalytics.siprecorder.Main;


import com.speechanalytics.siprecorder.rtp.RtpPacket;
import com.speechanalytics.siprecorder.sdp.MediaDescription;
import com.speechanalytics.siprecorder.sdp.SdpParser;
import com.speechanalytics.siprecorder.sdp.SessionDescription;
import com.speechanalytics.siprecorder.sip.*;
import com.speechanalytics.siprecorder.utils.RFC3261;
import org.jnetpcap.*;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.JFormatter;
import org.jnetpcap.packet.format.TextFormatter;
import org.jnetpcap.protocol.tcpip.Udp;
import org.jnetpcap.protocol.voip.Rtp;
import org.jnetpcap.protocol.voip.Sdp;
import org.jnetpcap.protocol.voip.Sip;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    JFormatter out = new TextFormatter(new StringBuilder());

    public static void main(String[] args) {

        List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs
        StringBuilder errbuf = new StringBuilder(); // For any error msgs

        //region Selecting Device from the list
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf
                    .toString());
            return;
        }

        System.out.println("\n\nNetwork devices found:");

        int i = 0;
        String ipAddr = "";
        for (PcapIf device : alldevs) {
            String description =
                    (device.getDescription() != null) ? device.getDescription()
                            : "No description available";
            String addr = device.getAddresses().get(0).getAddr().toString();
            ipAddr = addr.substring(addr.indexOf(':') + 1, addr.indexOf(']'));
            System.out.printf("#%d: %s [%s] [%s]\n", i++, device.getName(), description, ipAddr);
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Choose the Network Device: ");
        int deviceIndex = scanner.nextInt();

        PcapIf device = alldevs.get(deviceIndex); // We know we have atleast 1 device
        String addr = device.getAddresses().get(0).getAddr().toString();
        ipAddr = addr.substring(addr.indexOf(':') + 1, addr.indexOf(']'));

        System.out.printf("\nChoosing Device '%s' on %s:\n", device.getDescription(), ipAddr);
        //endregion

        //region Second we open up the selected device
        int snaplen = 64 * 1024;           // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000;           // 10 seconds in millis
        Pcap pcap = Pcap.openLive(device.getName(), snaplen, flags, timeout, errbuf);

        if (pcap == null) {
            System.err.printf("Error while opening device for capture: "
                    + errbuf.toString());
            return;
        }


        //endregion

        //region Creating Text file for saving packets strings
        long fileName = System.currentTimeMillis();
//        File file = new File("C:\\temp\\" + fileName + ".txt");
//        FileOutputStream fos;
//        fos = new FileOutputStream(file);
        //endregion

        JBufferHandler<PcapDumper> jphandler = new JBufferHandler<PcapDumper>() {

            @Override
            public void nextPacket(PcapHeader header, JBuffer buffer, PcapDumper dumper) {
                dumper.dump(header, buffer);
            }
        };

        SipSessions sessions = new SipSessions();

        //region Packet Types
        Rtp rtp = new Rtp();
        Sip sip = new Sip();
        Sdp sdp = new Sdp();
        Udp udp = new Udp();
        //endregion


        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {
                if (packet.hasHeader(rtp) || packet.hasHeader(sip) || packet.hasHeader(sdp)) {
                    int source = 0;
                    int destination = 0;
                    boolean hasSdp = false;
                    int rtpPort = 0;

                    //region UDP Packets Received
                    if (packet.hasHeader(udp)) {
                        source = udp.source();
                        destination = udp.destination();
                    }
                    //endregion

                    //region SDP Packets Received
                    try {
                        if (packet.hasHeader(sdp)) {
                            byte[] sdpBytes = sdp.getByteArray(0, sdp.getLength());
                            SdpParser sdpParser = new SdpParser();
                            SessionDescription sd = null;
                            boolean nullThrown = false;
                            if (sdpBytes != null) {
                                try {
                                    sd = sdpParser.parse(sdpBytes);
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                } catch (NullPointerException npe) {
                                    nullThrown = true;
                                    npe.printStackTrace();
                                }
                            }
                            if (sd != null && !nullThrown) {
                                List<MediaDescription> mdList = sd.getMediaDescriptions();
                                int i = 0;
                                for (MediaDescription md : mdList) {
                                    if (md.getPort() != 0) {
                                        hasSdp = true;
                                        rtpPort = md.getPort();
                                    }
                                }
                            } else {
                                print("Error:\n" + new String(packet.getByteArray(sdp.getOffset(), sdp.size())), true);
                            }
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        System.err.println("StringIndexOutOfBoundsException:\n" + new String(packet.getByteArray(sdp.getOffset(), sdp.size())));
                        e.printStackTrace();
                    }
                    //endregion

                    //region SIP Packets Received
                    if (packet.hasHeader(sip)) {
                        byte[] sipbytes = packet.getByteArray(sip.getOffset(), sip.getLength());
                        SipParser parser = new SipParser();
                        SipMessage sipMessage = null;
                        try {
                            sipMessage = parser.parse(sipbytes);
                        } catch (IOException io) {
                            io.printStackTrace();
                        } catch (SipParserException e) {
                            System.err.println(new String(packet.getByteArray(sip.getOffset(), sip.getLength())));
                            e.printStackTrace();
                        }


                        SipPacket sipPacket = new SipPacket(sipMessage, source, destination);
                        if (hasSdp)
                            sipPacket.setRtpPort(rtpPort);
                        String method = sipPacket.getMethod();
//                        print(sipPacket.toString());
                        if (!sipPacket.getHeaderFieldValue(RFC3261.HDR_TO).
                                equals(sipPacket.getHeaderFieldValue(RFC3261.HDR_FROM))) {
                            SipSession session = sessions.getSession(sipPacket);
                            if (session == null) {
                                session = new SipSession(sipPacket);
                                print("New Session started:\n" + session.getCALL_ID());
                            } else {
                                session.addSipPacket(sipPacket);
                                print(session.getSipPackets().size() + " packet added into " + session.getCALL_ID() + " on " + session.getRtpPort());

                            }

                            sessions.addSession(session);
                        }

                    }
                    //endregion

                    //region RTP Packets Received
                    if (packet.hasHeader(rtp)) {
                        byte[] byteArray = rtp.getPayload();
                        RtpPacket rp = new RtpPacket(byteArray);
                        rp.setSrcPort(source);
                        rp.setDestPort(destination);

                        SipSession session = sessions.getSession(rp);
                        if (session != null) {
                            session.addRtpPacket(rp);
                            print(session.getRtpPackets().size() + " packet added into " + session.getCALL_ID() + " on " + session.getRtpPort());
                        } else {
                            print("Null Session found on Dest: " + rp.getDestPort() + " Src: " + rp.getSrcPort() + "\n\n", true);
                        }
                    }
                    //endregion

                }
            }
        };

        PacketReader pReader = new PacketReader(pcap, jpacketHandler);
        Thread readerThread = new Thread(pReader);
        readerThread.start();
    }


    public static void WriteToFile(FileOutputStream fos, String msg)
            throws IOException {
        byte[] strToBytes = msg.getBytes();
        fos.write(strToBytes);
    }

    private static void print(Object message) {
        System.out.println(message.toString());
    }

    private static void print(Object message, boolean isErrorMsg) {
        if (isErrorMsg)
            System.err.println(message.toString());
        else
            print(message);
    }

    private static void print(byte[] bytes) {
        System.out.println(new String(bytes) + "\n");
    }

}

class PacketReader implements Runnable {

    public Pcap pcap;
    public PcapPacketHandler<String> handler;

    public PacketReader(Pcap pcap, PcapPacketHandler<String> handler) {
        this.pcap = pcap;
        this.handler = handler;
    }

    public void run() {
        pcap.loop(Integer.MAX_VALUE, handler, "Faraz");
    }


}

class PacketDumper implements Runnable {

    public Pcap pcap;
    public JBufferHandler<PcapDumper> handler;
    PcapDumper dumper;

    public PacketDumper(Pcap pcap, JBufferHandler<PcapDumper> handler, String fileName) {
        String ofile = "C:\\temp\\" + fileName + ".pcap";
        dumper = pcap.dumpOpen(ofile);
        this.pcap = pcap;
        this.handler = handler;
    }

    public void run() {
        pcap.loop(Integer.MAX_VALUE, handler, dumper);
    }


}
