package com.multiInterface;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Created by t-taman on 5/24/2016.
 */
public class UDPMeasurementTask {
    public static final int DEFAULT_PORT = 10101;
    public static final int MIN_PACKETSIZE = 36;
    public static final int DEFAULT_UDP_PACKET_SIZE = 160;
    public static final int DEFAULT_UDP_BURST_PPM = 50*60; //50*60; //50;//
    public static final int DEFAULT_MINS = 2;
    public static final int DEFAULT_UDP_INTERVAL = 20; //in millesecond
    public static final int RCV_UP_TIMEOUT = 4000; // round-trip delay, in msec.
    public static final int RCV_DOWN_TIMEOUT = 2000; // one-way delay, in msec
    public static final int PKT_ERROR = 1;
    public static final int PKT_RESPONSE = 2;
    public static final int PKT_DATA = 3;
    public static final int PKT_REQUEST = 4;
    public static final int PKT_TEST = 5;
    public String targetIp = null;
    public static int seq = 1;
    public long dataConsumed = 0;
    public char clientType = 'W';

    public static String target = "191.237.81.137";
    public int udpBurstCount = DEFAULT_UDP_BURST_PPM*DEFAULT_MINS;
    public int packetSizeByte = DEFAULT_UDP_PACKET_SIZE;
    public int dstPort = DEFAULT_PORT;
    public int udpInterval = DEFAULT_UDP_INTERVAL;
    public static int MAX_TIMOUTS = 3;
    public static int MAX_RETRY = 3;
    public int numRetry;
    public int numTimeouts;

    public int type = ConnectivityManager.TYPE_WIFI;
    public int experimentType = 0;
    public TextView textView;
    public Context context;
    public Utility utility;






    public UDPMeasurementTask(HashMap<String, String> hashMap, Context context){
        Logger.i("HashMap " + String.valueOf(hashMap.keySet().size()));
        if(hashMap.containsKey("packetCount")){
            this.udpBurstCount = Integer.parseInt(hashMap.get("packetCount"));
        }
        if (hashMap.containsKey("packetSize")){
            this.packetSizeByte = Integer.parseInt(hashMap.get("packetSize"));
        }
        if (hashMap.containsKey("networkType")){
            this.type = Integer.parseInt(hashMap.get("networkType"));
            Logger.i("TYPE: " + type);
            if(type == ConnectivityManager.TYPE_MOBILE) {
                this.clientType = 'M';
            }
        }
        if (hashMap.containsKey("experimentType")) {
            experimentType = Integer.parseInt(hashMap.get("experimentType"));
        }
        //TODO: Add some sanity checks on parameter values
        this.context = context;
        numRetry = 0;
        numTimeouts = 0;
        utility = new Utility();
        //udpBurstCount = 1000;

    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public DatagramSocket openSocket() throws MeasurementError, IOException {
        DatagramSocket sock = null;

        // Open datagram socket
        try {
            sock = new DatagramSocket();
        } catch (SocketException e) {
            throw new MeasurementError("Socket creation failed");
        }
        Network network = utility.getNetwork(type, context);
        if(network==null){
            return null;
        }
        network.bindSocket(sock);
        return sock;
    }



    public class UDPResult {
        public int packetCount;
        public double outOfOrderRatio;
        public long jitter;

        public UDPResult () {
            packetCount = 0;
            outOfOrderRatio = 0.0;
            jitter = 0L;
        }
    }










}

