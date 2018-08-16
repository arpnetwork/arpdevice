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

import android.content.Context;

import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.R;

import org.spongycastle.util.encoders.Hex;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Util {
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

    public static byte[] stringToBytes32(String address) {
        String addrCleanPrefix = Numeric.cleanHexPrefix(address);

        byte[] bytes = Hex.decode(addrCleanPrefix);
        byte[] address32 = new byte[32];
        System.arraycopy(bytes, 0, address32, 32 - bytes.length, bytes.length);
        return address32;
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

}
