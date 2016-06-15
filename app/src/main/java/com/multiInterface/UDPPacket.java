package com.multiInterface;

import android.provider.Settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by t-taman on 6/1/2016.
 */
public class UDPPacket {
    public int type;
    public int burstCount;
    public int packetNum;
    public int outOfOrderNum;
    // Data packet: local timestamp
    // Response packet: jitter
    public long timestamp;
    public int packetSize;
    public int seq;
    public int udpInterval;
    public char clientType;
    public int experimentType;

    /**
     * Create an empty structure

     */
    public UDPPacket() {}

    /**
     * Unpack received message and fill the structure

     * @param rawdata network message
     * @throws MeasurementError stream reader failed
     */
    public UDPPacket(byte[] rawdata)
            throws MeasurementError{
        ByteArrayInputStream byteIn = new ByteArrayInputStream(rawdata);
        DataInputStream dataIn = new DataInputStream(byteIn);

        try {
            type = dataIn.readInt();
            burstCount = dataIn.readInt();
            packetNum = dataIn.readInt();
            outOfOrderNum = dataIn.readInt();
            timestamp = dataIn.readLong();
            packetSize = dataIn.readInt();
            seq = dataIn.readInt();
            udpInterval = dataIn.readInt();
        } catch (IOException e) {
            throw new MeasurementError("Fetch payload failed! " + e.getMessage());
        }

        try {
            byteIn.close();
        } catch (IOException e) {
            throw new MeasurementError("Error closing inputstream!");
        }
    }

    /**
     * Pack the structure to the network message
     * @return the network message in byte[]
     * @throws MeasurementError stream writer failed
     */
    public byte[] getByteArray(int packetSizeBytes) throws MeasurementError {

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);


        int actualDataSize = 0;
        try {
            dataOut.writeInt(type); //4
            dataOut.writeInt(burstCount); //8
            dataOut.writeInt(packetNum); //12
            dataOut.writeInt(outOfOrderNum); //16
            dataOut.writeLong(timestamp); //24
            dataOut.writeInt(packetSize); //28
            dataOut.writeInt(seq); //32
            dataOut.writeInt(udpInterval); //36
            dataOut.writeInt(experimentType); //40
            dataOut.writeChar(clientType); //42
            dataOut.writeChars(Config.ANDROID_ID); //58
            actualDataSize = dataOut.size();
        } catch (IOException e) {
            throw new MeasurementError("Create rawpacket failed! " + e.getMessage());
        }
        for(int i=0;i<packetSizeBytes-actualDataSize;i++) {
            byteOut.write(1);
        }
        byte[] rawPacket = byteOut.toByteArray();

        try {
            byteOut.close();
        } catch (IOException e) {
            throw new MeasurementError("Error closing outputstream!");
        }
        return rawPacket;
    }

}