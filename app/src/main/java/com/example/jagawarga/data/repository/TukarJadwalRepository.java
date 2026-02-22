package com.example.jagawarga.data.repository;

import android.util.Log;

import com.example.jagawarga.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository untuk semua operasi Firestore terkait Tukar Jadwal.
 * Dipakai oleh TukarJadwalViewModel dan TukarJadwalActionReceiver.
 *
 * Method sendSwapRequest menggunakan Task Chaining (continueWithTask)
 * untuk menghindari callback hell.
 * Method getSwapDocument, performSwap, rejectSwap tetap flat (tidak perlu
 * chain).
 */
public class TukarJadwalRepository {

    private static final String TAG = "TukarJadwalRepo";
    private final FirebaseFirestore db;

    // ========================================================================
    // Custom Exceptions
    // ========================================================================

    /** Target user dengan jadwal_id tersebut tidak ditemukan. */
    public static class TargetNotFoundException extends Exception {
        public TargetNotFoundException() {
            super("User dengan ID jadwal tersebut tidak ditemukan.");
        }
    }

    /** Sudah ada permintaan tukar jadwal pending ke user ini. */
    public static class SwapAlreadyExistsException extends Exception {
        public SwapAlreadyExistsException() {
            super("Anda sudah mengirim permintaan ke user ini.");
        }
    }

    // ========================================================================
    // Callback Interfaces (hanya untuk method yang masih flat & dipakai Receiver)
    // ========================================================================

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

    /** Data internal hasil pencarian target user. */
    private static class FindResult {
        final String userId;
        final String nama;
        final String jadwalHari;

        FindResult(String userId, String nama, String jadwalHari) {
            this.userId = userId;
            this.nama = nama;
            this.jadwalHari = jadwalHari;
        }
    }

    public TukarJadwalRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Send Swap Request — Task Chain (flat, no nested callbacks)
    // ========================================================================

    /**
     * Cari target user → cek duplikat pending → kirim permintaan tukar jadwal.
     * Semua dalam satu rantai Task.
     *
     * @return Task<String> berisi nama target jika berhasil.
     *         Gagal dengan TargetNotFoundException atau SwapAlreadyExistsException.
     */
    public Task<String> sendSwapRequest(String myUserId, String myNama, String myJadwalId,
            String myJadwalHari, String myIdRt,
            String targetJadwalId) {

        // Step 1: Cari target user berdasarkan jadwal_id
        return findTargetUserTask(targetJadwalId, myIdRt)
                .continueWithTask(findTask -> {
                    FindResult target = findTask.getResult();

                    // Step 2: Cek duplikat pending request
                    return db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                            .whereEqualTo(Constants.FIELD_DARI_ID, myUserId)
                            .whereEqualTo(Constants.FIELD_KEPADA_ID, target.userId)
                            .whereEqualTo(Constants.FIELD_STATUS, Constants.STATUS_PENDING)
                            .get()
                            .continueWithTask(dupTask -> {
                                if (!dupTask.isSuccessful()) {
                                    throw dupTask.getException() != null
                                            ? dupTask.getException()
                                            : new Exception("Gagal cek duplikat");
                                }
                                if (!dupTask.getResult().isEmpty()) {
                                    throw new SwapAlreadyExistsException();
                                }

                                // Step 3: Buat swap request baru
                                Map<String, Object> swapRequest = new HashMap<>();
                                swapRequest.put(Constants.FIELD_DARI_ID, myUserId);
                                swapRequest.put(Constants.FIELD_DARI_NAMA, myNama);
                                swapRequest.put(Constants.FIELD_DARI_JADWAL_ID, myJadwalId);
                                swapRequest.put(Constants.FIELD_HARI_DARI, myJadwalHari);
                                swapRequest.put(Constants.FIELD_KEPADA_ID, target.userId);
                                swapRequest.put(Constants.FIELD_KEPADA_NAMA, target.nama);
                                swapRequest.put(Constants.FIELD_KEPADA_JADWAL_ID, targetJadwalId);
                                swapRequest.put(Constants.FIELD_HARI_KEPADA, target.jadwalHari);
                                swapRequest.put(Constants.FIELD_ID_RT, myIdRt);
                                swapRequest.put(Constants.FIELD_STATUS, Constants.STATUS_PENDING);
                                swapRequest.put(Constants.FIELD_CREATED_AT, Timestamp.now());

                                return db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                                        .add(swapRequest)
                                        .continueWith(addTask -> {
                                            if (!addTask.isSuccessful()) {
                                                throw addTask.getException() != null
                                                        ? addTask.getException()
                                                        : new Exception("Gagal mengirim permintaan");
                                            }
                                            Log.d(TAG, "Swap request created: "
                                                    + addTask.getResult().getId());
                                            return target.nama;
                                        });
                            });
                });
    }

    // ========================================================================
    // Accept / Reject Swap (dipakai oleh TukarJadwalActionReceiver — tetap flat)
    // ========================================================================

    /** Ambil data swap request dari Firestore. */
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
            transaction.update(db.collection(Constants.COLLECTION_USERS).document(dariId),
                    updateDari);
            transaction.update(db.collection(Constants.COLLECTION_USERS).document(kepadaId),
                    updateKepada);
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

    /** Tolak permintaan tukar jadwal. */
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

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Cari user dengan jadwal_id tertentu dalam RT yang sama.
     * 
     * @return Task<FindResult> — gagal dengan TargetNotFoundException jika tidak
     *         ada.
     */
    private Task<FindResult> findTargetUserTask(String targetJadwalId, String idRt) {
        return db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_JADWAL_ID, targetJadwalId)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Gagal mencari target");
                    }
                    QuerySnapshot snap = task.getResult();
                    if (snap.isEmpty()) {
                        throw new TargetNotFoundException();
                    }

                    // Ambil dokumen pertama saja
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    return new FindResult(
                            doc.getId(),
                            doc.getString(Constants.FIELD_NAMA),
                            doc.getString(Constants.FIELD_JADWAL_HARI));
                });
    }
}
