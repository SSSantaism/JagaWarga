package com.example.jagawarga;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Service untuk menangani Firebase Cloud Messaging
 * Digunakan untuk menerima notifikasi pengumuman dari FCM Topics
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // Token bisa disimpan ke Firestore jika diperlukan di masa depan
        // Untuk saat ini, kita menggunakan Topics sehingga tidak perlu menyimpan token
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Pastikan notification channels sudah dibuat
        NotificationHelper.createNotificationChannels(this);

        // Cek apakah ada data payload
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");

            if ("pengumuman".equals(type)) {
                String judul = data.get("judul");
                String isi = data.get("isi");
                if (judul != null && isi != null) {
                    NotificationHelper.showPengumumanNotification(this, judul, isi);
                }
            }
        }

        // Cek apakah ada notification payload (dari Firebase Console)
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            if (title != null && body != null) {
                NotificationHelper.showPengumumanNotification(this, title, body);
            }
        }
    }
}
