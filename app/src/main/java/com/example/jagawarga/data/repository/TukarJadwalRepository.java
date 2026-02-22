package com.example.jagawarga.data.repository;

import android.util.Log;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository untuk semua operasi Firestore terkait Tukar Jadwal.
 * Dipakai oleh TukarJadwalViewModel dan TukarJadwalActionReceiver.
 */
public class TukarJadwalRepository {

    private static final String TAG = "TukarJadwalRepo";
    private final FirebaseFirestore db;

    // ========================================================================
    // Callback Interfaces
    // ========================================================================

    public interface FindTargetCallback {
        void onFound(String userId, String nama, String jadwalHari);

        void onNotFound();

        void onError(String errorMessage);
    }

    public interface SwapRequestCallback {
        void onSuccess(String targetNama);

        void onAlreadyExists();

        void onError(String errorMessage);
    }

    public interface SwapActionCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    public interface SwapDocCallback {
        void onSuccess(String dariId, String kepadaId,
                String hariDari, String hariKepada,
                String jadwalIdDari, String jadwalIdKepada);

        void onNotFound();

        void onError(String errorMessage);
    }

    public TukarJadwalRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Find Target User
    // ========================================================================

    /**
     * Cari user dengan jadwal_id tertentu dalam RT yang sama.
     */
    public void findTargetUser(String targetJadwalId, String idRt, FindTargetCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_JADWAL_ID, targetJadwalId)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onNotFound();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        callback.onFound(
                                doc.getId(),
                                doc.getString(Constants.FIELD_NAMA),
                                doc.getString(Constants.FIELD_JADWAL_HARI));
                        break; // Hanya ambil satu
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding target: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // Send Swap Request
    // ========================================================================

    /**
     * Kirim permintaan tukar jadwal setelah target ditemukan.
     * Cek duplikat pending request dulu sebelum kirim.
     */
    public void sendSwapRequest(String myUserId, String myNama, String myJadwalId,
            String myJadwalHari, String myIdRt,
            String targetUserId, String targetNama,
            String targetJadwalHari, String targetJadwalId,
            SwapRequestCallback callback) {
        // Cek duplikat
        db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                .whereEqualTo(Constants.FIELD_DARI_ID, myUserId)
                .whereEqualTo(Constants.FIELD_KEPADA_ID, targetUserId)
                .whereEqualTo(Constants.FIELD_STATUS, Constants.STATUS_PENDING)
                .get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        callback.onAlreadyExists();
                        return;
                    }

                    Map<String, Object> swapRequest = new HashMap<>();
                    swapRequest.put(Constants.FIELD_DARI_ID, myUserId);
                    swapRequest.put(Constants.FIELD_DARI_NAMA, myNama);
                    swapRequest.put(Constants.FIELD_DARI_JADWAL_ID, myJadwalId);
                    swapRequest.put(Constants.FIELD_HARI_DARI, myJadwalHari);
                    swapRequest.put(Constants.FIELD_KEPADA_ID, targetUserId);
                    swapRequest.put(Constants.FIELD_KEPADA_NAMA, targetNama);
                    swapRequest.put(Constants.FIELD_KEPADA_JADWAL_ID, targetJadwalId);
                    swapRequest.put(Constants.FIELD_HARI_KEPADA, targetJadwalHari);
                    swapRequest.put(Constants.FIELD_ID_RT, myIdRt);
                    swapRequest.put(Constants.FIELD_STATUS, Constants.STATUS_PENDING);
                    swapRequest.put(Constants.FIELD_CREATED_AT, Timestamp.now());

                    db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                            .add(swapRequest)
                            .addOnSuccessListener(ref -> {
                                Log.d(TAG, "Swap request created: " + ref.getId());
                                callback.onSuccess(targetNama);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sending request: " + e.getMessage());
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // Accept / Reject Swap (used by TukarJadwalActionReceiver)
    // ========================================================================

    /**
     * Ambil data swap request dari Firestore.
     */
    public void getSwapDocument(String docId, SwapDocCallback callback) {
        db.collection(Constants.COLLECTION_TUKAR_JADWAL).document(docId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onNotFound();
                        return;
                    }

                    callback.onSuccess(
                            document.getString(Constants.FIELD_DARI_ID),
                            document.getString(Constants.FIELD_KEPADA_ID),
                            document.getString(Constants.FIELD_HARI_DARI),
                            document.getString(Constants.FIELD_HARI_KEPADA),
                            document.getString(Constants.FIELD_DARI_JADWAL_ID),
                            document.getString(Constants.FIELD_KEPADA_JADWAL_ID));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting swap document: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Eksekusi swap: tukar jadwal_hari dan jadwal_id antara kedua user,
     * dan update status permintaan ke "accepted".
     */
    public void performSwap(String docId,
            String dariId, String kepadaId,
            String hariDari, String hariKepada,
            String jadwalIdDari, String jadwalIdKepada,
            SwapActionCallback callback) {
        Map<String, Object> updateDari = new HashMap<>();
        updateDari.put(Constants.FIELD_JADWAL_HARI, hariKepada);
        updateDari.put(Constants.FIELD_JADWAL_ID, jadwalIdKepada);

        Map<String, Object> updateKepada = new HashMap<>();
        updateKepada.put(Constants.FIELD_JADWAL_HARI, hariDari);
        updateKepada.put(Constants.FIELD_JADWAL_ID, jadwalIdDari);

        db.runTransaction(transaction -> {
            transaction.update(db.collection(Constants.COLLECTION_USERS).document(dariId), updateDari);
            transaction.update(db.collection(Constants.COLLECTION_USERS).document(kepadaId), updateKepada);
            transaction.update(db.collection(Constants.COLLECTION_TUKAR_JADWAL).document(docId),
                    Constants.FIELD_STATUS, Constants.STATUS_ACCEPTED);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Swap completed successfully");
            callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Swap failed: " + e.getMessage());
            callback.onError(e.getMessage());
        });
    }

    /**
     * Tolak permintaan tukar jadwal.
     */
    public void rejectSwap(String docId, SwapActionCallback callback) {
        db.collection(Constants.COLLECTION_TUKAR_JADWAL).document(docId)
                .update(Constants.FIELD_STATUS, Constants.STATUS_REJECTED)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Swap request rejected");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error rejecting: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }
}
