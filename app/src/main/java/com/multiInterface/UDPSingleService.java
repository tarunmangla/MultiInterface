package com.multiInterface;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

public class UDPSingleService extends Service {
    UDPSingleTask udpSingleTask;
    NetworkMetricTask networkMetricTask;
    public UDPSingleService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Logger.d("Debug: service created");
        Config.isMeasurementRunning = 1;
        Logger.d("Debug: print start" + Config.isMeasurementRunning + " " + Config.isRunning);
        udpSingleTask = new UDPSingleTask();

        udpSingleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, UDPSingleService.this);

        networkMetricTask = new NetworkMetricTask();
        networkMetricTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, UDPSingleService.this);

        Logger.d("Debug: print end" + Config.isMeasurementRunning + " " + Config.isRunning);


    }

    public void onDestroy() {
        super.onDestroy();
        Config.isMeasurementRunning = 0;
        if(udpSingleTask != null)
            udpSingleTask.onCancelled();

    }
}
