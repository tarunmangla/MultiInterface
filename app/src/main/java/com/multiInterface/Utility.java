package com.multiInterface;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;

import com.multiInterface.upload.MultipartUploadRequest;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Arrays;

/**
 * Created by t-taman on 6/6/2016.
 */
public class Utility {

    PowerManager.WakeLock wakeLock = null;

    public String getNetworkClass(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    public void uploadFile(String fileName, Context context) throws FileNotFoundException {
        final String serverUrlString = Config.UPLOAD_SERVER_IP;

        final String filesToUploadString = fileName;
        Logger.d("FileName is" + filesToUploadString);


        MultipartUploadRequest req = new MultipartUploadRequest(context , serverUrlString)
                .addFileToUpload(filesToUploadString, "test")
                .setAutoDeleteFilesAfterSuccessfulUpload(true)
                .setUsesFixedLengthStreamingMode(false)
                .setMaxRetries(2);

        req.setUtf8Charset();

        try {
            String uploadID = req.startUpload();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }



    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Network getNetwork(Integer type, Context context){
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();
        NetworkInfo networkInfo;
        for (Network mNetwork : networks) {
            networkInfo = connManager.getNetworkInfo(mNetwork);
            if (networkInfo.getType() == type) {
                Logger.d("Network Found : " + networkInfo.getTypeName());
                return mNetwork;
            }
        }
        return null;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public String getCellId(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int cellId = -1;
        int lac = -1;
        int sid = -1;
        if (Config.locationPermissionGranted == 1 && Config.phoneStatePermissionGranted == 1) {
            if(telephonyManager.getAllCellInfo().size() > 0) {
                CellInfo cellInfo = telephonyManager.getAllCellInfo().get(0);
                if (cellInfo instanceof CellInfoGsm) {
                    CellIdentityGsm cellIdentityGsm = ((CellInfoGsm) cellInfo).getCellIdentity();
                    cellId = cellIdentityGsm.getCid();
                    lac = cellIdentityGsm.getLac();
                    sid = cellIdentityGsm.getMcc();
                } else if (cellInfo instanceof CellInfoWcdma) {
                    CellIdentityWcdma cellIdentityWcdma = ((CellInfoWcdma) cellInfo).getCellIdentity();
                    cellId = cellIdentityWcdma.getCid();
                    lac = cellIdentityWcdma.getLac();
                    sid = cellIdentityWcdma.getMcc();
                } else if (cellInfo instanceof CellInfoLte) {
                    CellIdentityLte cellIdentityLte = ((CellInfoLte) cellInfo).getCellIdentity();
                    cellId = cellIdentityLte.getCi();
                    lac = cellIdentityLte.getTac();
                    sid = cellIdentityLte.getMcc();
                } else if (cellInfo instanceof CellInfoCdma) {
                    CellIdentityCdma cellIdentityCdma = ((CellInfoCdma) cellInfo).getCellIdentity();
                    cellId = cellIdentityCdma.getBasestationId();
                    lac = cellIdentityCdma.getNetworkId();
                    sid = cellIdentityCdma.getSystemId();
                }
            }
        }
        String cellIdString = String.valueOf(cellId) + ' ' + String.valueOf(lac) + ' '  + String.valueOf(sid);
        return cellIdString;
    }

    public double getMedian(int[] numArray) {
        double median = -1;
        if(numArray.length == 0)
            return median;

        Arrays.sort(numArray);
        if (numArray.length % 2 == 0)
            median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
        else
            median = (double) numArray[numArray.length/2];
        return  median;
    }

    /** Wakes up the CPU of the phone if it is sleeping. */
    public synchronized void acquireWakeLock(Context context) {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tag");
        }
        Logger.d("PowerLock acquired");
        wakeLock.acquire();
    }

    /** Release the CPU wake lock. WakeLock is reference counted by default: no need to worry
     * about releasing someone else's wake lock */
    public synchronized void releaseWakeLock() {
        if (wakeLock != null) {
            try {
                wakeLock.release();
                Logger.i("PowerLock released");
            } catch (RuntimeException e) {
                Logger.e("Exception when releasing wakeup lock", e);
            }
        }
    }

    public String stringBetweenSubstring(String str, String str1, String str2) {
        int idx1 = str.indexOf(str1) + str1.length();
        if(idx1 == -1)
            return "0";

        int idx2 = str.substring(idx1).indexOf(str2) + idx1;

        if(idx2 < idx1)
            return "0";

        return str.substring(idx1, idx2);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public CellSignalStrength getCellSignalStrength(Context context)  {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        CellSignalStrength signalStrength = null;

        if(telephonyManager.getAllCellInfo().size() > 0) {
            CellInfo cellInfo = telephonyManager.getAllCellInfo().get(0);
            if (cellInfo instanceof CellInfoGsm) {
                signalStrength = ((CellInfoGsm) cellInfo).getCellSignalStrength();
            } else if (cellInfo instanceof CellInfoWcdma) {
                signalStrength = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
            } else if (cellInfo instanceof CellInfoLte) {
                signalStrength = ((CellInfoLte) cellInfo).getCellSignalStrength();
            } else if (cellInfo instanceof CellInfoCdma) {
                signalStrength = ((CellInfoCdma) cellInfo).getCellSignalStrength();
            } else {
                signalStrength = null;
            }
        }

        return signalStrength;
    }

}
