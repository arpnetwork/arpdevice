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

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

import java.util.List;

@Table(name = "installed_app")
public class InstalledApp extends BaseRecord {
    @Column(name = "pkgName")
    public String pkgName;

    public static boolean exists(String pkgName) {
        return find(pkgName) != null;
    }

    public static List<InstalledApp> findAll() {
        return new Select().from(InstalledApp.class).execute();
    }

    public static InstalledApp find(String pkgName) {
        return new Select().from(InstalledApp.class).where("pkgName = ?", pkgName).executeSingle();
    }

    public static void delete(String pkgName) {
        new Delete().from(InstalledApp.class).where("pkgName = ?", pkgName).execute();
    }
}
