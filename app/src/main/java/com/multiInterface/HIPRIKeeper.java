package com.multiInterface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;

/**
 * Created by t-taman on 5/24/2016.
 */
public class HIPRIKeeper {
    private Context context;
    private final Notifications notif;
    private final MobileDataMgr mobileDataMgr;
    private final Handler handler;
    private static long lastTimeHandler;

    public HIPRIKeeper(Context context) {
        this.context = context;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Config.getDefaultConfig(context);
        notif = new Notifications(context);
        mobileDataMgr = new MobileDataMgr(context);
        Log.i(Manager.TAG, "new HIPRI Keeper");

        handler = new Handler();
        initHandler();

		/*
		 * mConnReceiver will be called each time a change of connectivity
		 * happen
		 */
        context.registerReceiver(mConnReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        Logger.d("Connectivity changed");
        if (Config.enable)
            notif.showNotification();
        if (! Config.saveBattery)
            mobileDataMgr.keepMobileConnectionAlive();
    }

    public void destroy() {
        /*
        try {
            context.unregisterReceiver(mConnReceiver);
        } catch (IllegalArgumentException e) {
        }


        Log.i(Manager.TAG, "destroy MPCtrl");
        */
        handler.removeCallbacksAndMessages(null);

        notif.hideNotification();
    }

    public boolean setStatus(boolean isChecked) {
        if (isChecked == Config.enable)
            return false;

        Log.i(Manager.TAG, "set new status "
                + (isChecked ? "enable" : "disable"));
        Config.enable = isChecked;
        Config.saveStatus(context);

        if (isChecked)
            notif.showNotification();
        else
            notif.hideNotification();

        return true;
    }

    public boolean setSaveBattery(boolean isChecked) {
        if (isChecked == Config.saveBattery)
            return false;
        Config.saveBattery = isChecked;
        Config.saveStatus(context);
        return true; // nothing to do here: we need to restart app
    }

    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(Manager.TAG, "BroadcastReceiver " + intent);
            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mobileDataMgr.setMobileDataActive(Config.enable);
        }
    };

    /*
     * Will not be executed in deep sleep, nice, no need to use both connections
     * in deep-sleep
     */
    private void initHandler() {
        lastTimeHandler = System.currentTimeMillis();

        // First check
        handler.post(runnableSetMobileDataActive);
    }

    /*
     * Ensures that the data interface and WiFi are connected at the same time.
     */
    private Runnable runnableSetMobileDataActive = new Runnable() {
        final long periodMs = 10 * 1000;

        @Override
        public void run() {
            mobileDataMgr.setMobileDataActive(Config.enable);
            handler.postDelayed(this, periodMs);
        }
    };
}
