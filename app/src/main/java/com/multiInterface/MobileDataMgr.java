package com.multiInterface;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created by t-taman on 5/24/2016.
 */
public class MobileDataMgr {
    private final Context context;
    /*
     * We need an existing domain that nobody uses in order to have a existing
     * route assigned to the cellular connection. The goal is to not disable
     * Cellular interface when switching to a new Wi-Fi. example.org domain is
     * reserved by IANA, nobody will want to use it!
     */
    private static final String DEFAULT_LOOKUP_HOST = "example.org";

    public MobileDataMgr(Context context) {
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();
        NetworkInfo networkInfo;
        Log.d("Networks", String.valueOf(networks.length));
        for (Network mNetwork : networks) {
            networkInfo = connManager.getNetworkInfo(mNetwork);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return networkInfo.isConnectedOrConnecting();
            }
        }
        return false;
    }

    /* Check whether Mobile Data has been disabled in the System Preferences */
    private boolean isMobileDataEnabled() {
        boolean mobileDataEnabled = false;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class<?> cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
        }
        return mobileDataEnabled;
    }

    /* Enable having WiFi and 3G/LTE enabled at the same time */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public void setMobileDataActive(boolean mEnabled) {
        if (Manager.DEBUG)
            Log.d(Manager.TAG, "setMobileDataActive " + new Date());

        ConnectivityManager cManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);


        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);				// network.openConnection(url)
            }
            @Override
            public void onLosing(Network network, int maxMsToLive) {
                super.onLosing(network, maxMsToLive);
            }
            @Override
            public void onLost(Network network) {
                super.onLost(network);
            }
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
            }
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
            }
        };


        NetworkRequest.Builder builder = new NetworkRequest.Builder();

        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        NetworkRequest networkRequest = builder.build();


        if (isMobileDataEnabled() && isWifiConnected() && mEnabled) {
            Logger.d("Requested cellular network");
            cManager.requestNetwork(networkRequest, networkCallback);
            cManager.registerNetworkCallback(networkRequest, networkCallback);
        }
        else {
            try {
                cManager.unregisterNetworkCallback(networkCallback);
            }
            catch(Exception e){
                Logger.d("Unable to unregister");
            }
        }
    }

    private static int getAddr(InetAddress inetAddress) {
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8) | (addrBytes[0] & 0xff);
        return addr;
    }

    /**
     * Transform host name in int value used by
     * {@link android.net.ConnectivityManager#requestRouteToHost(int, int)} method
     *
     * @param hostname
     * @return -1 if the host doesn't exists, elsewhere its translation to an
     * integer
     */
    private static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        return getAddr(inetAddress);
    }

    /**
     * Enable mobile connection even when switching to WiFi
     * <p/>
     * Source: http://stackoverflow.com/a/4756630
     *
     * @return true for success, else false
     */
    public boolean keepMobileConnectionAlive() {
        Logger.d("I WAS CALLED I WAS CALLED");
        Logger.d("called keepMobileConnectionAlive");
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == connectivityManager) {
            Log.d(Manager.TAG,
                    "ConnectivityManager is null, cannot try to force a mobile connection");
            return false;
        }

        // create a route for the specified address
        int hostAddress = lookupHost(DEFAULT_LOOKUP_HOST);
        if (-1 == hostAddress) {
            Log.e(Manager.TAG,
                    "Wrong host address transformation, result was -1");
            return false;
        }
        // wait some time needed to connection manager for waking up
        try {
            for (int counter = 0; counter < 30; counter++) {
                NetworkInfo.State checkState = connectivityManager.getNetworkInfo(
                        ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
                Log.d(Manager.TAG, "TYPE_MOBILE_HIPRI network state: "
                        + checkState);
                if (0 == checkState.compareTo(NetworkInfo.State.CONNECTED))
                    break;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // nothing to do
        }
        boolean resultBool = connectivityManager.requestRouteToHost(
                ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);
        Log.d(Manager.TAG, "requestRouteToHost result: " + resultBool);
        if (!resultBool)
            Log.e(Manager.TAG,
                    "Wrong requestRouteToHost result: expected true, but was false");

        return resultBool;
    }
}
