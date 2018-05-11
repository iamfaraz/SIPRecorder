package com.speechanalytics.siprecorder.utils;

import com.speechanalytics.siprecorder.decoder.PcmaDecoder;
import com.speechanalytics.siprecorder.decoder.PcmuDecoder;
import com.speechanalytics.siprecorder.rtp.RtpPacket;
import org.jnetpcap.protocol.voip.Rtp;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class RTPConverter {

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

    public void ConvertRTPToAudio(List<RtpPacket> packets, File file) {

        List<byte[]> byteArrayList = new ArrayList<>();

        for (RtpPacket packet : packets) {

//            PcmaDecoder pcmaDecoder = new PcmaDecoder();
            PcmuDecoder pcmuDecoder = new PcmuDecoder();

//            byte[] rab = pcmaDecoder.process(packet.getData());
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
        if (byteArray != null)
            try {
                try {
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
                    AudioSystem.getAudioInputStream(byteStream);
                } catch (UnsupportedAudioFileException e) {
//                e.printStackTrace();
                    byteArray = addWavHeader(byteArray);
                    WriteByteToAudio(byteArray, file);
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

    private void WriteByteToAudio(byte[] byteArray, File file) throws IOException {
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

    private RtpPacket decode(byte[] packet) {
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
