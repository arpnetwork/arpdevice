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

import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SignatureException;

public class VerifyAPI {

    /**
     * Sign random data with password for private key
     *
     * @param signatureContent signature content
     * @param password         password for private key
     * @return
     */
    public static String sign(String signatureContent, String password) {
        Credentials credentials = Wallet.get().loadCredentials(password);
        return sign(signatureContent, credentials);
    }

    /**
     * Sign random data with credentials
     *
     * @param signatureContent
     * @param credentials
     * @return
     */
    public static String sign(String signatureContent, Credentials credentials) {
        if (credentials == null) {
            return null;
        }

        return toSignatureString(sign(signatureContent.getBytes(), credentials));
    }

    public static Sign.SignatureData sign(byte[] message, Credentials credentials) {
        return Sign.signMessage(message, credentials.getEcKeyPair());
    }

    /**
     * Verify whether the sign is right
     */
    public static boolean verifySign(String data, String sign, String address) {
        boolean res = false;
        try {
            String addr = getSignatureAddress(data, sign);
            if (Numeric.cleanHexPrefix(address).equalsIgnoreCase(addr)) {
                res = true;
            }
        } catch (Exception e) {
        }
        return res;
    }

    /**
     * Verify promise
     *
     * @param promise
     * @return
     */
    public static boolean isEffectivePromise(Promise promise) {
        Uint256 cid = new Uint256(new BigInteger(promise.getCid(), 16));
        String from = Numeric.cleanHexPrefix(promise.getFrom());
        String sender = Numeric.cleanHexPrefix(promise.getTo());
        Uint256 amount = new Uint256(new BigInteger(promise.getAmount(), 16));

        StringBuilder builder = new StringBuilder();
        builder.append(TypeEncoder.encode(cid));
        builder.append(from);
        builder.append(sender);
        builder.append(TypeEncoder.encode(amount));
        String address = null;
        try {
            address = VerifyAPI.getSignatureAddress(Hex.decode(builder.toString()), Hex.decode(promise.getSign()));
        } catch (SignatureException ignore) {
        }

        return from.toLowerCase().equals(address.toLowerCase());
    }

    /**
     * Get address with sign content and signature data string
     *
     * @param signatureContent
     * @param signatureData
     * @return
     * @throws SignatureException
     */
    public static String getSignatureAddress(String signatureContent, String signatureData) throws SignatureException {
        byte[] bytes = signatureContent.getBytes();
        byte[] signatureDataBytes = Hex.decode(signatureData);

        return getSignatureAddress(bytes, signatureDataBytes);
    }

    public static String getSignatureAddress(byte[] contentBytes, byte[] signatureDataBytes) throws SignatureException {
        BigInteger key = Sign.signedMessageToKey(contentBytes, getSignatureDataFromByte(signatureDataBytes));

        return Keys.getAddress(key);
    }

    public static Sign.SignatureData getSignatureDataFromHexString(String hexString) {
        return getSignatureDataFromByte(Hex.decode(hexString));
    }

    public static Sign.SignatureData getSignatureDataFromByte(byte[] bytesBinary) {
        if (bytesBinary.length != (32 + 32 + 1)) return null;

        ByteBuffer buffer = ByteBuffer.wrap(bytesBinary);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(bytesBinary, 0, r, 0, 32);
        System.arraycopy(bytesBinary, 32, s, 0, 32);
        byte v = buffer.get(64);

        return new Sign.SignatureData(v, r, s);
    }

    public static String toSignatureString(Sign.SignatureData signatureData) {
        byte[] signatureByte = new byte[65];
        System.arraycopy(signatureData.getR(), 0, signatureByte, 0, 32);
        System.arraycopy(signatureData.getS(), 0, signatureByte, 32, 32);
        signatureByte[64] = signatureData.getV();

        return Hex.toHexString(signatureByte);
    }
}
