package com.example.jagawarga;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * BroadcastReceiver untuk menangani aksi tombol pada notifikasi tukar jadwal.
 * Menangani aksi "Setuju" dan "Tolak" langsung dari notification action
 * buttons.
 */
public class TukarJadwalActionReceiver extends BroadcastReceiver {

    private static final String TAG = "TukarJadwalAction";

    public static final String ACTION_ACCEPT = "com.example.jagawarga.ACTION_ACCEPT_SWAP";
    public static final String ACTION_REJECT = "com.example.jagawarga.ACTION_REJECT_SWAP";

    public static final String EXTRA_DOC_ID = "doc_id";
    public static final String EXTRA_NOTIF_ID = "notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String docId = intent.getStringExtra(EXTRA_DOC_ID);
        int notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0);

        if (docId == null || docId.isEmpty()) {
            Log.e(TAG, "Document ID is null or empty");
            return;
        }

        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notifId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (ACTION_ACCEPT.equals(action)) {
            handleAccept(context, db, docId);
        } else if (ACTION_REJECT.equals(action)) {
            handleReject(context, db, docId);
        }
    }

    /**
     * Handle aksi Setuju - tukar jadwal antara kedua user
     */
    private void handleAccept(Context context, FirebaseFirestore db, String docId) {
        Log.d(TAG, "Accepting swap request: " + docId);

        db.collection("tukar_jadwal").document(docId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showToast(context, "Permintaan tidak ditemukan");
                        return;
                    }

                    String dariId = document.getString("dari_id");
                    String kepadaId = document.getString("kepada_id");
                    String hariDari = document.getString("hari_dari");
                    String hariKepada = document.getString("hari_kepada");
                    String jadwalIdDari = document.getString("dari_jadwal_id");
                    String jadwalIdKepada = document.getString("kepada_jadwal_id");

                    if (dariId == null || kepadaId == null) {
                        showToast(context, "Data tidak lengkap");
                        return;
                    }

                    // Tukar jadwal_hari dan jadwal_id antara kedua user
                    performSwap(context, db, docId,
                            dariId, kepadaId,
                            hariDari, hariKepada,
                            jadwalIdDari, jadwalIdKepada);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting swap document: " + e.getMessage());
                    showToast(context, "Gagal memproses: " + e.getMessage());
                });
    }

    /**
     * Melakukan pertukaran jadwal antara 2 user
     */
    private void performSwap(Context context, FirebaseFirestore db, String docId,
            String dariId, String kepadaId,
            String hariDari, String hariKepada,
            String jadwalIdDari, String jadwalIdKepada) {

        // Update user "dari" dengan jadwal "kepada"
        Map<String, Object> updateDari = new HashMap<>();
        updateDari.put("jadwal_hari", hariKepada);
        updateDari.put("jadwal_id", jadwalIdKepada);

        // Update user "kepada" dengan jadwal "dari"
        Map<String, Object> updateKepada = new HashMap<>();
        updateKepada.put("jadwal_hari", hariDari);
        updateKepada.put("jadwal_id", jadwalIdDari);

        // Batch update
        db.runTransaction(transaction -> {
            // Update kedua user
            transaction.update(db.collection("users").document(dariId), updateDari);
            transaction.update(db.collection("users").document(kepadaId), updateKepada);

            // Update status permintaan
            transaction.update(db.collection("tukar_jadwal").document(docId), "status", "accepted");

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Swap completed successfully");
            showToast(context, "✅ Jadwal berhasil ditukar!");

            // Update local session jika user yang accept adalah current user
            String currentUserId = PrefUtils.getIdWarga(context);
            if (kepadaId.equals(currentUserId)) {
                PrefUtils.setJadwalHari(context, hariDari);
                // Reschedule reminder dengan jadwal baru
                RondaReminderManager.scheduleWeeklyReminder(context, hariDari);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Swap failed: " + e.getMessage());
            showToast(context, "Gagal menukar jadwal: " + e.getMessage());
        });
    }

    /**
     * Handle aksi Tolak
     */
    private void handleReject(Context context, FirebaseFirestore db, String docId) {
        Log.d(TAG, "Rejecting swap request: " + docId);

        db.collection("tukar_jadwal").document(docId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Swap request rejected");
                    showToast(context, "❌ Permintaan tukar jadwal ditolak");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error rejecting: " + e.getMessage());
                    showToast(context, "Gagal: " + e.getMessage());
                });
    }

    private void showToast(Context context, String message) {
        // Toast dari BroadcastReceiver perlu handler
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
