package com.speechanalytics.siprecorder.decoder;

public abstract class Decoder {

    public abstract byte[] process(byte[] media);

}