package org.arpnetwork.arpdevice.app;

import android.app.Activity;

import org.arpnetwork.arpdevice.ui.home.HomeActivity;

import java.util.Map;
import java.util.WeakHashMap;

public class ActivityManager {
    private Map<Activity, Boolean> mActivities;
    private Map<Activity, Boolean> mTempActivities;

    private static ActivityManager sInstance = null;

    public static ActivityManager getInstance() {
        if (sInstance == null) {
            sInstance = new ActivityManager();
        }

        return sInstance;
    }

    public void add(Activity activity) {
        if (mTempActivities != null) {
            mTempActivities.put(activity, true);
        } else {
            mActivities.put(activity, true);
        }
    }

    public void remove(Activity activity) {
        if (mTempActivities != null) {
            mTempActivities.remove(activity);
            if (mTempActivities.size() == 0) {
                mTempActivities = null;
            }
        } else {
            mActivities.remove(activity);
        }
    }

    public void createNewTask() {
        mTempActivities = new WeakHashMap<Activity, Boolean>();
    }

    public void clearTask() {
        finishActivities(mTempActivities);
        mTempActivities = null;
    }

    public Activity getMainActivity() {
        for (Activity activity : mActivities.keySet()) {
            if (activity instanceof HomeActivity) {
                return activity;
            }
        }
        return null;
    }

    public void finishActivities() {
        clearTask();
        finishActivities(mActivities);
    }

    private void finishActivities(Map<Activity, Boolean> activities) {
        if (activities == null) {
            return;
        }

        for (Activity activity : activities.keySet()) {
            activity.finish();
        }
        activities.clear();
    }

    private ActivityManager() {
        mActivities = new WeakHashMap<Activity, Boolean>();
    }
}
