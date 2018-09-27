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
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

import org.web3j.utils.Numeric;

import java.util.List;

@Table(name = "transaction_record")
public class TransactionRecord extends BaseRecord {
    @Column(name = "hash", index = true)
    public String hash;

    @Column(name = "opType")
    public int opType;

    @Column(name = "args")
    public String args;

    public static List<TransactionRecord> findAll() {
        return new Select().from(TransactionRecord.class).orderBy("create_at DESC").execute();
    }

    public static TransactionRecord get(String transactionHash) {
        if (TextUtils.isEmpty(transactionHash)) {
            return null;
        }
        TransactionRecord record = find(transactionHash);
        if (record == null) {
            record = new TransactionRecord();
            record.hash = transactionHash;
        }
        return record;
    }

    public static TransactionRecord find(String transactionHash) {
        return new Select().from(TransactionRecord.class).where("hash = ?", transactionHash).executeSingle();
    }

    public static void delete(String transactionHash, int opType) {
        new Delete().from(TransactionRecord.class).where("hash = ? and opType = ? ", transactionHash, opType).execute();
    }

}
