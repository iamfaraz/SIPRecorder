package com.speechanalytics.siprecorder.utils;

import com.speechanalytics.siprecorder.decoder.PcmaDecoder;
import com.speechanalytics.siprecorder.decoder.PcmuDecoder;
import com.speechanalytics.siprecorder.rtp.RtpPacket;
import org.jnetpcap.*;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JRegistry;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.tcpip.Udp;
import org.jnetpcap.protocol.voip.Rtp;
import org.jnetpcap.protocol.voip.Sdp;
import org.jnetpcap.protocol.voip.Sip;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

public class TestVoip {

    private static final String SIP = "C:\\TEMP\\1523809413851.pcap";
//        private static final String SIP = "C:\\TEMP\\1524998608522.pcap";
//    private static final String SIP = "C:\\TEMP\\1523297257058.pcap";
//    private static final String SIP = "D:\\Downloads\\Documents\\IntelliJProjects\\traffic.pcap";

    /**
     * The Constant SIP_G711.
     */
//    private static final String SIP_G711 = "C:\\TEMP\\1523809413851.pcap";
    private static final String SIP_G711 = "C:\\TEMP\\1524998608522.pcap";
//    private static final String SIP_G711 = "C:\\TEMP\\1523297257058.pcap";
//    private static final String SIP_G711 = "D:\\Downloads\\Documents\\IntelliJProjects\\traffic.pcap";

    public static PcapPacket getPcapPacket(final String file, final int index) {

        /***************************************************************************
         * First, open offline file
         **************************************************************************/
        StringBuilder errbuf = new StringBuilder();

        final Pcap pcap = Pcap.openOffline(file, errbuf);
        if (pcap == null) {
            System.err.println(errbuf.toString());
            return null;
        }

        /***************************************************************************
         * Second, setup a packet we're going to copy the captured contents into.
         * Allocate 2K native memory block to hold both state and buffer. Notice
         * that the packet has to be marked "final" in order for the JPacketHandler
         * to be able to access that variable from within the loop.
         **************************************************************************/
        final PcapPacket result = new PcapPacket(2 * 1024);

        /***************************************************************************
         * Third, Enter our loop and count packets until we reach the index of the
         * packet we are looking for.
         **************************************************************************/
        try {
            pcap.loop(Pcap.LOOP_INFINATE, new JBufferHandler<Pcap>() {
                int i = 0;

                public void nextPacket(PcapHeader header, JBuffer buffer, Pcap pcap) {

                    /*********************************************************************
                     * Forth, once we reach our packet transfer the capture data from our
                     * temporary, shared packet, to our preallocated permanent packet. The
                     * method transferStateAndDataTo will do a deep copy of the packet
                     * contents and state to the destination packet. The copy is done
                     * natively with memcpy. The packet content in destination packet is
                     * layout in memory as follows. At the front of the buffer is the
                     * packet_state_t structure followed immediately by the packet data
                     * buffer and its size is adjusted to the exact size of the temporary
                     * buffer. The remainder of the allocated memory block is unused, but
                     * needed to be allocated large enough to hold a decent size packet.
                     * To break out of the Pcap.loop we call Pcap.breakLoop().
                     ********************************************************************/
                    if (i++ == index) {
                        PcapPacket packet = new PcapPacket(header, buffer);
                        packet.scan(JRegistry.mapDLTToId(pcap.datalink()));
                        System.out.println(packet.getState().toDebugString());
                        packet.transferStateAndDataTo(result);

                        pcap.breakloop();
                        return;
                    }
                }

            }, pcap);
        } finally {

            /*************************************************************************
             * Lastly, we close the pcap handle and return our result :)
             ************************************************************************/
            pcap.close();
        }

        return result;
    }

    public static Iterable<PcapPacket> getIterable(final String file) {
        return () -> getPcapPacketIterator(file, 0, Integer.MAX_VALUE);
    }

    public static Iterator<PcapPacket> getPcapPacketIterator(final String file,
                                                             final int start,
                                                             final int end) {
        return getPcapPacketIterator(file, start, end, null);
    }

