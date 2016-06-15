package com.multiInterface;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by t-taman on 6/1/2016.
 */
public class UDPSendTask extends UDPMeasurementTask {

    public DatagramSocket mobileSock;
    public DatagramSocket wifiSock;
    private Thread tWifi;
    private Thread tMobile;
    public UDPResult udpResultMobile;
    public UDPResult udpResultWifi;

    public UDPSendTask(HashMap<String, String> hashMap, Context context) {
        super(hashMap, context);
        int numSockets = 0;
        try {
            numSockets = openBothSockets();
        } catch (MeasurementError measurementError) {
            measurementError.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(numSockets != 2) {
            Logger.i("Failed to open sockets " + numSockets);
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public int openBothSockets() throws MeasurementError, IOException {
        // Open datagram socket
        try {
            wifiSock = new DatagramSocket();
            mobileSock = new DatagramSocket();
        } catch (SocketException e) {
            throw new MeasurementError("Socket creation failed");
        }
        Network mobileNetwork = utility.getNetwork(ConnectivityManager.TYPE_MOBILE, context);
        Network wifiNetwork = utility.getNetwork(ConnectivityManager.TYPE_WIFI, context);

        if(mobileNetwork==null){
            return 0;
        }
        if(wifiNetwork == null){
            return 1;
        }
        mobileNetwork.bindSocket(mobileSock);
        wifiNetwork.bindSocket(wifiSock);
        return 2;
    }

    private class sendPacket implements Runnable {
        public int packetNum;
        public InetAddress addr;
        public char clientType;
        public DatagramSocket sock;

        public sendPacket(int packetNum, InetAddress addr, char clientType, DatagramSocket sock) {
            this.packetNum = packetNum;
            this.addr = addr;
            this.clientType = clientType;
            this.sock = sock;
        }

        public void run() {

            UDPPacket dataPacket = new UDPPacket();
            dataPacket.type = PKT_DATA;
            dataPacket.burstCount = udpBurstCount;
            dataPacket.packetNum = packetNum;
            dataPacket.timestamp = System.currentTimeMillis();
            dataPacket.packetSize = packetSizeByte;
            dataPacket.seq = Config.seq;
            dataPacket.experimentType = experimentType;
            dataPacket.clientType = clientType;
             // Flatten UDP packet
            byte[] data = new byte[0];
            try {
                data = dataPacket.getByteArray(packetSizeByte);
            } catch (MeasurementError measurementError) {
                measurementError.printStackTrace();
            }

            DatagramPacket packet = new DatagramPacket(data, data.length, addr,
                    dstPort);

            try {
                sock.send(packet);
                dataConsumed += packet.getLength();
            } catch (IOException e) {
                try {
                    throw new MeasurementError("Error sending via clientType" + clientType + " to " + target);
                } catch (MeasurementError measurementError) {
                    measurementError.printStackTrace();
                }
            }
        }
    }



    public void SendUDPData() throws MeasurementError, IOException {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(target);
            targetIp = addr.getHostAddress();
        } catch (UnknownHostException e) {
            throw new MeasurementError("Unknown host " + target);
        }

        for (int i = 0; i < udpBurstCount; i++) {

            if(Config.isRunning == 0) {
                return;
            }

            Runnable wifiSend = new sendPacket(i, addr, 'W', wifiSock);
            tWifi = new Thread(wifiSend);

            Runnable mobileSend = new sendPacket(i, addr, 'M', mobileSock);
            tMobile = new Thread(mobileSend);

            tWifi.start();
            tMobile.start();



            try {
                tWifi.join();
                tMobile.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String data = "Sent " + String.valueOf(i + 1) + " packets";
            Config.mobileSendData = data;
            Config.wifiSendData = data;




            try {
                long startTime = System.currentTimeMillis();
                while(true) {
                    long now = System.currentTimeMillis();
                    if(now - startTime >= udpInterval)
                        break;
                }

                //TimeUnit.MILLISECONDS.sleep(udpInterval);
                //Thread.sleep(udpInterval);


               //long endTime = System.currentTimeMillis();
               //Logger.d("Slept " + String.valueOf(endTime-startTime));

            } catch (Exception e) {
                Logger.e("Error: sleep interrupted!");
            }
        } // for()\

    }

    private class receivePacket implements Runnable {
        public DatagramSocket sock;
        char clientType;
        public receivePacket(DatagramSocket sock, char clientType)
        {
            this.sock = sock;
            this.clientType = clientType;
        }

        UDPResult udpResult;

        public UDPResult getUDPResult(){
            return udpResult;
        }

        public void run() {
            udpResult = new UDPResult();

            byte buffer[] = new byte[MIN_PACKETSIZE];
            DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
            try {
                sock.setSoTimeout(RCV_UP_TIMEOUT);
                sock.receive(recvPacket);
            } catch (SocketException e1) {
                sock.close();
                try {
                    throw new MeasurementError("Timed out reading from " + target);
                } catch (MeasurementError measurementError) {
                    measurementError.printStackTrace();
                }
            } catch (IOException e) {
                sock.close();
                try {
                    displayResult("Error reading response", clientType);
                    throw new MeasurementError("Error reading from " + target);
                } catch (MeasurementError measurementError) {
                    measurementError.printStackTrace();
                }
            }
            try {
                UDPPacket responsePacket = new UDPPacket(recvPacket.getData());
                if (responsePacket.type == PKT_RESPONSE) {
                    // Received seq number must be same with client seq
                    if (responsePacket.seq != Config.seq) {
                        Logger.e("Error: Server send response packet with different seq, old "
                                + Config.seq + " => new " + responsePacket.seq);
                        return;
                    }

                    Logger.i("Recv UDP resp from " + target + " type:"
                            + responsePacket.type + " burst:" + responsePacket.burstCount
                            + " pktnum:" + responsePacket.packetNum + " out_of_order_num: "
                            + responsePacket.outOfOrderNum + " jitter: " + responsePacket.timestamp);

                    udpResult.packetCount = responsePacket.packetNum;
                    udpResult.outOfOrderRatio =
                            (double) responsePacket.outOfOrderNum / responsePacket.packetNum;
                    udpResult.jitter = responsePacket.timestamp;
                    String data = "Send Result: Server received " + udpResult.packetCount + " packets";
                    displayResult(data, clientType);

                }
            } catch (MeasurementError measurementError) {
                measurementError.printStackTrace();
            }
        }

    }

    private void recvUpResponse() throws MeasurementError, InterruptedException {
        // Receive response
        Logger.i("Waiting for UDP response from " + target + ": "
                + targetIp);

        Runnable wifiReceive = new receivePacket(wifiSock, 'W');
        Runnable mobileReceive = new receivePacket(mobileSock, 'M');
        tWifi = new Thread(wifiReceive);
        tMobile = new Thread(mobileReceive);

        tWifi.start();
        tMobile.start();

        tWifi.join();
        tMobile.join();

        return;

    }

    public void displayResult(final String data, char clientType) {
        if (clientType == 'W') {
            Config.wifiSendData = data;
        } else {
            Config.mobileSendData = data;
        }
    }





    public void sendUDPBurst(){
        try {
            SendUDPData();
        } catch (MeasurementError measurementError) {
            measurementError.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(Config.isRunning == 1) {

            try {
                recvUpResponse();
                //Logger.i(String.valueOf(udpResult.jitter) + ' ' + String.valueOf(udpResult.outOfOrderRatio) + ' ' + String.valueOf(udpResult.packetCount));
            } catch (MeasurementError measurementError) {
                measurementError.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
