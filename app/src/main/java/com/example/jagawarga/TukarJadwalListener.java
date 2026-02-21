package com.example.jagawarga;

import android.content.Context;
import android.util.Log;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Listener untuk permintaan tukar jadwal dari Firestore
 * Menampilkan notifikasi lokal ketika ada permintaan baru
 */
public class TukarJadwalListener {

    private static final String TAG = "TukarJadwalListener";
    private static ListenerRegistration listenerRegistration;

    /**
     * Memulai listener untuk permintaan tukar jadwal
     * 
     * @param context Context aplikasi
     * @param userId  ID user yang sedang login
     */
    public static void startListening(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "userId is null or empty, cannot start listener");
            return;
        }

        // Hentikan listener sebelumnya jika ada
        stopListening();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Listen ke collection tukar_jadwal dimana user ini adalah target
        listenerRegistration = db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                .whereEqualTo(Constants.FIELD_KEPADA_ID, userId)
                .whereEqualTo(Constants.FIELD_STATUS, Constants.STATUS_PENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null)
                        return;

                    // Pastikan notification channels sudah dibuat
                    NotificationHelper.createNotificationChannels(context);

                    // Cek perubahan document
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            // Ada permintaan tukar jadwal baru
                            String docId = dc.getDocument().getId();
                            String dariNama = dc.getDocument().getString(Constants.FIELD_DARI_NAMA);
                            String hariTukar = dc.getDocument().getString(Constants.FIELD_HARI_DARI);

                            if (dariNama != null && hariTukar != null) {
                                Log.d(TAG, "New swap request from: " + dariNama + ", docId: " + docId);
                                NotificationHelper.showTukarJadwalNotification(
                                        context,
                                        dariNama,
                                        hariTukar,
                                        docId);
                            }
                        }
                    }
                });

        Log.d(TAG, "Started listening for swap requests for user: " + userId);
    }

    /**
     * Menghentikan listener
     */
    public static void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
            Log.d(TAG, "Stopped listening for swap requests");
        }
    }
}
