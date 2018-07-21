package org.arpnetwork.arpdevice.device;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.TextUtils;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.Touch;

public class TaskHelper {
    private static final String TAG = "TaskHelper";
    private static final boolean DEBUG = Config.DEBUG;
    private static final int CHECK_TOP_INTERVAL = 800; // shell cost 80ms.

    private String mPackageName;
    private final Handler mHandler;
    private final Adb mAdb;

    public TaskHelper(Handler handler) {
        mHandler = handler;
        mAdb = new Adb(Touch.getInstance().getConnection());
    }

    public boolean launchApp(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        mPackageName = packageName;
        PackageManager packageManager = CustomApplication.sInstance.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(mPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            CustomApplication.sInstance.startActivity(intent);
            startCheckTopTimer();
            return true;
        }
        return false;
    }

    private void startCheckTopTimer() {
        mHandler.postDelayed(mCheckTopRunnable, CHECK_TOP_INTERVAL);
    }

    private final Runnable mCheckTopRunnable = new Runnable() {
        @Override
        public void run() {
            mAdb.getTopAndroidTask(new ShellChannel.ShellListener() {
                @Override
                public void onStdout(ShellChannel ch, String data) {
                    String topPackage = getTopPackage(data);
                    if (!topPackage.contains(mPackageName)) {
                        DataServer.getInstance().onClientDisconnected();
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }

                @Override
                public void onStderr(ShellChannel ch, String data) {
                }

                @Override
                public void onExit(ShellChannel ch, int code) {
                }
            });

            mHandler.postDelayed(this, CHECK_TOP_INTERVAL);
        }
    };

    public void killLaunchedApp() {
        mHandler.removeCallbacksAndMessages(null);
        mAdb.killApp(mPackageName);
    }

    private String getTopPackage(String topTaskPackages) {
        String[] lines = topTaskPackages.split("\n");
        String lastLine = lines[lines.length - 1];
        try {
            return lastLine.split(" ")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }
}
