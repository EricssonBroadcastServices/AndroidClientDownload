package net.ericsson.emovs.download;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import net.ericsson.emovs.exposure.clients.exposure.ExposureClient;
import net.ericsson.emovs.utilities.system.RunnableThread;

import net.ericsson.emovs.utilities.emp.EMPRegistry;


/**
 * Created by Joao Coelho on 2017-10-05.
 */

public class EMPDownloadService extends Service {
    private static final String TAG = EMPDownloadService.class.toString();

    static final int NOTIFICATION_ID = 543;
    //private static boolean isRunning;

    public EMPDownloadService() {
        super();
        //isRunning = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new RunnableThread(new Runnable() {
            @Override
            public void run() {
                start();
            }
        }).start();
        this.showNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // START_REDELIVER_INTENT ??
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    //public static boolean isRunning() {
    //    return isRunning;
    //}


    private void start() {
        Log.d(TAG, "Starting service.");
        //isRunning = true;
        run();
    }

    private void stop() {
        Log.d(TAG, "Stopping service.");
        stopForeground(true);
        stopSelf();
        //isRunning = false;
    }

    private void waitUntilLogin() {
        while (ExposureClient.getInstance().getSessionToken() == null||
                ContextCompat.checkSelfPermission(EMPRegistry.applicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void run() {
        waitUntilLogin();
        DownloadItemManager.getInstance().syncWithStorage();
        for (;;) {
            try {
                if (DownloadItemManager.getInstance().hasAssetsToDelete()) {
                    DownloadItemManager.getInstance().flushRemovedAssets();
                }
                if (DownloadItemManager.getInstance().count(DownloadItem.State.QUEUED) > 0) {
                    if (DownloadItemManager.getInstance().canStartNewDownload()) {
                        Log.d(TAG, "Downloading next asset...");
                        DownloadItemManager.getInstance().downloadNext();
                    }
                    else {
                        Thread.sleep(100);
                        Log.d(TAG, "Waiting for download to finish...");
                        continue;
                    }
                }
                else if (DownloadItemManager.getInstance().count(DownloadItem.State.DOWNLOADING) == 0 &&
                         DownloadItemManager.getInstance().count(DownloadItem.State.PAUSED) == 0) {
                    Log.d(TAG, "Stopping service...");
                    stop();
                    break;
                }
                else {
                    Log.d(TAG, "Waiting for download to finish...");
                    Thread.sleep(100);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        Log.d(TAG, "Stopping service...");
    }

    private void showNotification() {
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("DownloadItem")
//                .setTicker(getResources().getString(R.string.app_name))
                .setContentText("Downloading...")
//                .setSmallIcon(R.drawable.my_icon)
//                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
//                .setContentIntent(contentPendingIntent)
//                .setOngoing(true)
//                .setDeleteIntent(contentPendingIntent)  // if needed
                .build();
//
//        // NO_CLEAR makes the notification stay when the user performs a "delete all" command
        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;
        startForeground(NOTIFICATION_ID, notification);
    }
}
