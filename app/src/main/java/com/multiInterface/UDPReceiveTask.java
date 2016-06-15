package com.multiInterface;

import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Created by t-taman on 6/1/2016.
 */
public class UDPReceiveTask extends UDPMeasurementTask {

    public UDPReceiveTask(HashMap<String, String> hashMap, Context context){
        super(hashMap, context);
    }

    private DatagramSocket sendDownRequest() throws MeasurementError, IOException {
        DatagramPacket packet;
        InetAddress addr = null;

        // Resolve the server's name
        try {
            addr = InetAddress.getByName(target);
            targetIp = addr.getHostAddress();
        } catch (UnknownHostException e) {
            throw new MeasurementError("Unknown host " + target);
        }

        DatagramSocket sock = null;
        sock = openSocket();
        if(sock == null) {
            Config.mobileReceiveData = "Unable to open socket..Skipping this interface";
            return null;
        }

        Logger.i("Requesting UDP burst:" + udpBurstCount + " pktsize: "
                + packetSizeByte + " to " + target + ": " + targetIp);

        UDPPacket requestPacket = new UDPPacket();
        requestPacket.type = PKT_REQUEST;
        requestPacket.burstCount = udpBurstCount;
        requestPacket.packetSize = packetSizeByte;
        requestPacket.seq = Config.seq;
        requestPacket.udpInterval = udpInterval;
        requestPacket.experimentType = experimentType;
        requestPacket.clientType = clientType;
        // Flatten UDP packet
        byte[] data = requestPacket.getByteArray(packetSizeByte);
        packet = new DatagramPacket(data, data.length, addr, dstPort);

        try {
            sock.send(packet);
            dataConsumed += packet.getLength();
        } catch (IOException e) {
            sock.close();
            throw new MeasurementError("Error sending " + target);
        }

        return sock;
    }

    private UDPResult recvDownResponse(DatagramSocket sock)
            throws MeasurementError, IOException {
        int pktRecv = 0;

        // Reconstruct UDP packet from flattened network data
        byte buffer[] = new byte[packetSizeByte];
        DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
        MetricCalculator metricCalculator = new MetricCalculator(udpBurstCount, Config.seq , clientType, context, experimentType);
        for (int i = 0; i < udpBurstCount; i++) {

            if(Config.isRunning == 0) {
                return null;
            }

            try {
                if(sock == null) {
                    return null;
                }
                sock.setSoTimeout(RCV_DOWN_TIMEOUT);
                sock.receive(recvPacket);

                numRetry = 0;
                numTimeouts = 0;

            } catch (IOException e) {

                Logger.i(numRetry + " " + numTimeouts);
                numTimeouts++;
                if(numTimeouts >= MAX_TIMOUTS){
                    numRetry++;
                    if(numRetry >= MAX_RETRY) {
                        break;
                    }
                    numTimeouts = 0;
                }
                Logger.i("TimeOuted");
                continue;
            }

            dataConsumed += recvPacket.getLength();

            UDPPacket dataPacket = new UDPPacket(recvPacket.getData());
            if (dataPacket.type == PKT_DATA) {
                // Received seq number must be same with client seq
                if ( dataPacket.seq != Config.seq ) {
                    Logger.e("Error: Server send data packets with different seq, old "
                            + Config.seq + " => new " + dataPacket.seq);
                    break;
                }
                /*
                Logger.i("Recv UDP response from " + target + " type:"
                        + dataPacket.type + " burst:" + dataPacket.burstCount + " pktnum:"
                        + dataPacket.packetNum + " timestamp:" + dataPacket.timestamp);
                */

                pktRecv++;
                metricCalculator.addPacket(dataPacket.packetNum, dataPacket.timestamp);

                String data = "Receiver result: Received "  + pktRecv + " packets";
                if(clientType == 'M')
                    Config.mobileReceiveData = data;
                else
                    Config.wifiReceiveData = data;

                if(dataPacket.packetNum == udpBurstCount){
                    break;
                }
            }
            else {
                throw new MeasurementError("Error: not a data packet! seq: " + Config.seq);
            }
        } // for()

        UDPResult udpResult = new UDPResult();
        udpResult.packetCount = pktRecv;
        udpResult.outOfOrderRatio = metricCalculator.calculateOutOfOrderRatio();
        udpResult.jitter = metricCalculator.calculateJitter();

        metricCalculator.logInfo();
        return udpResult;
    }

    public void recvUDPBurst(){
        DatagramSocket sock = null;
        Logger.i("Receive UDP burst");

        if(Config.isRunning == 0)
            return;

        try {
            sock = sendDownRequest();
            if(sock == null) {
                Logger.i("socket is null");
                return;
            }
        } catch (MeasurementError measurementError) {
            measurementError.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            UDPResult udpResult = recvDownResponse(sock);
            if(udpResult!=null) {
                Logger.i(String.valueOf(udpResult.jitter) + ' ' + String.valueOf(udpResult.outOfOrderRatio) + ' ' + String.valueOf(udpResult.packetCount));
            }
        } catch (MeasurementError measurementError) {
            measurementError.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
