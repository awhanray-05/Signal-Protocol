package com.example.securityprotocol.data;

import java.util.List;

public class BobKeyData {
    public String identityKeyPair;
    public int registrationId;
    public List<String> preKeys;
    public String signedPreKey;

    public BobKeyData(String identityKeyPair, int registrationId, List<String> preKeys, String signedPreKey) {
        this.identityKeyPair = identityKeyPair;
        this.registrationId = registrationId;
        this.preKeys = preKeys;
        this.signedPreKey = signedPreKey;
    }

    public String getIdentityKeyPair() { return identityKeyPair; }
    public int getRegistrationId() { return registrationId; }
    public List<String> getPreKeys() { return preKeys; }
    public String getSignedPreKey() { return signedPreKey; }
}
