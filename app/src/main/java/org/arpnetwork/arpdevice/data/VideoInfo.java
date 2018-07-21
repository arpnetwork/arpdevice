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

package org.arpnetwork.arpdevice.data;

public class VideoInfo {
    public int width;
    public int height;
    public int quality;
    public int statusBarHeight;
    public int virtualBarHeight;
    public int resolutionWidth;
    public int resolutionHeight;

    public VideoInfo(int width, int height, int quality, int statusBarHeight, int virtualBarHeight,
            int resolutionWidth, int resolutionHeight) {
        this.width = width;
        this.height = height;
        this.quality = quality;
        this.statusBarHeight = statusBarHeight;
        this.virtualBarHeight = virtualBarHeight;
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
    }
}
