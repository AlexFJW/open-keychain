/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.service.input;

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.ParcelableLong;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a base class for the input of crypto operations.
 */
public class CryptoInputParcel implements Parcelable {

    private Date mSignatureTime;
    private boolean mHasSignature;

    public Passphrase mPassphrase;

    // used for backing up of secret keys
    private final HashMap<Long, Passphrase> mPassphrases;

    // used to supply an explicit proxy to operations that require it
    // this is not final so it can be added to an existing CryptoInputParcel
    // (e.g) CertifyOperation with upload might require both passphrase and orbot to be enabled
    private ParcelableProxy mParcelableProxy;

    // specifies whether passphrases should be cached
    public boolean mCachePassphrase = true;

    // this map contains both decrypted session keys and signed hashes to be
    // used in the crypto operation described by this parcel.
    private HashMap<ByteBuffer, byte[]> mCryptoData = new HashMap<>();

    public CryptoInputParcel() {
        mSignatureTime = null;
        mPassphrase = null;
        mCachePassphrase = true;
        mPassphrases = null;
    }

    public CryptoInputParcel(Date signatureTime, Passphrase passphrase) {
        mHasSignature = true;
        mSignatureTime = signatureTime == null ? new Date() : signatureTime;
        mPassphrase = passphrase;
        mCachePassphrase = true;
        mPassphrases = null;
    }

    public CryptoInputParcel(Passphrase passphrase) {
        mPassphrase = passphrase;
        mCachePassphrase = true;
        mPassphrases = null;
    }

    public CryptoInputParcel(HashMap<Long, Passphrase> parcelablePassphrases) {
        mPassphrases = parcelablePassphrases;
    }

    public CryptoInputParcel(Date signatureTime) {
        mHasSignature = true;
        mSignatureTime = signatureTime == null ? new Date() : signatureTime;
        mPassphrase = null;
        mCachePassphrase = true;
        mPassphrases = null;
    }

    public CryptoInputParcel(ParcelableProxy parcelableProxy) {
        this();
        mParcelableProxy =  parcelableProxy;
    }

    public CryptoInputParcel(Date signatureTime, boolean cachePassphrase) {
        mHasSignature = true;
        mSignatureTime = signatureTime == null ? new Date() : signatureTime;
        mPassphrase = null;
        mCachePassphrase = cachePassphrase;
        mPassphrases = null;
    }

    public CryptoInputParcel(boolean cachePassphrase) {
        mCachePassphrase = cachePassphrase;
        mPassphrases = null;
    }

    protected CryptoInputParcel(Parcel source) {
        mHasSignature = source.readByte() != 0;
        if (mHasSignature) {
            mSignatureTime = new Date(source.readLong());
        }
        mPassphrase = source.readParcelable(getClass().getClassLoader());
        mParcelableProxy = source.readParcelable(getClass().getClassLoader());
        mCachePassphrase = source.readByte() != 0;

        {
            int count = source.readInt();
            mCryptoData = new HashMap<>(count);
            for (int i = 0; i < count; i++) {
                byte[] key = source.createByteArray();
                byte[] value = source.createByteArray();
                mCryptoData.put(ByteBuffer.wrap(key), value);
            }
        }
        ParcelableHashMap<ParcelableLong, Passphrase> parcelablePassphrases =
                source.readParcelable(ParcelableHashMap.class.getClassLoader());
        mPassphrases = ParcelableHashMap.toHashMap(parcelablePassphrases);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mHasSignature ? 1 : 0));
        if (mHasSignature) {
            dest.writeLong(mSignatureTime.getTime());
        }
        dest.writeParcelable(mPassphrase, 0);
        dest.writeParcelable(mParcelableProxy, 0);
        dest.writeByte((byte) (mCachePassphrase ? 1 : 0));

        dest.writeInt(mCryptoData.size());
        for (HashMap.Entry<ByteBuffer, byte[]> entry : mCryptoData.entrySet()) {
            dest.writeByteArray(entry.getKey().array());
            dest.writeByteArray(entry.getValue());
        }
        dest.writeParcelable(ParcelableHashMap.toParcelableHashMap(mPassphrases), 0);
    }

    public void addParcelableProxy(ParcelableProxy parcelableProxy) {
        mParcelableProxy = parcelableProxy;
    }

    public void addSignatureTime(Date signatureTime) {
        mSignatureTime = signatureTime;
    }

    public void addCryptoData(byte[] hash, byte[] signedHash) {
        mCryptoData.put(ByteBuffer.wrap(hash), signedHash);
    }

    public void addCryptoData(Map<ByteBuffer, byte[]> cachedSessionKeys) {
        mCryptoData.putAll(cachedSessionKeys);
    }

    public ParcelableProxy getParcelableProxy() {
        return mParcelableProxy;
    }

    public Map<ByteBuffer, byte[]> getCryptoData() {
        return mCryptoData;
    }

    public Date getSignatureTime() {
        return mSignatureTime;
    }

    public boolean hasPassphrase() {
        return mPassphrase != null;
    }

    public Passphrase getPassphrase() {
        return mPassphrase;
    }

    public HashMap<Long, Passphrase> getPassphrases() {
        return mPassphrases;
    }

    public static final Creator<CryptoInputParcel> CREATOR = new Creator<CryptoInputParcel>() {
        public CryptoInputParcel createFromParcel(final Parcel source) {
            return new CryptoInputParcel(source);
        }

        public CryptoInputParcel[] newArray(final int size) {
            return new CryptoInputParcel[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("CryptoInput: { ");
        b.append(mSignatureTime).append(" ");
        if (mPassphrase != null) {
            b.append("passphrase ");
        }
        if (mPassphrases != null) {
            b.append("passphrases ");
        }
        if (mCryptoData != null) {
            b.append(mCryptoData.size());
            b.append(" hashes ");
        }
        b.append("}");
        return b.toString();
    }

}
