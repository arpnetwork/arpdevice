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

import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InputManagerProxy {
    private static final String TAG = "InputManagerProxy";

    private Object inputManager;
    private Method injectMethod;

    public InputManagerProxy() {
        try {
            inputManager = getSingleton("android.hardware.input.InputManager");
            if (inputManager != null) {
                injectMethod = inputManager.getClass()
                        .getMethod("injectInputEvent", InputEvent.class, int.class);
            }
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "InputManagerEventInjector is not supported");
        }
    }

    public boolean injectKeyEvent(KeyEvent event) {
        if (inputManager == null) return false;

        try {
            injectMethod.invoke(inputManager, event, 0);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    public static Object getSingleton(String className) {
        try {
            Class<?> aClass = Class.forName(className);

            Method getInstance = aClass.getMethod("getInstance");

            return getInstance.invoke(null);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "getSingleton ClassNotFoundException : " + className);
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "getSingleton NoSuchMethodException : " + className);
        } catch (IllegalAccessException e) {
            Log.w(TAG, "getSingleton IllegalAccessException : " + className);
        } catch (InvocationTargetException e) {
            Log.w(TAG, "getSingleton InvocationTargetException : " + className);
        }
        return null;
    }
}
