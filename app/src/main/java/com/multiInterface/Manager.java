package com.multiInterface;

import android.content.Context;
import android.util.Log;

/**
 * Created by t-taman on 5/24/2016.
 */
public class Manager {
    public static final String TAG = "hiprikeeper";
    public static final boolean DEBUG = false;

    private static HIPRIKeeper hipriKeeper = null;
    private static int instances = 0;
    private static Context usedContext;

    private Manager() {
    }

     /**
     * Create a new instance of hipriKeeper.
     *
     * @return null if you're not root.
     */
    public static HIPRIKeeper create(Context context) {
        if (hipriKeeper == null) {
            usedContext = context;
            hipriKeeper = new HIPRIKeeper(context);
        }
        instances++;
        return hipriKeeper;
    }

    /**
     * Destroy the instance only if we are using this context.
     *
     * @return true if the instance has really been fully destroyed
     */
    public static boolean destroy(Context context) {
        if (context != usedContext || hipriKeeper == null)
            return false;
        instances--;
        if (instances != 0) {
            Log.e(TAG, "destroying the non last instance");
            return false;
        }
        hipriKeeper.destroy();
        hipriKeeper = null;
        return true;
    }
}
