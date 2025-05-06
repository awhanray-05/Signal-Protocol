package com.example.securityprotocol.ux;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securityprotocol.R;
import com.example.securityprotocol.data.AliceKeyData;
import com.example.securityprotocol.data.BobKeyData;
import com.example.securityprotocol.entity.EncryptedLocalUser;
import com.example.securityprotocol.entity.EncryptedRemoteUser;
import com.example.securityprotocol.helper.Helper;
import com.example.securityprotocol.registration.RegistrationItem;
import com.example.securityprotocol.registration.RegistrationManager;
import com.example.securityprotocol.singleConversation.EncryptedSingleSession;
import com.google.firebase.firestore.FirebaseFirestore;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SingleMessagingAlice extends AppCompatActivity {
    RegistrationItem alice;
    RegistrationItem bob;

    private MessagingAdapter aliceMessagingAdapter;
    private MessagingAdapter bobMessagingAdapter;

    private Button aliceSendButton;
    private EditText aliceMessageBox;
    private String aliceUserId = "100";

    private String bobUserId = "200";

    private EncryptedSingleSession aliceEncryptedSession;
    private EncryptedSingleSession bobEncryptedSession;

    private RecyclerView aliceMessagingView;
    private RecyclerView bobMessagingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_messaging_alice);

        aliceSendButton = findViewById(R.id.aliceSendButton);
        aliceMessageBox = findViewById(R.id.aliceMessageBox);

        aliceMessagingView = findViewById(R.id.aliceMessagingView);

        initAlice();
        initBob();

        initAliceChatSession();
    }

    private void initAliceChatSession() {
        try {
            EncryptedLocalUser aliceModel = new EncryptedLocalUser(
                    alice.getIdentityKeyPair().serialize(),
                    alice.getRegistrationId(),
                    "alice",
                    RegistrationManager.DEFAULT_DEVICE_ID,
                    alice.getPreKeysBytes(),
                    alice.signedPreKeyRecord());

            EncryptedRemoteUser bobModel = new EncryptedRemoteUser(
                    bob.getRegistrationId(),
                    "bob",
                    RegistrationManager.DEFAULT_DEVICE_ID,
                    bob.getPreKeys().get(0).getId(),
                    bob.getPreKeys().get(0).getKeyPair().getPublicKey().serialize(),
                    bob.getSignedPreKeyId(),
                    bob.getSignedPreKeyRecord().getKeyPair().getPublicKey().serialize(),
                    bob.getSignedPreKeyRecord().getSignature(),
                    bob.getIdentityKeyPair().getPublicKey().serialize()
            );

            aliceEncryptedSession = new EncryptedSingleSession(
                    aliceModel,
                    bobModel
            );

            aliceSendButton.setOnClickListener(view -> {
                onMessageSendFromAlice();
            });

        } catch (InvalidKeyException | IOException | UntrustedIdentityException e) {
            e.printStackTrace();
        }
    }

    private void onMessageSendFromAlice() {
        String text = aliceMessageBox.getText().toString();

        if (text.isEmpty()) return;

        try {
            String encryptedMessage = aliceEncryptedSession.encrypt(text);
            String id = UUID.randomUUID().toString();
            Log.d("Alice: ","MessageSentFromAliceAfterEncryption: " + encryptedMessage + ", id: " + id);
            aliceMessagingAdapter.onNewMessage(new Message(
                    id,
                    text,
                    aliceUserId,
                    bobUserId
            ));
            onNewMessageFromAlice(new Message(
                    id,
                    encryptedMessage,
                    aliceUserId,
                    bobUserId
            ));
        } catch (UntrustedIdentityException | InvalidKeyException | InvalidMessageException | InvalidVersionException e) {
            e.printStackTrace();
        }
    }

    private void initAlice() {
        try {
            alice = RegistrationManager.generateKeys();

            String identityKeyPairBase64 = Helper.encodeToBase64(alice.getIdentityKeyPair().serialize());
            int registrationId = alice.getRegistrationId();

            List<String> preKeysBase64 = new ArrayList<>();
            for (byte[] preKey : alice.getPreKeysBytes()) {
                preKeysBase64.add(Helper.encodeToBase64(preKey));
            }

            String signedPreKeyBase64 = Helper.encodeToBase64(alice.signedPreKeyRecord());

            Log.d("ALICE-KeyGen", "identityKeyPair: " + identityKeyPairBase64);
            Log.d("ALICE-KeyGen", "registrationId: " + registrationId);
            for (String pk : preKeysBase64) {
                Log.d("ALICE-KeyGen", "preKey: " + pk);
            }
            Log.d("ALICE-KeyGen", "signedPreKey: " + signedPreKeyBase64);

            storeAliceKeysToFirestore(identityKeyPairBase64, registrationId, preKeysBase64, signedPreKeyBase64);
        } catch (InvalidKeyException | IOException e) {
            e.printStackTrace();
        }

        aliceMessagingAdapter = new MessagingAdapter();
        aliceMessagingView.setAdapter(aliceMessagingAdapter);
    }

    private void storeAliceKeysToFirestore(String identityKeyPair, int registrationId, List<String> preKeys, String signedPreKey) {
        AliceKeyData data = new AliceKeyData(identityKeyPair, registrationId, preKeys, signedPreKey);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document("alice")
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Alice keys stored successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to store Alice keys", e));
    }

