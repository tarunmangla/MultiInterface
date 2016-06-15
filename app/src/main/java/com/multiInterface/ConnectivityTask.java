package com.multiInterface;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Created by t-taman on 5/20/2016.
 */
public class ConnectivityTask extends AsyncTask<Context, Void, Integer> {
    private Exception exception;
    public WifiManager wifi;
    public String[] wifis;
    Context context;

    public int enableWifi(WifiManager wifi){
        try {
            if (!wifi.isWifiEnabled()) {
                wifi.setWifiEnabled(true);
            }
        }
        catch (Exception e) {
            this.exception = e;
            return 1;
        }
        return 0;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public int associateWifi(WifiManager wifi) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo;
        Network[] networks = connManager.getAllNetworks();
        Log.i("Number", String.valueOf(networks.length));
        for(int i=0;i<networks.length;i++){
            networkInfo = connManager.getNetworkInfo(networks[i]);
            Log.i("State", String.valueOf(networkInfo.getState()));
            Log.i("Type", networkInfo.getTypeName());
        }
        return 0;
    }

    public Integer doInBackground(Context... contexts) {
        //Check if Wifi is enabled, if not enable WiFi
        context = contexts[0];
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int CHECK_FAILED = enableWifi(wifi);
        if(CHECK_FAILED==1)
            Log.i("WiFi","Could not enable");

        associateWifi(wifi);

        /*if(wifi.isConnected())
        List<ScanResult> wifiScanList = wifi.getScanResults();
        wifis = new String[wifiScanList.size()];
        Log.i("Intent", String.valueOf(wifiScanList.size()));
        doWifiScan(wifi, contexts[0]);
        */
        return 0;
    }


    public void doWifiScan(WifiManager wifi, Context context){
        WifiScanReceiver wifiReceiver = new WifiScanReceiver();
        if(context==null){
            Log.i("CTX","NULL");
        }
        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        Log.i("WiFi","Starting Scan");
        Log.i("Scan",String.valueOf(wifi.startScan()));

    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = wifi.getScanResults();
            wifis = new String[wifiScanList.size()];
            Log.i("Scan", String.valueOf(wifiScanList.size()));
            for(int i = 0; i < wifiScanList.size(); i++){
                wifis[i] = ((wifiScanList.get(i)).toString());
                Log.i("AP",wifis[i]);
            }

        }
    }

}

