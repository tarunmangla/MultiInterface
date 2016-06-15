package com.multiInterface;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.multiInterface.upload.MultipartUploadRequest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Median;

/**
 * Created by tarunmangla on 6/5/16.
 */
public class NetworkMetricTask extends AsyncTask <Context,Integer, Long>  {
    Context context;
    int curSeconds = 0;
    int interval = 20;
    public Utility utility;
    FileOutputStream fOut;



    public void addMeasurementParams(WiFiMetric wiFiMetric, MobileMetric mobileMetric) throws IOException {
        MeasurementParams measurementParams = new MeasurementParams();
        measurementParams.timeStamp = System.currentTimeMillis();
        measurementParams.cellID = mobileMetric.cellIdStr;
        measurementParams.wifiID = wiFiMetric.bSSID;

        int [] mobileSignalStrengthArray = new int[mobileMetric.signalStrengthList.size()];

        for(int i=0; i < mobileMetric.signalStrengthList.size(); i++) {
            mobileSignalStrengthArray[i] = mobileMetric.signalStrengthList.get(i);
        }

        int [] wifiSignalStrengthArray = new int[wiFiMetric.rSSIList.size()];

        for(int i=0; i < wiFiMetric.rSSIList.size(); i++) {
            wifiSignalStrengthArray[i] = wiFiMetric.rSSIList.get(i);
        }


        measurementParams.cellMedianRSSI = utility.getMedian(mobileSignalStrengthArray);
        measurementParams.wifiMedianRSSI = utility.getMedian(wifiSignalStrengthArray);
        Config.measurementParamsList.add(measurementParams);
        Logger.d("Added to list " + Config.measurementParamsList.size());

        //write to file
        fOut = context.openFileOutput(Config.measurementParamsFile, Context.MODE_APPEND);
        ObjectOutputStream oOut = new ObjectOutputStream(fOut);
        oOut.writeObject(measurementParams);
    }



    private class WiFiMetric {
        public String bSSID;
        public String sSID;
        public int frequency;
        public ArrayList<Integer> linkSpeedList;
        public ArrayList<Integer> rSSIList;
        public ArrayList<String> timeStampList;
        public WiFiMetric() {
            linkSpeedList = new ArrayList<Integer>();
            rSSIList = new ArrayList<Integer>();
            timeStampList = new ArrayList<String>();
        }


    }




    private class MobileMetric {
        public String deviceId;
        public String networkOperatorName;
        public String cellIdStr;
        public ArrayList<Integer> signalStrengthList;
        public ArrayList<Integer> asuLevelList;
        public ArrayList<String> timeStampList;
        public ArrayList<String> networkTypeList;
        public ArrayList<Integer> rsrpList;
        public ArrayList<Integer> rsrqList;

        public MobileMetric() {
            signalStrengthList = new ArrayList<Integer>();
            asuLevelList = new ArrayList<Integer>();
            networkTypeList = new ArrayList<String>();
            timeStampList = new ArrayList<String>();
        }

    }


