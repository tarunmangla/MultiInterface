package com.multiInterface;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class UDPService extends Service {
    private UDPSingleTask udpSingleTask;
    private UDPAsyncTask udpAsyncTask;
    public UDPService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        udpAsyncTask = new UDPAsyncTask();
        udpAsyncTask.execute(UDPService.this);


    }

    public void onDestroy() {
        super.onDestroy();

        if(udpAsyncTask != null)
            udpAsyncTask.onCancelled();

    }
}
