package org.arpnetwork.arpdevice.device;

import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.stream.Touch;

public class Adb {
    private static final String TAG = "Adb";
    private static final boolean DEBUG = Config.DEBUG;

    private final Connection mConnection;

    public Adb(Connection syncChannel) {
        this.mConnection = syncChannel;
    }

    public void getTopAndroidTask(ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            ShellChannel ss = mConnection.openShell("dumpsys activity top | grep TASK");
            ss.setListener(listener);
        }
    }

    public void killApp(String packageName) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            mConnection.openShell(String.format("am force-stop %s", packageName));
        }
    }
}
