package com.example.securityprotocol.helper;

import android.util.Base64;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.IOException;

public class Helper {
    public static String encodeToBase64(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    public static byte[] decodeToByteArray(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    public static IdentityKeyPair decodeIdentityKeyPair(String base64) throws IOException {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return new IdentityKeyPair(bytes);  // uses Signal's built-in deserialization
    }

    public static PreKeyRecord decodePreKey(String base64) throws IOException {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return new PreKeyRecord(bytes);
    }

    public static SignedPreKeyRecord decodeSignedPreKey(String base64) throws IOException {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return new SignedPreKeyRecord(bytes);
    }
}
