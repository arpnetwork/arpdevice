/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arpnetwork.arpdevice.util;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.R;

import org.spongycastle.util.encoders.Hex;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class Util {
    private static final String TAG = "Util";

    private Util() {
        // prevent initial.
    }

    public static void copy(InputStream in, OutputStream out) {
        if (in != null && out != null) {
            int BUFFER_SIZE = 1024 * 8;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            try {
                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
            } catch (IOException ignored) {
            } finally {
                try {
                    out.close();
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void copy(InputStream in, SyncChannel out) throws IOException {
        if (in != null && out != null) {
            int BUFFER_SIZE = 1024 * 8;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            try {
                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    out.writeData(buffer, 0, len);
                }
                out.writeDone((int) (System.currentTimeMillis() / 1000));
            } finally {
                try {
                    out.close();
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String stringFromFile(String path) {
        StringBuilder builder = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                builder.append(str);
            }
            bufferedReader.close();
            fileReader.close();
        } catch (Exception e) {
        }
        return builder.toString();
    }

    public static final String ObjToJson(Object obj) {
        Gson gson = new Gson();

        return gson.toJson(obj);
    }

    // Cmd cmd = Util.loadObject(msg, Cmd.class);
    public static <T> T loadObject(String json, Class<T> klass) {
        T resp = null;
        Gson mGson = new Gson();
        try {
            resp = mGson.fromJson(json, klass);
        } catch (JsonSyntaxException e) {
        }

        return resp;
    }

    public static String md5(byte[] data) {
        String md5 = null;
        if (data != null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(data, 0, data.length);

                BigInteger bigInt = new BigInteger(1, digest.digest());
                md5 = bigInt.toString(16);
            } catch (NoSuchAlgorithmException ignored) {
            }
        }
        return md5;
    }

    public static String md5(File file) {
        String md5 = null;
        MessageDigest digest = null;
        if (file != null && file.isFile()) {
            FileInputStream in = null;
            byte buffer[] = new byte[1024 * 1024];
            int len = 0;
            try {
                digest = MessageDigest.getInstance("MD5");
                in = new FileInputStream(file);

                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    digest.update(buffer, 0, len);
                }

                BigInteger bigInt = new BigInteger(1, digest.digest());
                md5 = bigInt.toString(16);
                md5 = md5.length() < 32 ? "0" + md5 : md5;
            } catch (NoSuchAlgorithmException ignored) {
            } catch (FileNotFoundException ignored) {
            } catch (IOException ignored) {
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return md5;
    }

    public static String getRandomString(int length) {
        String str = "zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; ++i) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static BigDecimal getEthCost(BigDecimal gasPriceGwei, BigInteger gasUsed) {
        BigDecimal gasUsedDecimal = new BigDecimal(gasUsed);
        BigDecimal bUsed = gasPriceGwei.multiply(gasUsedDecimal);
        return Convert.fromWei(bUsed.toString(), Convert.Unit.GWEI);
    }

    public static double getYuanCost(BigDecimal gasPriceGwei, BigInteger gasUsed, BigDecimal ethToYuanRate) {
        BigDecimal getEthCost = getEthCost(gasPriceGwei, gasUsed);
        BigDecimal result = getEthCost.multiply(ethToYuanRate)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        return result.doubleValue();
    }

    public static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) +
                "." + ((ip >> 16) & 0xFF) +
                "." + ((ip >> 8) & 0xFF) +
                "." + (ip & 0xFF);
    }

    public static long ipToLong(String strIp) {
        String[] ip = strIp.split("\\.");
        return (Long.parseLong(ip[0]) << 24) + (Long.parseLong(ip[1]) << 16) + (Long.parseLong(ip[2]) << 8) + Long.parseLong(ip[3]);
    }

    public static String join(String join, String[] array) {
        StringBuilder sb = new StringBuilder();
        if (array != null) {
            for (int i = 0, len = array.length; i < len; i++) {
                sb.append(array[i]);

                if (i < len - 1) {
                    sb.append(join);
                }
            }
        }

        return sb.toString();
    }

    public static byte[] stringToBytes32(String address) {
        String addrCleanPrefix = Numeric.cleanHexPrefix(address);

        byte[] bytes = Hex.decode(addrCleanPrefix);
        byte[] address32 = new byte[32];
        System.arraycopy(bytes, 0, address32, 32 - bytes.length, bytes.length);
        return address32;
    }

    public static String getDateTime(String pattern, long times) {
        DateFormat format = new SimpleDateFormat(pattern, Locale.US);
        String dateTime = format.format(new Date(times * 1000));

        return dateTime;
    }

    public static String getLongDurationString(Context context, long deadLine) {
        if (deadLine == 0) {
            return context.getString(R.string.duration_permanent);
        }

        long duration = deadLine - System.currentTimeMillis() / 1000;
        final long day = 60 * 60 * 1000 * 24;
        final long month = 30 * day;
        final long year = 365 * day;
        if (duration > year) {
            return context.getResources().getQuantityString(R.plurals.duration_year, (int) (duration / year), (int) (duration / year));
        } else if (duration > month) {
            return context.getResources().getQuantityString(R.plurals.duration_month, (int) (duration / month), (int) (duration / month));
        } else if (duration > day) {
            return context.getResources().getQuantityString(R.plurals.duration_day, (int) (duration / day), (int) (duration / day));
        } else {
            return getDurationString(context, duration);
        }
    }

    public static String getDurationString(Context context, long duration) {
        final long second = 1000;
        final long minute = 60 * second;
        final long hour = 60 * minute;

        String showTime = "";
        if (duration >= minute) {
            if (duration >= hour) {
                int durationHour = (int) (duration / hour);
                showTime = context.getResources().getQuantityString(R.plurals.duration_hour, durationHour, durationHour) + " ";
            }
            int durationMinute = (int) ((duration % hour) / minute);
            showTime = showTime + context.getResources().getQuantityString(R.plurals.duration_minute, durationMinute, durationMinute) + " ";
        } else {
            int durationSecond = (int) (duration / second);
            showTime = context.getResources().getQuantityString(R.plurals.duration_second, durationSecond, durationSecond);
        }

        return showTime;
    }

    public static float getHumanicAmount(BigInteger amount) {
        return Convert.fromWei(new BigDecimal(amount), Convert.Unit.ETHER).floatValue();
    }

    public static String getAppName(Context context) {
        String label = "";
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            label = pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
        }
        return label;
    }

    public static String getAppVersion(Context context) {
        String appVersion = "";
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return appVersion;
    }

    public static int getAppVersionCode(Context context) {
        int appVersionCode = 0;
        try {
            appVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return appVersionCode;
    }

    public static void jumpToSettingADB(Context context) {
        try {
            ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings");
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setAction("android.intent.action.View");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            UIHelper.showToast(context, context.getString(R.string.check_fail_adb_exception));
        }
    }

    public static boolean hasPackage(Context context, String packageName) {
        if (null == context || null == packageName) {
            return false;
        }

        boolean bHas = true;
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_GIDS);
        } catch (PackageManager.NameNotFoundException e) {
            bHas = false;
        }
        return bHas;
    }

    public static boolean isCharging(Activity activity) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, intentFilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        return isCharging;
    }

    public static float getBatteryPct(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    public static float getScreenBrightness(Context context) {
        float result = 1;
        int value = 0;
        ContentResolver cr = context.getContentResolver();
        try {
            value = Settings.System.getInt(cr,
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        result = (float) value / 255;
        return result;
    }

    public static void dimOn(Activity activity) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();

        params.screenBrightness = 0.08f;
        window.setAttributes(params);
    }

    public static void dimOff(Activity activity, float screenBrightness) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();

        if (params.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            params.screenBrightness = getScreenBrightness(activity);
        }

        params.screenBrightness = screenBrightness;

        if (params.screenBrightness > 1.0f) {
            params.screenBrightness = 1.0f;
        }
        if (params.screenBrightness <= 0.01f) {
            params.screenBrightness = 0.01f;
        }
        window.setAttributes(params);
    }

    public static void setArgV0(String text) {
        try {
            Method setter = android.os.Process.class.getMethod("setArgV0", String.class);
            setter.invoke(android.os.Process.class, text);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException");
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException");
        }
    }
}