    public static Iterator<PcapPacket> getPcapPacketIterator(final String file,
                                                             final int start,
                                                             final int end,
                                                             String filter) {

        /***************************************************************************
         * First, open offline file
         **************************************************************************/
        StringBuilder errbuf = new StringBuilder();

        final Pcap pcap = Pcap.openOffline(file, errbuf);

        if (filter != null) {
            PcapBpfProgram prog = new PcapBpfProgram();
            if (pcap.compile(prog, filter, 0, 0xffffff00) != Pcap.OK) {
                System.err.printf("pcap filter %s: %s\n", pcap.getErr(), filter);
                return null;
            }
            pcap.setFilter(prog);
        }

        final Exchanger<PcapPacket> barrier = new Exchanger<PcapPacket>();

        /***************************************************************************
         * Third, Enter our loop and count packets until we reach the index of the
         * packet we are looking for.
         **************************************************************************/

        final PcapTask<Pcap> task = new PcapTask<Pcap>(pcap, end - start, pcap) {

            public void run() {
                try {
                    barrier.exchange(null);
                } catch (InterruptedException e1) {
                }

                this.result = pcap.loop(end - start, new PcapPacketHandler<Pcap>() {
                    int i = 0;

                    public void nextPacket(PcapPacket packet, Pcap pcap) {


                        if (i >= start) {
                            try {
                                barrier.exchange(packet);
                            } catch (InterruptedException e) {
                                throw new IllegalStateException(e);
                            }
                        }

                        i++;
                    }

                }, pcap);

                try {
                    barrier.exchange(null, 1000, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }

        };

        try {
            task.start();
            barrier.exchange(null); // Synchronize startup
        } catch (InterruptedException e1) {
            throw new IllegalStateException(e1);
        }

        return new Iterator<PcapPacket>() {

            PcapPacket packet;

            public boolean hasNext() {
                try {
                    packet = barrier.exchange(null, 1000, TimeUnit.MILLISECONDS);
                    return packet != null;

                } catch (Exception e) {
                    return false;
                }
            }

            public PcapPacket next() {
                return packet;
            }

            public void remove() {
                throw new UnsupportedOperationException(
                        "Invalid operation for readonly offline read");
            }

        };
    }

    private static byte[] addWavHeader(byte[] bytes) {
        ByteBuffer bufferWithHeader = ByteBuffer.allocate(bytes.length + 44);
        bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
        bufferWithHeader.put("RIFF".getBytes());
        bufferWithHeader.putInt(bytes.length + 36);
        bufferWithHeader.put("WAVE".getBytes());
        bufferWithHeader.put("fmt ".getBytes());
        bufferWithHeader.putInt(16);
        bufferWithHeader.putShort((short) 1);
        bufferWithHeader.putShort((short) 1);
        bufferWithHeader.putInt(8000);
        bufferWithHeader.putInt(16000);
        bufferWithHeader.putShort((short) 2);
        bufferWithHeader.putShort((short) 16);
        bufferWithHeader.put("data".getBytes());
        bufferWithHeader.putInt(bytes.length);
        bufferWithHeader.put(bytes);
        return bufferWithHeader.array();
    }

    public void testSip() {
        Sip sip = new Sip();
        Sdp sdp = new Sdp();
        PcapPacket packet = getPcapPacket(SIP, 223 - 1);
        if (packet.hasHeader(sip)) {
            System.out.printf("SIP: %s", sip);

            if (packet.hasHeader(sdp)) {
                System.out.printf("SDP: %s", sdp);

            }
        } else {
            System.out.printf("Packet: " + packet.toString());
        }
    }

    /**
     * Test rtp heuristics.
     */
    public void testRtpHeuristics() {

        JPacket packet = getPcapPacket(SIP_G711, 499 - 1);

        // System.out.println(JRegistry.toDebugString());
        System.out.println(packet.getState().toDebugString());
        System.out.println(packet);
        System.out.flush();

    }

    private void print(Object o) {
        System.out.println(o.toString());
    }

    public void RtpInfoExtract() {
        Rtp rtp = new Rtp();
        Sip sip = new Sip();
        Sdp sdp = new Sdp();
        Udp udp = new Udp();
        Ethernet eth = new Ethernet();
        List<RtpPacket> rtpPacketList = new ArrayList<>();
        try {
            for (PcapPacket packet : getIterable(SIP)) {
                if (packet.hasHeader(rtp) || packet.hasHeader(sip) || packet.hasHeader(sdp)) {
                    if (packet.hasHeader(sip)) {
                        System.out.println(packet.getUTF8String(sip.getOffset(), sip.getLength()));
                        if (sip.hasContent()) {
                            System.out.println(packet.getUTF8String(sdp.getOffset(), sdp.getLength()) + "\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String byteArrayToMac(byte[] mac) {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void RtpAudioExtract() {
        Rtp rtp = new Rtp();

        List<RtpPacket> rtpPackets = new ArrayList<>();
        try {
            for (PcapPacket packet : getIterable(SIP)) {
                if (packet.hasHeader(rtp) && rtp.hasPayload()) {

                    if (rtp.hasPostfix() || rtp.paddingLength() != 0) {
                    }
                    byte[] byteArray = rtp.getPayload();
                    RtpPacket rp = decode(byteArray);
                    rtpPackets.add(rp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ConvertRTPToAudio(rtpPackets);
    }

    private void ConvertRTPToAudio(List<RtpPacket> packets) {

        List<byte[]> byteArrayList = new ArrayList<>();

        for (RtpPacket packet : packets) {
//            CMG711 decoder = new CMG711();
//            byte[] byteArray1 = decoder.decode(packet.getData());


            PcmaDecoder pcmaDecoder = new PcmaDecoder();
            PcmuDecoder pcmuDecoder = new PcmuDecoder();

            byte[] rab = pcmaDecoder.process(packet.getData());
            byte[] rub = pcmuDecoder.process(packet.getData());
            byteArrayList.add(rub);
        }

        byte[] byteArray = null;
        for (byte[] array : byteArrayList) {
            if (byteArray == null)
                byteArray = array;
            else
                byteArray = ConcatByteArray(byteArray, array);
        }

//        try {
//            byteArray = addWavHeader(byteArray);
//            PlayAudio(byteArray);
//        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
//            e.printStackTrace();
//        }

        try {
            try {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
                AudioSystem.getAudioInputStream(byteStream);
            } catch (UnsupportedAudioFileException e) {
//                e.printStackTrace();
                byteArray = addWavHeader(byteArray);
                SimpleDateFormat sdf = new SimpleDateFormat("hh-mm-ss-dd-mm-yyyy");
                String date = sdf.format(new Date(System.currentTimeMillis()));
                WriteByteToAudio(byteArray, new File("C:\\Tester\\" + date + ".wav"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private void ConvertRTP(List<Rtp> packets) throws IOException {

        List<byte[]> byteArrayList = new ArrayList<>();

        for (Rtp packet : packets) {

            PcmaDecoder pcmaDecoder = new PcmaDecoder();
            PcmuDecoder pcmuDecoder = new PcmuDecoder();

            byte[] rab = pcmaDecoder.process(packet.getPayload());
            byte[] rub = pcmuDecoder.process(packet.getPayload());
            byteArrayList.add(rab);
        }
        byte[] byteArray = null;
        for (byte[] array : byteArrayList) {
            if (byteArray == null)
                byteArray = array;
            else
                byteArray = ConcatByteArray(byteArray, array);
        }

        try {
            byteArray = addWavHeader(byteArray);
            PlayAudio(byteArray);
        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }

    public void WriteByteToAudio(byte[] byteArray, File file) throws IOException {
        AudioInputStream source;
        AudioInputStream pcm;
        InputStream b_in = new ByteArrayInputStream(byteArray);
        AudioFormat audioFormat = new AudioFormat(8000f, 16, 1, true, false);
        byte[] audioData = byteArray;
        source = new AudioInputStream(new BufferedInputStream(b_in), audioFormat, byteArray.length / audioFormat.getFrameSize());

        pcm = AudioSystem.getAudioInputStream(audioFormat.getEncoding(), source);
        AudioSystem.write(pcm, AudioFileFormat.Type.WAVE, file);
        source.close();
        pcm.close();
    }

    private void PlayAudio(byte[] byteArray) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        AudioInputStream source;
        InputStream b_in = new ByteArrayInputStream(byteArray);
        source = AudioSystem.getAudioInputStream(new BufferedInputStream(b_in));
        long fileSize = byteArray.length;
        int frameSize = 160;
//        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 8, 1, 2, 50, false);
        AudioFormat audioFormat = new AudioFormat(8000f, 8, 1, false, false);

        byte[] streamBuffer = new byte[1024];

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        while (source.available() > streamBuffer.length) {
            int bytesRead = source.read(streamBuffer);
            int bytesWrote = line.write(streamBuffer, 0, streamBuffer.length);
            assert bytesRead == bytesWrote;
        }
        line.close();
        source.close();
    }

    private byte[] ConcatByteArray(byte[] a, byte[] b) {
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        outputStream.write(a);
//        outputStream.write(b);
        ByteBuffer bb = ByteBuffer.allocate(a.length + b.length);
        bb.put(a);
        bb.put(b);

        byte c[] = bb.array();//toByteArray();
//        byte c[] = outputStream.toByteArray();
        return c;
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
}