package com.multiInterface;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Created by t-taman on 5/24/2016.
 */
public class Notifications {
    private final NotificationManager mNotification;
    private final Context context;

    public Notifications(Context context) {
        this.context = context;
        mNotification = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void showNotification() {
        Intent intent = new Intent(context, NetworkTools.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1,
                intent, 0);

        android.app.Notification notif = new android.app.Notification.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(
                        context.getResources().getString(
                                R.string.notification_title))
                .setContentText(
                        context.getResources().getString(
                                R.string.notification_text))
                .setContentIntent(pendingIntent).build();

        notif.flags |= android.app.Notification.FLAG_NO_CLEAR;

        mNotification.notify(1, notif);
    }

    public void hideNotification() {
        NotificationManager mNotification = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification.cancelAll();
    }
}
