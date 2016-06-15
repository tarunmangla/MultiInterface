package com.multiInterface;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.CellSignalStrength;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by t-taman on 5/31/2016.
 */



public class UDPAsyncTask extends AsyncTask<Context, Integer, Long> {

    private Thread tWifi = null;
    private Thread tMobile = null;
    private static int NUMRETRY = 3;
    public HashMap<String, String> hashMapWifi = new HashMap<>();
    public HashMap<String, String> hashMapMobile = new HashMap<>();

    public long delayMsec = 10*1000;


    Timer timer = new Timer();

    UDPReceiveTask udpReceiveTaskWifi;
    UDPReceiveTask udpReceiveTaskMobile;
    UDPSendTask udpSendTask;
    NetworkMetricTask networkMetricTask;
    ScheduledExecutorService scheduledExecutorService;

    Context context;
    Utility utility;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean isActive(Integer type){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();
        NetworkInfo networkInfo;
        for (Network mNetwork : networks) {
            networkInfo = connManager.getNetworkInfo(mNetwork);
            if (networkInfo.getType() == type) {
                Logger.d("Network Found : " + networkInfo.getTypeName());
                return true;
            }
        }
        return false;
    }




    public void initReceiveThreads() {
        tWifi = new Thread(new Runnable() {
            @Override
            public void run() {
                udpReceiveTaskWifi = new UDPReceiveTask(hashMapWifi, context);
                udpReceiveTaskWifi.recvUDPBurst();
            }
        });

        tMobile = new Thread(new Runnable() {
            @Override
            public void run() {
                hashMapMobile.put("networkType", String.valueOf(ConnectivityManager.TYPE_MOBILE));
                udpReceiveTaskMobile = new UDPReceiveTask(hashMapMobile, context);
                udpReceiveTaskMobile.recvUDPBurst();
            }
        });
    }

    public boolean areBothActive() {
        return (isActive(ConnectivityManager.TYPE_MOBILE) && isActive(ConnectivityManager.TYPE_WIFI));
    }


    private class SendTestPacket{
        public int packetNum = 1;
        public InetAddress addr;
        public char clientType;
        public DatagramSocket sock;
        public int packetSizeByte = 100;
        public SendTestPacket(String target, char clientType, DatagramSocket sock) throws UnknownHostException {
            this.addr = InetAddress.getByName(target);
            this.clientType = clientType;
            this.sock = sock;
        }

