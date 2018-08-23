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

package org.arpnetwork.arpdevice.database;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;

public abstract class BaseRecord extends Model {
    @Column(name = "create_at")
    public long createAt;

    @Column(name = "update_at")
    public long updateAt;

    public BaseRecord() {
        createAt = System.currentTimeMillis();
        updateAt = createAt;
    }

    public boolean isNewRecord() {
        return getId() == null;
    }

    public Long saveRecord() {
        updateAt = System.currentTimeMillis();
        return save();
    }
}
