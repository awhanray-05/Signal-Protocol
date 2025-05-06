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

public class SingleMessagingBob extends AppCompatActivity {
    RegistrationItem alice;
    RegistrationItem bob;

    private MessagingAdapter aliceMessagingAdapter;
    private MessagingAdapter bobMessagingAdapter;

    private String aliceUserId = "100";

    private Button bobSendButton;
    private EditText bobMessageBox;
    private String bobUserId = "200";

    private EncryptedSingleSession aliceEncryptedSession;
    private EncryptedSingleSession bobEncryptedSession;

    private RecyclerView aliceMessagingView;
    private RecyclerView bobMessagingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_messaging_bob);

        bobSendButton = findViewById(R.id.bobSendButton);
        bobMessageBox = findViewById(R.id.bobMessageBox);

        bobMessagingView = findViewById(R.id.bobMessagingView);

        initAlice();
        initBob();

        initBobChatSession();
    }

    private void initBobChatSession() {
        try {
            EncryptedLocalUser bobModel = new EncryptedLocalUser(
                    bob.getIdentityKeyPair().serialize(),
                    bob.getRegistrationId(),
                    "bob",
                    RegistrationManager.DEFAULT_DEVICE_ID,
                    bob.getPreKeysBytes(),
                    bob.signedPreKeyRecord());

            EncryptedRemoteUser aliceModel = new EncryptedRemoteUser(
                    alice.getRegistrationId(),
                    "alice",
                    RegistrationManager.DEFAULT_DEVICE_ID,
                    alice.getPreKeys().get(0).getId(),
                    alice.getPreKeys().get(0).getKeyPair().getPublicKey().serialize(),
                    alice.getSignedPreKeyId(),
                    alice.getSignedPreKeyRecord().getKeyPair().getPublicKey().serialize(),
                    alice.getSignedPreKeyRecord().getSignature(),
                    alice.getIdentityKeyPair().getPublicKey().serialize()
            );

            bobEncryptedSession = new EncryptedSingleSession(
                    bobModel,
                    aliceModel
            );

            bobSendButton.setOnClickListener(view -> onMessageSendFromBob());

        } catch (InvalidKeyException | IOException | UntrustedIdentityException e) {
            e.printStackTrace();
        }
    }

    private void onMessageSendFromBob() {
        String text = bobMessageBox.getText().toString();

        if (text.isEmpty()) return;

        try {
            String encryptedMessage = bobEncryptedSession.encrypt(text);
            String id = UUID.randomUUID().toString();
            Log.d("Bob: ","MessageSentFromBobAfterEncryption: " + encryptedMessage + ", id: " + id);
            bobMessagingAdapter.onNewMessage(new Message(
                    id,
                    text,
                    aliceUserId,
                    bobUserId
            ));

            onNewMessageFromBob(new Message(
                    id,
                    encryptedMessage,
                    bobUserId,
                    aliceUserId
            ));
        } catch (UntrustedIdentityException | InvalidKeyException | InvalidMessageException | InvalidVersionException e) {
            e.printStackTrace();
        }
    }

    private void initAlice() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document("alice")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AliceKeyData data = documentSnapshot.toObject(AliceKeyData.class);
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
                                alice = new RegistrationItem(identityKeyPair, registrationId, preKeyRecords, signedPreKey);

                                Log.d("ALICE-Fetch", "Alice's key data successfully initialized into RegistrationItem");
                            } catch (Exception e) {
                                Log.e("ALICE-Fetch", "Failed to decode Alice key data", e);
                            }
                        }
                    } else {
                        Log.w("ALICE-Fetch", "No Alice key data found in Firestore.");
                    }
                })
                .addOnFailureListener(e -> Log.e("ALICE-Fetch", "Failed to fetch Alice keys", e));
    }

    private void initBob() {
        try {
            bob = RegistrationManager.generateKeys();

            String identityKeyPairBase64 = Helper.encodeToBase64(bob.getIdentityKeyPair().serialize());
            int registrationId = bob.getRegistrationId();

            List<String> preKeysBase64 = new ArrayList<>();
            for (byte[] preKey : bob.getPreKeysBytes()) {
                preKeysBase64.add(Helper.encodeToBase64(preKey));
            }

            String signedPreKeyBase64 = Helper.encodeToBase64(bob.signedPreKeyRecord());

            Log.d("BOB-KeyGen", "identityKeyPair: " + identityKeyPairBase64);
            Log.d("BOB-KeyGen", "registrationId: " + registrationId);
            for (String pk : preKeysBase64) {
                Log.d("BOB-KeyGen", "preKey: " + pk);
            }
            Log.d("BOB-KeyGen", "signedPreKey: " + signedPreKeyBase64);

            storeBobKeysToFirestore(identityKeyPairBase64, registrationId, preKeysBase64, signedPreKeyBase64);
        } catch (InvalidKeyException | IOException e) {
            e.printStackTrace();
        }

        bobMessagingAdapter = new MessagingAdapter();
        bobMessagingView.setAdapter(bobMessagingAdapter);
    }

    private void storeBobKeysToFirestore(String identityKeyPair, int registrationId, List<String> preKeys, String signedPreKey) {
        BobKeyData data = new BobKeyData(identityKeyPair, registrationId, preKeys, signedPreKey);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document("bob")
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Bob keys stored successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to store Bob keys", e));
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
