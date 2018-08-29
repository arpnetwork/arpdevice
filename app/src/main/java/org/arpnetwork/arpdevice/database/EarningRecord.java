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

import android.text.TextUtils;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.List;

@Table(name = "earning_record")
public class EarningRecord extends BaseRecord {
    public static final int STATE_SUCCESS = 1;
    public static final int STATE_PENDING = 2;
    public static final int STATE_FAILED = 3;

    @Column(name = "key", index = true)
    public String key;

    @Column(name = "minerAddress")
    public String minerAddress;
    @Column(name = "time")
    public long time;
    @Column(name = "earning")
    public String earning;

    @Column(name = "blockNumber")
    public String blockNumber;
    @Column(name = "state")
    public int state;

    public BigInteger getEarning() {
        return new BigInteger(earning);
    }

    public float getFloatEarning() {
        return Convert.fromWei(earning, Convert.Unit.ETHER).floatValue();
    }

    public static List<EarningRecord> findAll() {
        return new Select().from(EarningRecord.class).orderBy("create_at DESC").execute();
    }

    public static EarningRecord get(String transactionHash) {
        if (TextUtils.isEmpty(transactionHash)) {
            return null;
        }
        EarningRecord record = find(transactionHash);
        if (record == null) {
            record = new EarningRecord();
            record.key = transactionHash;
        }
        return record;
    }

    public static EarningRecord find(String key) {
        return new Select().from(EarningRecord.class).where("key = ?", key).executeSingle();
    }

    public static EarningRecord find(int state) {
        return new Select().from(EarningRecord.class).where("state = ?", state).executeSingle();
    }

    public static EarningRecord findTop() {
        return new Select().from(EarningRecord.class).orderBy("create_at DESC").limit(1).executeSingle();
    }

    public static EarningRecord findTopWithState(int state) {
        return new Select().from(EarningRecord.class).where("state = ?", state).orderBy("create_at DESC").limit(1).executeSingle();
    }

    public BigInteger getCid() {
        String cid = key.split(":")[0];
        return new BigInteger(cid, 16);
    }

}
