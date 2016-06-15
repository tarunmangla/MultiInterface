package com.multiInterface;

import android.app.Service;
import android.content.Intent;
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
        networkMetricTask = new NetworkMetricTask();
        networkMetricTask.execute(UDPSingleService.this);
        udpSingleTask = new UDPSingleTask();
        udpSingleTask.execute(UDPSingleService.this);



    }

    public void onDestroy() {
        super.onDestroy();

        if(udpSingleTask != null)
            udpSingleTask.onCancelled();

    }
}