    public void writeToFile(WiFiMetric wiFiMetric, MobileMetric mobileMetric) throws IOException {
        Logger.d("Started Writing");
        String fileDir = context.getApplicationContext().getFilesDir().getAbsolutePath();
        String id = Config.ANDROID_ID + '_' + String.valueOf(Config.seq) + "_" + Config.VERSION;
        String wifiFileName = id + "_net_W" ;
        String mobileFileName = id + "_net_M" ;


        FileOutputStream mobileFileOs = context.openFileOutput(mobileFileName, Context.MODE_PRIVATE);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(mobileFileOs);

        outputStreamWriter.write(mobileMetric.deviceId+ "\t" + mobileMetric.networkOperatorName +"\n");

        for(int i = 0; i < mobileMetric.signalStrengthList.size(); i++) {
            String timeStamp = mobileMetric.timeStampList.get(i);
            String rssi = String.valueOf(mobileMetric.signalStrengthList.get(i));
            String asu = String.valueOf(mobileMetric.asuLevelList.get(i));
            String type = mobileMetric.networkTypeList.get(i);
            outputStreamWriter.write(timeStamp + "\t" + type + "\t" +  rssi+"\t"+asu+"\n");
        }
        outputStreamWriter.close();
        System.out.println("Wrote: " + mobileFileName);
        utility.uploadFile(fileDir + '/' + mobileFileName, context);

        FileOutputStream wifiFileOs = context.openFileOutput(wifiFileName, Context.MODE_PRIVATE);
        outputStreamWriter = new OutputStreamWriter(wifiFileOs);
        outputStreamWriter.write(wiFiMetric.bSSID+ "\t" + wiFiMetric.sSID+ "\t" + wiFiMetric.frequency + "\n");
        for(int i=0; i < wiFiMetric.rSSIList.size(); i++) {
            String timeStamp = wiFiMetric.timeStampList.get(i);
            String rssi = String.valueOf(wiFiMetric.rSSIList.get(i));
            String linkSpeed = String.valueOf(wiFiMetric.linkSpeedList.get(i));
            outputStreamWriter.write(timeStamp + "\t" + rssi+"\t"+linkSpeed+"\n");
        }

        outputStreamWriter.close();
        System.out.println("Wrote: " + mobileFileName);

        utility.uploadFile(fileDir + '/' + wifiFileName, context);
    }




    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected Long doInBackground(Context... contexts) {
        //BSSID, RSSI, LinkSpeed, Wifi APS ?
        Logger.d("Started Logging Network Metrics");


        context = contexts[0];
        utility = new Utility();

        WiFiMetric wiFiMetric = new WiFiMetric();
        WifiManager wifiManager = (WifiManager)
                context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        wiFiMetric.bSSID = wifiInfo.getBSSID();
        wiFiMetric.sSID  = wifiInfo.getSSID();
        wiFiMetric.frequency = wifiInfo.getFrequency();

        MobileMetric mobileMetric = new MobileMetric();
        if(Config.locationPermissionGranted == 1 && Config.phoneStatePermissionGranted == 1) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            mobileMetric.deviceId = telephonyManager.getDeviceId();
            mobileMetric.networkOperatorName = telephonyManager.getNetworkOperatorName();
            mobileMetric.cellIdStr = utility.getCellId(context);
        }
        else {
            Logger.d("Boo! Permission not granted");
        }


        while(Config.isMeasurementRunning==1) {

            wifiInfo = wifiManager.getConnectionInfo();
            wiFiMetric.timeStampList.add(String.valueOf(System.currentTimeMillis()));
            wiFiMetric.linkSpeedList.add(wifiInfo.getLinkSpeed());
            wiFiMetric.rSSIList.add(wifiInfo.getRssi());


            if(Config.locationPermissionGranted == 1 && Config.phoneStatePermissionGranted == 1) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                CellSignalStrength signalStrength = null;

                int rsrp = 0;
                int rsrq = 0;
                if(telephonyManager.getAllCellInfo().size() > 0) {
                    CellInfo cellInfo = telephonyManager.getAllCellInfo().get(0);
                    if (cellInfo instanceof CellInfoGsm) {
                        signalStrength = ((CellInfoGsm) cellInfo).getCellSignalStrength();
                    } else if (cellInfo instanceof CellInfoWcdma) {
                        signalStrength = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
                    } else if (cellInfo instanceof CellInfoLte) {
                        signalStrength = ((CellInfoLte) cellInfo).getCellSignalStrength();
                        String cellSignalStrengthLteStr = ((CellInfoLte) cellInfo).getCellSignalStrength().toString();
                        rsrp = Integer.valueOf(utility.stringBetweenSubstring(cellSignalStrengthLteStr, "rsrp=", "\t"));
                        rsrq = Integer.valueOf(utility.stringBetweenSubstring(cellSignalStrengthLteStr, "rsrq=", "\t"));
                    } else if (cellInfo instanceof CellInfoCdma) {
                        signalStrength = ((CellInfoCdma) cellInfo).getCellSignalStrength();
                    } else {
                        signalStrength = null;
                    }
                }

                mobileMetric.timeStampList.add(String.valueOf(System.currentTimeMillis()));
                if(signalStrength!=null) {
                    mobileMetric.asuLevelList.add(signalStrength.getAsuLevel());
                    mobileMetric.signalStrengthList.add(signalStrength.getDbm());
                }
                else {
                    mobileMetric.asuLevelList.add(Integer.MAX_VALUE);
                    mobileMetric.signalStrengthList.add(Integer.MAX_VALUE);
                }
                mobileMetric.rsrpList.add(rsrp);
                mobileMetric.rsrqList.add(rsrq);
                mobileMetric.networkTypeList.add(utility.getNetworkClass(context));
            }

            try {
                Thread.sleep(interval);
                curSeconds += interval/1000;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(Config.isRunning == 0)
                return 1L;
        }

        //addMeasurementMetricToList()
        try {
            addMeasurementParams(wiFiMetric, mobileMetric);
            writeToFile(wiFiMetric, mobileMetric);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
    }
}