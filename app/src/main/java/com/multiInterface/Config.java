package com.multiInterface;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by t-taman on 5/24/2016.
 */
public class Config {
    public static String VERSION = " ";
    public static final String PREFS_NAME         = "HIPRIKeeper";
    public static final String PREFS_STATUS       = "enableMultiInterfaces";
    public static final String PREFS_SAVE_BATTERY = "saveBattery";
    public static final int DEBUG = 1;
    public static boolean enable = true;
    public static boolean saveBattery = true;
    public static final String UPLOAD_SERVER_IP = "http://191.237.81.137:10102/upload/multipart";
    public static int numSeconds = 120;
    public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    public static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 3;
    public static int phoneStatePermissionGranted = 0;
    public static int locationPermissionGranted = 0;
    public static volatile Context context;
    public static volatile int isRunning = 0;
    public static volatile String mobileSendData = "";
    public static volatile String wifiSendData = "";
    public static volatile String mobileReceiveData = " ";
    public static volatile  String wifiReceiveData = " ";
    public static volatile String progressData = "";
    public static volatile int isMeasurementRunning = 0;
    public static volatile ArrayList<MeasurementParams> measurementParamsList = new ArrayList<>();
    public static long THRESHOLD = 3*60*60*1000;
    public static int WIFI_RSSI_THRESHOLD = 20;
    public static int MOBILE_RSSI_THRESHOLD = 20;
    public static volatile int hasFinished = 0;
    public static volatile int isWaiting = 0;
    public static volatile int isClockRunning = 0;
    public static volatile long lastTime = 0;
    public static long periodMsec = 3*60*1000;
    public static volatile int waitingTime = 0;
    public static volatile int seq = 1;
    public static int numSkips = 0;
    public static String ANDROID_ID;
    public static int counter = 0;
    public static String measurementParamsFile = "measurementParams.txt";
    private Config() {
    }

    public static void getDefaultConfig(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        enable = settings.getBoolean(PREFS_STATUS, true);
        saveBattery = settings.getBoolean(PREFS_SAVE_BATTERY, true);
    }

    public static void saveStatus(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(PREFS_STATUS, enable);
        editor.putBoolean(PREFS_SAVE_BATTERY, saveBattery);
        editor.apply();
    }




}
