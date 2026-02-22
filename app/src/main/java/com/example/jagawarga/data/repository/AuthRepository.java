package com.example.jagawarga.data.repository;

import com.example.jagawarga.data.model.User;
import com.example.jagawarga.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository untuk semua operasi Firebase Authentication dan login-related
 * Firestore.
 * Mengenkapsulasi login, register, role check, user profile save, dan jadwal
 * assignment.
 *
 * Semua method async mengembalikan Task<T> — tanpa callback interfaces.
 * Branching logic (pending/rejected/notFound) menggunakan custom Exception.
 */
public class AuthRepository {

    private static final String EMAIL_DOMAIN = "@jagawarga.app";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    // ========================================================================
    // Custom Exceptions (untuk flow yang bukan error sesungguhnya)
    // ========================================================================

    /** Akun masih menunggu persetujuan RT. */
    public static class UserPendingException extends Exception {
        public UserPendingException() {
            super("Akun masih menunggu persetujuan RT.");
        }
    }

    /** Akun ditolak oleh RT. */
    public static class UserRejectedException extends Exception {
        public UserRejectedException() {
            super("Akun ditolak oleh RT.");
        }
    }

    /** User tidak ditemukan di Firestore. */
    public static class UserNotFoundException extends Exception {
        public UserNotFoundException() {
            super("Data pengguna tidak ditemukan.");
        }
    }

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Auth Operations
    // ========================================================================

