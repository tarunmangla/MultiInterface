package com.multiInterface;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by t-taman on 5/20/2016.
 */
 public class NetworkTools extends Activity {

    TextView textMobileSend, textWifiSend, textMobileReceive, textWifiReceive, textProgress, textWait;
    Utility utility;
    UDPAsyncTask udpAsyncTask;
    int checkedId;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Config.MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Logger.d("Permission granted");
                    Config.phoneStatePermissionGranted = 1;

                } else {
                    Logger.d("Read Phone State Permission not granted");
                    Config.phoneStatePermissionGranted = 0;
                }
                return;
            }
            case Config.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Logger.d("Permission granted");
                    Config.locationPermissionGranted = 1;

                } else {
                    Logger.d("Access Coarse Location Permission not granted");
                    Config.locationPermissionGranted = 0;
                }
                return;
            }
            case Config.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        Config.phoneStatePermissionGranted = 0;

                    }
                    if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                        Config.locationPermissionGranted = 0;
                    }
                } else {
                    Logger.d("Permissions granted");
                    Config.locationPermissionGranted = 0;
                    Config.phoneStatePermissionGranted = 0;
                }
                return;
            }
        }
    }

    public void checkAppPermission() {
        int permissionCheckPhoneState = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);
        int permissionCheckCoarseLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if(permissionCheckPhoneState!= PackageManager.PERMISSION_GRANTED ||
                permissionCheckCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                    Config.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

        }
        else {
            Config.phoneStatePermissionGranted = 1;
            Config.locationPermissionGranted = 1;
        }
    }


    public void updateTextBox()
    {
        textMobileSend = (TextView) findViewById(R.id.textViewMobileSend);
        textWifiSend = (TextView) findViewById(R.id.textViewWiFiSend);
        textMobileReceive = (TextView) findViewById(R.id.textViewMobileReceive);
        textWifiReceive = (TextView) findViewById(R.id.textViewWifiReceive);
        textProgress = (TextView) findViewById(R.id.currentProgress);
        textWait = (TextView) findViewById(R.id.waitTime);

        final Handler handler =new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                textMobileSend.setText(Config.mobileSendData);
                textWifiSend.setText(Config.wifiSendData);
                textMobileReceive.setText(Config.mobileReceiveData);
                textWifiReceive.setText(Config.wifiReceiveData);
                textProgress.setText(Config.progressData);

                if(Config.isRunning == 0) {
                    Config.wifiSendData = " ";
                    Config.mobileSendData = " ";
                    Config.wifiReceiveData = " ";
                    Config.mobileReceiveData = " ";
                    Config.progressData = "Stopped!";
                }


                if(checkedId == R.id.radioBackground && Config.isRunning == 1 && Config.isWaiting == 1) {
                    if(Config.isClockRunning == 0) {
                        Config.isClockRunning = 1;
                        Config.waitingTime = (int) TimeUnit.MILLISECONDS.toSeconds(Config.periodMsec);
                        Config.lastTime = System.currentTimeMillis();
                    }
                    else{
                        Config.waitingTime = (int) (Config.periodMsec - (System.currentTimeMillis() - Config.lastTime))/1000;
                    }
                    textWait.setText("Waiting for " + Config.waitingTime + " seconds");
                }
                else {
                    Config.isClockRunning = 0;
                    Config.isWaiting = 0;
                    textWait.setText(" ");
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.postDelayed(r,0000);
    }


    public void waitForFinish()
    {
        final Handler handler =new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                if(Config.hasFinished == 1) {
                    ToggleButton toggle = (ToggleButton) findViewById(R.id.startToggleButton);
                    if(toggle.isChecked())
                        toggle.setChecked(false);
                }
                else {
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.postDelayed(r,0000);
    }



    public void readParamsFromFile() throws IOException, ClassNotFoundException {
        Config.measurementParamsList = new ArrayList<>();
        File f = new File(Config.measurementParamsFile);
        if(!f.exists())
            return;

        FileInputStream fIn = new FileInputStream(Config.measurementParamsFile);
        ObjectInputStream oIn = new ObjectInputStream(fIn);
        MeasurementParams measurementParams;
        do {
            measurementParams = (MeasurementParams) oIn.readObject();
            if(measurementParams!=null) {
                Config.measurementParamsList.add(measurementParams);
            }
        }
        while (measurementParams!=null);
        Logger.d("Count is " + Config.measurementParamsList.size());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        checkAppPermission();
        utility = new Utility();

        setContentView(R.layout.tools);

        updateTextBox();

        Config.ANDROID_ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Config.VERSION = String.valueOf(pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        ToggleButton toggle = (ToggleButton) findViewById(R.id.startToggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(Config.isRunning!=1) {
                        Config.isRunning = 1;
                        Logger.d("Size is " + Config.measurementParamsList.size());
                        utility.acquireWakeLock(NetworkTools.this);
                        RadioGroup rg = (RadioGroup) findViewById(R.id.options);
                        checkedId = rg.getCheckedRadioButtonId();

                        startService(new Intent(NetworkTools.this, HIPRIService.class));

                        if (checkedId == R.id.radioBackground) {
                            startService(new Intent(NetworkTools.this, UDPService.class));
                        }
                        else {
                            Config.hasFinished = 0;
                            startService(new Intent(NetworkTools.this, UDPSingleService.class));
                            waitForFinish();
                        }
                    }
                } else {
                    Config.isRunning = 0;
                    utility.releaseWakeLock();
                    stopService(new Intent(NetworkTools.this, HIPRIService.class));

                    if (checkedId == R.id.radioBackground) {
                        stopService(new Intent(NetworkTools.this, UDPService.class));
                    }
                    else {
                        stopService(new Intent(NetworkTools.this, UDPSingleService.class));
                    }
                }
            }
        });

        toggle.setChecked(true);



    }


    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioBackground:
                if (checked)
                    break;
            case R.id.radioSingle:
                if (checked)
                    break;
        }
    }


}

