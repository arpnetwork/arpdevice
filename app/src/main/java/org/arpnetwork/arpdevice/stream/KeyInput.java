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

package org.arpnetwork.arpdevice.stream;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import org.arpnetwork.arpdevice.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * proguard tips: keep this class.add line below
 * <p>
 * -keep class org.arpnetwork.arpdevice.stream.KeyInput
 */
public class KeyInput {
    private static final String TAG = "KeyInput";
    public static final String PROCESS_NAME = "arpinput";

    private InputManagerProxy inputManager;

    private int deviceId = -1; // KeyCharacterMap.VIRTUAL_KEYBOARD
    private KeyCharacterMap keyCharacterMap;

    public static void main(String[] args) {
        Util.setArgV0(PROCESS_NAME);

        new KeyInput().run();
    }

    private void run() {
        inputManager = new InputManagerProxy();

        selectDevice();
        loadKeyCharacterMap();
        waitForClients();
    }

    private void waitForClients() {
        try {
            BufferedReader stdIn = new BufferedReader(
                    new InputStreamReader(System.in));
            String line;
            while ((line = stdIn.readLine()) != null) {
                keyPress(KeyEvent.KEYCODE_BACK, 0);
            }
        } catch (IOException e) {
            Log.w(TAG, "IOException");
        }
    }

    private void selectDevice() {
        try {
            deviceId = KeyCharacterMap.class.getDeclaredField("VIRTUAL_KEYBOARD")
                    .getInt(KeyCharacterMap.class);
        } catch (NoSuchFieldException e) {
            Log.w(TAG, "Falling back to KeyCharacterMap.BUILT_IN_KEYBOARD");
            deviceId = 0;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadKeyCharacterMap() {
        keyCharacterMap = KeyCharacterMap.load(deviceId);
    }

    private void keyDown(int keyCode, int metaState) {
        long time = SystemClock.uptimeMillis();
        inputManager.injectKeyEvent(new KeyEvent(
                time,
                time,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                metaState,
                deviceId,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD
        ));
    }

    private void keyUp(int keyCode, int metaState) {
        long time = SystemClock.uptimeMillis();
        inputManager.injectKeyEvent(new KeyEvent(
                time,
                time,
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                metaState,
                deviceId,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD
        ));
    }

    private void keyPress(int keyCode, int metaState) {
        keyDown(keyCode, metaState);
        keyUp(keyCode, metaState);
    }

}