    /**
     * Login user dengan phone (dikonversi ke fake email) dan password.
     * 
     * @return Task<String> berisi UID.
     */
    public Task<String> login(String rawPhone, String password) {
        String fakeEmail = createFakeEmail(rawPhone);
        return auth.signInWithEmailAndPassword(fakeEmail, password)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Login gagal");
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        throw new Exception("Login gagal: user null");
                    }
                    return user.getUid();
                });
    }

    /**
     * Register user baru dengan phone (dikonversi ke fake email) dan password.
     * 
     * @return Task<String> berisi UID.
     */
    public Task<String> register(String rawPhone, String password) {
        String fakeEmail = createFakeEmail(rawPhone);
        return auth.createUserWithEmailAndPassword(fakeEmail, password)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Registrasi gagal");
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        throw new Exception("Registrasi gagal: user null");
                    }
                    return user.getUid();
                });
    }

    /** Logout user dari Firebase Auth. */
    public void logout() {
        auth.signOut();
    }

    /** Get current Firebase user (null jika belum login). */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // ========================================================================
    // Firestore: Check User Role & Status (Task Chain)
    // ========================================================================

    /**
     * Cek role & status user di Firestore saat login.
     * Juga auto-assign jadwal untuk user lama yang belum punya jadwal.
     *
     * @return Task<User> — berhasil jika user verified,
     *         gagal dengan UserPendingException / UserRejectedException /
     *         UserNotFoundException jika status tidak valid.
     */
    public Task<User> checkUserRole(String uid) {
        // Step 1: Ambil dokumen user
        return db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw new Exception("Gagal mengambil data pengguna");
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (!doc.exists()) {
                        throw new UserNotFoundException();
                    }

                    String role = doc.getString(Constants.FIELD_ROLE);
                    String nama = doc.getString(Constants.FIELD_NAMA);
                    String idRt = doc.getString(Constants.FIELD_ID_RT);
                    String jadwalHari = doc.getString(Constants.FIELD_JADWAL_HARI);
                    String jadwalId = doc.getString(Constants.FIELD_JADWAL_ID);
                    String statusWarga = doc.getString(Constants.FIELD_STATUS_WARGA);

                    // === CEK STATUS AKUN ===
                    if (Constants.ROLE_WARGA.equals(role)) {
                        if (statusWarga == null || Constants.STATUS_PENDING.equals(statusWarga)) {
                            auth.signOut();
                            throw new UserPendingException();
                        } else if (Constants.STATUS_REJECTED.equals(statusWarga)) {
                            auth.signOut();
                            throw new UserRejectedException();
                        }
                    }

                    // Step 2: Cek apakah perlu auto-assign jadwal
                    boolean needsJadwalHari = (jadwalHari == null || jadwalHari.isEmpty());
                    boolean needsJadwalId = (jadwalId == null || jadwalId.isEmpty());

                    if (needsJadwalHari || needsJadwalId) {
                        // Chain ke auto-assign jadwal
                        return autoAssignJadwalTask(uid, idRt, jadwalHari, jadwalId,
                                needsJadwalHari, needsJadwalId)
                                .continueWith(assignTask -> {
                                    // assignTask.getResult() = String[] {finalId, finalHari}
                                    String[] result = assignTask.getResult();
                                    return new User(uid, nama, null, idRt, role,
                                            statusWarga, result[1], result[0]);
                                });
                    } else {
                        // Sudah lengkap, langsung buat User
                        return Tasks.forResult(
                                new User(uid, nama, null, idRt, role,
                                        statusWarga, jadwalHari, jadwalId));
                    }
                });
    }

    // ========================================================================
    // Firestore: Save New User (Registration) — Task Chain
    // ========================================================================

    /**
     * Simpan data user baru ke Firestore setelah register Auth berhasil.
     * Status = pending, role = Warga, tanpa jadwal.
     * Otomatis logout setelah save (createUser auto-login).
     *
     * @return Task<Void>
     */
    public Task<Void> saveNewUser(String uid, String phone, String nama, String rt) {
        String formattedPhone = formatPhoneNumber(phone);

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_NAMA, nama);
        data.put(Constants.FIELD_TELEPON, formattedPhone);
        data.put(Constants.FIELD_ID_RT, rt);
        data.put(Constants.FIELD_ROLE, Constants.ROLE_WARGA);
        data.put(Constants.FIELD_STATUS_WARGA, Constants.STATUS_PENDING);
        data.put(Constants.FIELD_CREATED_AT, Timestamp.now());

        return db.collection(Constants.COLLECTION_USERS).document(uid)
                .set(data)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Gagal menyimpan data user");
                    }
                    // Logout dari sesi register (createUser otomatis login)
                    auth.signOut();
                    return null;
                });
    }

    // ========================================================================
    // Phone/Email Helpers
    // ========================================================================

    public String createFakeEmail(String rawPhone) {
        String formatted = formatPhoneNumber(rawPhone);
        return formatted + EMAIL_DOMAIN;
    }

    public String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }

    // ========================================================================
    // Private Helpers — Task-based
    // ========================================================================

    /**
     * Auto-assign jadwal untuk user lama yang belum punya jadwal.
     * 
     * @return Task<String[]> dimana [0] = jadwalId, [1] = jadwalHari.
     */
    private Task<String[]> autoAssignJadwalTask(String uid, String idRt,
            String existingJadwalHari, String existingJadwalId,
            boolean needsHari, boolean needsId) {

        // Step 1: Dapatkan hari yang paling seimbang
        return getBalancedDayForRtTask(idRt)
                .continueWithTask(dayTask -> {
                    String day = dayTask.getResult();

                    Map<String, Object> updates = new HashMap<>();
                    String finalHari = existingJadwalHari;
                    String finalId = existingJadwalId;

                    if (needsHari) {
                        updates.put(Constants.FIELD_JADWAL_HARI, day);
                        finalHari = day;
                    }
                    if (needsId) {
                        String newId = generateJadwalId(idRt);
                        updates.put(Constants.FIELD_JADWAL_ID, newId);
                        finalId = newId;
                    }

                    String resultId = finalId;
                    String resultHari = finalHari;

                    // Step 2: Update dokumen user
                    return db.collection(Constants.COLLECTION_USERS).document(uid)
                            .update(updates)
                            .continueWith(updateTask -> {
                                // Meski gagal update, tetap lanjut dengan data yang ada
                                return new String[] { resultId, resultHari };
                            });
                });
    }

    /**
     * Hitung hari dengan jumlah warga terkecil di RT tertentu.
     * Menggunakan Tasks.whenAllComplete() untuk parallel queries (7 hari).
     *
     * @return Task<String> berisi nama hari (Senin, Selasa, dst).
     */
    private Task<String> getBalancedDayForRtTask(String rt) {
        String[] days = { "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };

        // Buat 7 query parallel
        List<Task<QuerySnapshot>> queryTasks = new ArrayList<>();
        for (String day : days) {
            queryTasks.add(
                    db.collection(Constants.COLLECTION_USERS)
                            .whereEqualTo(Constants.FIELD_ID_RT, rt)
                            .whereEqualTo(Constants.FIELD_JADWAL_HARI, day)
                            .get());
        }

        // Tunggu semua selesai, lalu pilih hari minimum
        return Tasks.whenAllComplete(queryTasks)
                .continueWith(allTask -> {
                    int[] counts = new int[7];
                    for (int i = 0; i < 7; i++) {
                        Task<QuerySnapshot> t = queryTasks.get(i);
                        if (t.isSuccessful() && t.getResult() != null) {
                            counts[i] = t.getResult().size();
                        } else {
                            counts[i] = Integer.MAX_VALUE; // Error → skip hari ini
                        }
                    }

                    int minIdx = 0;
                    for (int j = 1; j < 7; j++) {
                        if (counts[j] < counts[minIdx]) {
                            minIdx = j;
                        }
                    }

                    // Fallback jika semua error
                    if (counts[minIdx] == Integer.MAX_VALUE) {
                        return days[(int) (Math.random() * days.length)];
                    }
                    return days[minIdx];
                });
    }

    private String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }
}
