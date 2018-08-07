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

package org.arpnetwork.arpdevice.contracts.api;

import org.arpnetwork.arpdevice.ui.wallet.WalletManager;
import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SignatureException;
import java.util.Random;

public class VerifyAPI {

    /**
     * Get a random string
     *
     * @param length length of string wanted
     * @return
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }

        return sb.toString();
    }

    /**
     * Sign random data with password for private key
     *
     * @param signatureContent signature content
     * @param password         password for private key
     * @return
     */
    public static String sign(String signatureContent, String password) {
        byte[] message = toSignatureByte(signatureContent);
        Credentials credentials = WalletManager.getInstance().loadCredentials(password);
        if (credentials == null) return null;
        Sign.SignatureData signatureData = Sign.signMessage(
                message, credentials.getEcKeyPair());

        return toSignatureString(signatureData);
    }

    /**
     * Get address with sign content and signature data string
     *
     * @param signatureContent
     * @param signatureDataBytes
     * @return
     * @throws SignatureException
     */
    public static String getSignatureAddress(String signatureContent, byte[] signatureDataBytes) throws SignatureException {
        byte[] bytes = toSignatureByte(signatureContent);
        BigInteger key = Sign.signedMessageToKey(bytes, getSignatureDataFromByte(signatureDataBytes));

        return Keys.getAddress(key);
    }

    /**
     * Get row transaction string by params
     *
     * @param gasPrice
     * @param gasLimit
     * @param contractAddress
     * @param data
     * @param credentials
     * @return
     */
    public static String getRawTransaction(BigInteger gasPrice, BigInteger gasLimit,
            String contractAddress, String data, Credentials credentials) {
        BigInteger nonce = getTransactionCount(credentials.getAddress());
        RawTransaction rawTransaction  = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, data);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return hexValue;
    }

    private static BigInteger getTransactionCount(String address) {
        EthGetTransactionCount transactionCount = new EthGetTransactionCount();
        try {
            transactionCount = BalanceAPI.getWeb3J()
                    .ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return transactionCount.getTransactionCount();
    }

    private static Sign.SignatureData getSignatureDataFromByte(byte[] bytesBinary) {
        if (bytesBinary.length != (32 + 32 + 1)) return null;

        ByteBuffer buffer = ByteBuffer.wrap(bytesBinary);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(bytesBinary, 0, r, 0, 32);
        System.arraycopy(bytesBinary, 32, s, 0, 32);
        byte v = buffer.get(64);

        return new Sign.SignatureData(v, r, s);
    }

    private static byte[] toSignatureByte(String signatureContent) {
        String string = "Ethereum Signed Message:\n" + signatureContent.length() + signatureContent;
        byte[] stringBytes = string.getBytes();
        byte[] bytes = new byte[stringBytes.length + 1];
        bytes[0] = 0x19;
        System.arraycopy(stringBytes, 0, bytes, 1, stringBytes.length);

        return bytes;
    }

    private static String toSignatureString(Sign.SignatureData signatureData) {
        byte[] signatureByte = new byte[65];
        System.arraycopy(signatureData.getR(), 0, signatureByte, 0, 32);
        System.arraycopy(signatureData.getS(), 0, signatureByte, 32, 32);
        signatureByte[64] = signatureData.getV();

        return Hex.toHexString(signatureByte);
    }
}