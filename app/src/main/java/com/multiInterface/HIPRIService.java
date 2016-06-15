package com.multiInterface;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HIPRIService extends Service {
    private HIPRIKeeper hipriKeeper;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        hipriKeeper = Manager.create(getApplicationContext());
        Log.i(Manager.TAG, "Create service");
    }

    public void onDestroy() {
        super.onDestroy();
        if (hipriKeeper != null) {
            Manager.destroy(getApplicationContext());
            Log.i(Manager.TAG, "Destroy service");
        }
    }


}