//    private void initBob() {
//        try {
//            fetchBobKeysFromFirestore();
//            bob = RegistrationManager.generateKeys();
//
//            Log.d("BOB-KeyGen", "identityKeyPair: " + Helper.encodeToBase64(alice.getIdentityKeyPair().serialize()));
//            Log.d("BOB-KeyGen", "registrationId: " + alice.getRegistrationId());
//
//            for (byte[] preKey : alice.getPreKeysBytes()) {
//                Log.d("BOB-KeyGen", "preKey: " + Helper.encodeToBase64(preKey));
//            }
//
//            Log.d("BOB-KeyGen", "signedPreKey: " + Helper.encodeToBase64(alice.signedPreKeyRecord()));
//        } catch (InvalidKeyException | IOException e) {
//            e.printStackTrace();
//        }
//        bobMessagingAdapter = new MessagingAdapter();
//        bobMessagingView.setAdapter(bobMessagingAdapter);
//    }

    private void initBob() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document("bob")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        BobKeyData data = documentSnapshot.toObject(BobKeyData.class);
                        if (data != null) {
                            try {
                                // Deserialize each field
                                IdentityKeyPair identityKeyPair = Helper.decodeIdentityKeyPair(data.getIdentityKeyPair());

                                int registrationId = data.getRegistrationId();

                                List<PreKeyRecord> preKeyRecords = new ArrayList<>();
                                for (String preKeyString : data.getPreKeys()) {
                                    PreKeyRecord preKey = Helper.decodePreKey(preKeyString);
                                    preKeyRecords.add(preKey);
                                }

                                SignedPreKeyRecord signedPreKey = Helper.decodeSignedPreKey(data.getSignedPreKey());

                                // Construct RegistrationItem
                                bob = new RegistrationItem(identityKeyPair, registrationId, preKeyRecords, signedPreKey);

                                Log.d("BOB-Fetch", "Bob's key data successfully initialized into RegistrationItem");
                            } catch (Exception e) {
                                Log.e("BOB-Fetch", "Failed to decode Bob key data", e);
                            }
                        }
                    } else {
                        Log.w("BOB-Fetch", "No Bob key data found in Firestore.");
                    }
                })
                .addOnFailureListener(e -> Log.e("BOB-Fetch", "Failed to fetch Bob keys", e));
    }

    private void onNewMessageFromAlice(Message remoteMessage) {
        try {
            String encryptedMessage = remoteMessage.getMessage();
            String decryptedMessage = bobEncryptedSession.decrypt(encryptedMessage);
            Log.d("Bob: ","MessageReceivedFromAliceAfterDecryption: " + decryptedMessage);
            bobMessagingAdapter.onNewMessage(new Message(
                    remoteMessage.getId(),
                    decryptedMessage,
                    remoteMessage.getFromId(),
                    remoteMessage.toString()
            ));
        } catch (UntrustedIdentityException | InvalidKeyException | InvalidMessageException | InvalidVersionException | DuplicateMessageException | InvalidKeyIdException | LegacyMessageException e) {
            e.printStackTrace();
        }
    }
    private void onNewMessageFromBob(Message remoteMessage) {
        try {
            String encryptedMessage = remoteMessage.getMessage();
            String decryptedMessage = aliceEncryptedSession.decrypt(encryptedMessage);
            Log.d("Alice: ","MessageReceivedFromBobAfterDecryption: " + decryptedMessage);
            aliceMessagingAdapter.onNewMessage(new Message(
                    remoteMessage.getId(),
                    decryptedMessage,
                    remoteMessage.getFromId(),
                    remoteMessage.toString()
            ));
        } catch (UntrustedIdentityException | InvalidKeyException | InvalidMessageException | InvalidVersionException | DuplicateMessageException | InvalidKeyIdException | LegacyMessageException e) {
            e.printStackTrace();
        }
    }
}