        public void sendPacket() {

            UDPPacket dataPacket = new UDPPacket();
            dataPacket.type = UDPMeasurementTask.PKT_TEST;
            dataPacket.burstCount = 5;
            dataPacket.packetNum = packetNum;
            dataPacket.timestamp = System.currentTimeMillis();
            dataPacket.packetSize = packetSizeByte;
            dataPacket.seq = 1;
            dataPacket.clientType = clientType;
            dataPacket.experimentType = -1;

            // Flatten UDP packet
            byte[] data = new byte[0];
            try {
                data = dataPacket.getByteArray(packetSizeByte);
            } catch (MeasurementError measurementError) {
                measurementError.printStackTrace();
            }

            DatagramPacket packet = new DatagramPacket(data, data.length, addr,
                    UDPMeasurementTask.DEFAULT_PORT);

            try {
                sock.send(packet);
            } catch (IOException e) {
                sock.close();
                try {
                    throw new MeasurementError("Error sending " + addr);
                } catch (MeasurementError measurementError) {
                    measurementError.printStackTrace();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public MeasurementParams getCurrentParams() {
        MeasurementParams measurementParams = new MeasurementParams();
        measurementParams.timeStamp = System.currentTimeMillis();
        measurementParams.cellID = utility.getCellId(context);
        WifiManager wifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        measurementParams.wifiID= wifiInfo.getBSSID();
        measurementParams.wifiMedianRSSI = wifiInfo.getRssi();
        CellSignalStrength cellSignalStrength = utility.getCellSignalStrength(context);
        int cellRSSI = -1;
        if (cellSignalStrength!=null) {
            cellRSSI = cellSignalStrength.getDbm();
        }

        measurementParams.cellMedianRSSI = cellRSSI;
        return measurementParams;
    }

    public boolean differentParams() {
        MeasurementParams currentParams = getCurrentParams();
        MeasurementParams pastParams;
        Logger.d("Debug: Measurement Params size " + Config.measurementParamsList.size());
        Logger.d("Current Measurement Params " + currentParams.toString());

        //Hack for 5 different measurements
        if(Config.measurementParamsList.size() < 5)
            return true;

        for(int i = 0; i < Config.measurementParamsList.size(); i++) {
            pastParams = Config.measurementParamsList.get(i);
            Logger.d("Past Measurement Params " + pastParams.toString());
            if(currentParams.timeStamp-pastParams.timeStamp > Config.THRESHOLD) {
                Logger.d("Measurement Params timer");
                continue;
            }
            else if(!currentParams.wifiID.equals(pastParams.wifiID)) {
                Logger.d("Measurement Params wifi-ID");
                continue;
            }
            else if(!currentParams.cellID.equals(pastParams.cellID)) {
                Logger.d("Measurement Params cellID");

                continue;
            }
            else if(Math.abs(currentParams.wifiMedianRSSI - pastParams.wifiMedianRSSI) > Config.WIFI_RSSI_THRESHOLD) {
                Logger.d("Measurement Params wifiRSSI");

                continue;
            }
            else if(Math.abs(currentParams.cellMedianRSSI - pastParams.cellMedianRSSI) > Config.MOBILE_RSSI_THRESHOLD) {
                Logger.d("Measurement Params cellRSSI");

                continue;
            }
            else {
                return false;
            }
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public int checkWifiUDP() {
                /*Code for basic test if UDP packets can be sent*/
        if(isActive(ConnectivityManager.TYPE_WIFI)) {
            int i = 0;
            try {
                DatagramSocket wifiSock = new DatagramSocket();
                Network wifiNetwork = utility.getNetwork(ConnectivityManager.TYPE_WIFI, context);
                if (wifiNetwork != null) {
                    wifiNetwork.bindSocket(wifiSock);
                }

                for (i = 0; i < NUMRETRY; i++) {
                    new SendTestPacket(UDPMeasurementTask.target, 'W', wifiSock).sendPacket();
                    try {
                        wifiSock.setSoTimeout(UDPMeasurementTask.RCV_DOWN_TIMEOUT);
                        byte buffer[] = new byte[100];
                        DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
                        wifiSock.receive(recvPacket);
                        break;
                    } catch (IOException e) {

                        Config.progressData = "wifi UDP test failed : " + i + " time";
                        continue;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (i == NUMRETRY) {
                Config.progressData = "Unable to send/receive UDP packets on this network";
            } else {
                Config.progressData = "wifi UDP works";
                return 1;
            }
        }
        else {
            Config.progressData = "Wifi data not active";
        }
        return 0;
    }

    public void printActiveNetwork() {
        if(isActive(ConnectivityManager.TYPE_MOBILE)) {
            Config.progressData = "only mobile active";
        }
        else if (isActive(ConnectivityManager.TYPE_WIFI)){
            Config.progressData = "only wifi active";
        }
        else {
            Config.progressData = "both interfaces inactive";
        }
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    protected Long doInBackground(Context... contexts) {
        Config.progressData = "Started";

        utility = new Utility();


        context = contexts[0];
        if(context == null)
            Logger.i("Context is Null!! ");




       // timer.schedule(new TimerTask() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                Logger.d("Debug: Started " + new Date());
                long timeNow = System.currentTimeMillis();
                Config.seq = (int) TimeUnit.MILLISECONDS.toSeconds(timeNow);
                networkMetricTask = new NetworkMetricTask();
                Config.isWaiting = 0;
                Config.isClockRunning = 0;

                while (true) {

                    if (areBothActive()) {

                        if(checkWifiUDP() == 0){
                            Logger.d("Debug: UDP Not working");
                        }
                        else if(!differentParams()) {
                            Logger.d("Debug: Skipped Measurement");
                            Config.numSkips ++;
                            Config.progressData = "Skipped Measurement " + Config.numSkips + " time";
                        }
                        else {
                            Logger.d("Debug: Measurement Started");
                            Config.isMeasurementRunning = 1;
                            Config.numSkips = 0;

                            networkMetricTask.execute(context);

                            Config.progressData = "Sending data";
                            udpSendTask = new UDPSendTask(hashMapWifi, context);
                            udpSendTask.sendUDPBurst();
                            Logger.d("Debug: UDP Burst Sent");
                            Config.progressData = "Finished Sending data";

                            if(Config.isRunning == 0) {
                                break;
                            }
                            else {
                                Config.progressData = "Receiving data";
                            }

                            initReceiveThreads();
                            tWifi.start();
                            tMobile.start();
                            try {
                                tWifi.join();
                                tMobile.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Logger.d("Debug: Received Data");
                            Config.progressData = "Finished Receiving data";
                            Config.isMeasurementRunning = 0;
                        }
                        break;
                    }
                    else
                    {
                        Logger.d("Debug: Network not active");
                        printActiveNetwork();
                        try
                        {
                            Thread.sleep(5 * 1000);
                            if(Config.isRunning == 0) {
                                break;
                            }
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } //else
                } //while loop

                if(Config.isRunning == 1)
                    Config.isWaiting = 1;


                Logger.d("Debug: Ended " + new Date());

            }
        }, delayMsec, Config.periodMsec, TimeUnit.MILLISECONDS);

        return 0L;
    }



    @Override
    protected void onCancelled() {
        super.onCancelled();

        scheduledExecutorService.shutdown();



        try {
            if (!scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS)){
                Logger.d("Problem stooping");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        Config.wifiSendData = " ";
        Config.mobileSendData = " ";
        Config.wifiReceiveData = " ";
        Config.mobileReceiveData = " ";
        Config.progressData = "Stopped!";
        Config.isMeasurementRunning = 0;
        Config.isWaiting = 0;


    }

}