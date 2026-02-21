package com.example.jagawarga;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.text.InputType;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    private ViewGroup mainContainer;
    private LinearLayout layoutLogin, layoutRegister;
    private Button btnMasukTab, btnDaftarTab;

    // UI Login
    private EditText inputPhoneLogin, inputPasswordLogin;
    private Button btnLogin;

    // UI Register
    private EditText inputNamaReg, inputPhoneReg, inputPassReg;
    private Spinner inputRtReg;
    private Button btnRegisterAction;

    // Password Toggle
    private ImageView btnTogglePassLogin, btnTogglePassReg;
    private boolean isPasswordVisibleLogin = false;
    private boolean isPasswordVisibleReg = false;

    // Remember Me
    private CheckBox checkRemember;
    private boolean isAutoLogin = false; // Flag untuk menandai auto-login flow

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Domain palsu untuk trik login (User tidak perlu tahu ini)
    private static final String EMAIL_DOMAIN = "@jagawarga.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Cek jika user sudah login sebelumnya DAN "Ingat Saya" aktif
        SharedPreferences sessionPrefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
        boolean rememberMe = sessionPrefs.getBoolean(Constants.PREF_KEY_REMEMBER_ME, false); // Default false - user
                                                                                             // harus centang manual
        boolean hasSession = sessionPrefs.contains(Constants.PREF_KEY_ID); // Cek apakah ada session yang tersimpan

        if (mAuth.getCurrentUser() != null && hasSession && rememberMe) {
            // User sudah login dan centang "Ingat Saya" - langsung ke dashboard
            isAutoLogin = true; // Tandai sebagai auto-login
            checkUserRole(mAuth.getCurrentUser().getUid());
        } else if (mAuth.getCurrentUser() != null) {
            // User tidak centang "Ingat Saya" ATAU session tidak ada, sign out Firebase
            mAuth.signOut();
            sessionPrefs.edit().clear().apply();
        }

        initViews();
        setupSpinnerRt();
        setupTabs();
        setupActionButtons();
    }

    private void initViews() {
        mainContainer = findViewById(R.id.mainContainer);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);

        btnMasukTab = findViewById(R.id.btnMasukTab);
        btnDaftarTab = findViewById(R.id.btnDaftarTab);

        // Login Inputs
        inputPhoneLogin = findViewById(R.id.inputPhoneLogin);
        inputPasswordLogin = findViewById(R.id.inputPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        checkRemember = findViewById(R.id.checkRemember);
        checkRemember.setChecked(false); // Default unchecked - user harus centang untuk tetap login

        // Register Inputs
        inputNamaReg = findViewById(R.id.inputNamaReg);
        inputPhoneReg = findViewById(R.id.inputPhoneReg);
        inputRtReg = findViewById(R.id.inputRtReg);
        inputPassReg = findViewById(R.id.inputPassReg);
        btnRegisterAction = findViewById(R.id.btnRegisterAction);

        // Pastikan field password aktif (karena sebelumnya mungkin di-disable utk OTP)
        inputPasswordLogin.setEnabled(true);

        // Setup Password Toggle untuk Login
        btnTogglePassLogin = findViewById(R.id.btnTogglePassLogin);
        btnTogglePassLogin.setOnClickListener(v -> {
            isPasswordVisibleLogin = !isPasswordVisibleLogin;
            if (isPasswordVisibleLogin) {
                // Password visible
                inputPasswordLogin.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassLogin.setImageResource(R.drawable.icon_mata1);
            } else {
                // Password hidden
                inputPasswordLogin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassLogin.setImageResource(R.drawable.icon_mata2);
            }
            // Pindahkan cursor ke akhir text
            inputPasswordLogin.setSelection(inputPasswordLogin.getText().length());
        });

        // Setup Password Toggle untuk Register
        btnTogglePassReg = findViewById(R.id.btnTogglePassReg);
        btnTogglePassReg.setOnClickListener(v -> {
            isPasswordVisibleReg = !isPasswordVisibleReg;
            if (isPasswordVisibleReg) {
                // Password visible
                inputPassReg.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassReg.setImageResource(R.drawable.icon_mata1);
            } else {
                // Password hidden
                inputPassReg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassReg.setImageResource(R.drawable.icon_mata2);
            }
            // Pindahkan cursor ke akhir text
            inputPassReg.setSelection(inputPassReg.getText().length());
        });
    }

    private void setupSpinnerRt() {
        // Data dummy RT, sesuaikan dengan kebutuhan
        String[] rtOptions = { "01", "02", "03", "04" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rtOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputRtReg.setAdapter(adapter);
    }

    private void setupTabs() {
        btnMasukTab.setOnClickListener(v -> {
            if (layoutLogin.getVisibility() == View.VISIBLE)
                return;
            TransitionManager.beginDelayedTransition(mainContainer);
            layoutRegister.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
            updateTabStyle(true);
        });

        btnDaftarTab.setOnClickListener(v -> {
            if (layoutRegister.getVisibility() == View.VISIBLE)
                return;
            TransitionManager.beginDelayedTransition(mainContainer);
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.VISIBLE);
            updateTabStyle(false);
        });
    }

    private void updateTabStyle(boolean isLoginActive) {
        if (isLoginActive) {
            btnMasukTab.setBackgroundResource(R.drawable.rounded_button);
            btnMasukTab.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnMasukTab.setTextColor(Color.BLACK);

            btnDaftarTab.setBackgroundColor(Color.TRANSPARENT);
            btnDaftarTab.setTextColor(ContextCompat.getColor(this, R.color.gray));
        } else {
            btnDaftarTab.setBackgroundResource(R.drawable.rounded_button);
            btnDaftarTab.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnDaftarTab.setTextColor(Color.BLACK);

            btnMasukTab.setBackgroundColor(Color.TRANSPARENT);
            btnMasukTab.setTextColor(ContextCompat.getColor(this, R.color.gray));
        }
    }

    private void setupActionButtons() {
        // ============================================================
        // 1. LOGIC LOGIN (MASUK)
        // ============================================================
        btnLogin.setOnClickListener(v -> {
            String rawPhone = inputPhoneLogin.getText().toString().trim();
            String password = inputPasswordLogin.getText().toString().trim();

            if (rawPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_phone_password), Toast.LENGTH_SHORT).show();
                return;
            }

            // Format No HP dan ubah jadi Email Palsu
            String fakeEmail = createFakeEmail(rawPhone);

            showLoading(true);
            mAuth.signInWithEmailAndPassword(fakeEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Login Auth Sukses, sekarang cek data di Firestore
                            checkUserRole(mAuth.getCurrentUser().getUid());
                        } else {
                            showLoading(false);
                            String error = task.getException() != null ? task.getException().getMessage()
                                    : getString(R.string.toast_login_failed_default);
                            Toast.makeText(this, getString(R.string.toast_login_failed, error), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        });

        // ============================================================
        // 2. LOGIC REGISTER (DAFTAR)
        // ============================================================
        btnRegisterAction.setOnClickListener(v -> {
            String nama = inputNamaReg.getText().toString().trim();
            String rawPhone = inputPhoneReg.getText().toString().trim();
            String password = inputPassReg.getText().toString().trim();
            String rt = inputRtReg.getSelectedItem().toString();

            if (nama.isEmpty() || rawPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_all_data_required), Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, getString(R.string.toast_password_min_length), Toast.LENGTH_SHORT).show();
                return;
            }

            // Format No HP jadi format standar (+62...)
            String formattedPhone = formatPhoneNumber(rawPhone);
            // Ubah jadi Email Palsu untuk Firebase Auth
            String fakeEmail = createFakeEmail(rawPhone);

            showLoading(true);

            // Buat User di Firebase Authentication
            mAuth.createUserWithEmailAndPassword(fakeEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Auth berhasil dibuat, sekarang SIMPAN DATA DETAIL ke Firestore
                            String uid = mAuth.getCurrentUser().getUid();
                            saveUserDataToFirestore(uid, formattedPhone, nama, rt);
                        } else {
                            showLoading(false);
                            String error = task.getException() != null ? task.getException().getMessage()
                                    : getString(R.string.toast_register_failed_default);
                            Toast.makeText(this, getString(R.string.toast_register_failed, error), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        });
    }

    // --- HELPER METHODS ---

    /**
     * Mengubah input user (misal: 0812345) menjadi email (misal:
     * +62812345@jagawarga.app)
     * Ini trik agar bisa pakai Password tanpa OTP.
     */
    private String createFakeEmail(String rawPhone) {
        String formatted = formatPhoneNumber(rawPhone);
        return formatted + EMAIL_DOMAIN;
    }

    /**
     * Standarisasi nomor HP ke format +62
     */
    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }

    /**
     * Callback interface for balanced day assignment
     */
    private interface OnBalancedDayCallback {
        void onResult(String day);
    }

    /**
     * Get day with least users for a specific RT (balanced assignment)
     */
    private void getBalancedDayForRt(String rt, OnBalancedDayCallback callback) {
        String[] days = { "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
        int[] counts = new int[7];
        AtomicInteger completedQueries = new AtomicInteger(0);

        for (int i = 0; i < days.length; i++) {
            final int index = i;
            db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo(Constants.FIELD_ID_RT, rt)
                    .whereEqualTo(Constants.FIELD_JADWAL_HARI, days[index])
                    .get()
                    .addOnSuccessListener(snap -> {
                        counts[index] = snap.size();
                        if (completedQueries.incrementAndGet() == 7) {
                            // Find day with minimum count
                            int minIndex = 0;
                            for (int j = 1; j < 7; j++) {
                                if (counts[j] < counts[minIndex])
                                    minIndex = j;
                            }
                            callback.onResult(days[minIndex]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to random if query fails
                        if (completedQueries.incrementAndGet() == 7) {
                            callback.onResult(days[(int) (Math.random() * days.length)]);
                        }
                    });
        }
    }

    /**
     * Generate unique jadwal ID in format: JDW-{RT}-{6 random alphanumeric chars}
     * Example: JDW-01-A3F2K9
     */
    private String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }

    /**
     * Simpan data detail (Nama, RT, NoHP asli) ke Firestore setelah register Auth
     * berhasil
     * Menggunakan balanced assignment untuk jadwal hari
     */
    private void saveUserDataToFirestore(String uid, String phone, String nama, String rt) {
        // NOTE: jadwal_hari and jadwal_id are NOT assigned during registration
        // They will be assigned when the account is verified by RT

        Map<String, Object> user = new HashMap<>();
        user.put(Constants.FIELD_NAMA, nama);
        user.put(Constants.FIELD_TELEPON, phone);
        user.put(Constants.FIELD_ID_RT, rt);
        user.put(Constants.FIELD_ROLE, Constants.ROLE_WARGA);
        // jadwal_hari and jadwal_id intentionally NOT set - will be assigned on
        // verification
        user.put(Constants.FIELD_STATUS_WARGA, Constants.STATUS_PENDING);
        user.put(Constants.FIELD_CREATED_AT, com.google.firebase.Timestamp.now());

        db.collection(Constants.COLLECTION_USERS).document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);

                    // Logout dari sesi register (karena createUser otomatis login)
                    mAuth.signOut();

                    // Redirect ke halaman Pending Register
                    Intent intent = new Intent(LoginActivity.this, PendingRegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.toast_save_profile_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cek role user di Firestore saat Login
     */
    private void checkUserRole(String uid) {
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // User ditemukan di DB
                            String role = document.getString(Constants.FIELD_ROLE);
                            String nama = document.getString(Constants.FIELD_NAMA);
                            String idRt = document.getString(Constants.FIELD_ID_RT);
                            String jadwalHari = document.getString(Constants.FIELD_JADWAL_HARI);
                            String jadwalId = document.getString(Constants.FIELD_JADWAL_ID);
                            String statusWarga = document.getString(Constants.FIELD_STATUS_WARGA);

                            // === CEK STATUS AKUN ===
                            // Hanya role "Warga" yang perlu dicek status, RT dan RW langsung bisa masuk
                            if (Constants.ROLE_WARGA.equals(role)) {
                                if (statusWarga == null || Constants.STATUS_PENDING.equals(statusWarga)) {
                                    // Akun belum diverifikasi oleh RT
                                    showLoading(false);
                                    mAuth.signOut();
                                    Toast.makeText(this,
                                            getString(R.string.toast_account_pending),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                } else if (Constants.STATUS_REJECTED.equals(statusWarga)) {
                                    // Akun ditolak
                                    showLoading(false);
                                    mAuth.signOut();
                                    Toast.makeText(this,
                                            getString(R.string.toast_account_rejected),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                // statusWarga == "verified" -> lanjut ke dashboard
                            }
                            // === END CEK STATUS AKUN ===

                            // Cek apakah user lama tanpa jadwal_hari atau jadwal_id
                            if (jadwalHari == null || jadwalHari.isEmpty() || jadwalId == null || jadwalId.isEmpty()) {
                                // Auto-assign jadwal untuk user lama
                                getBalancedDayForRt(idRt, day -> {
                                    Map<String, Object> updates = new HashMap<>();
                                    if (jadwalHari == null || jadwalHari.isEmpty()) {
                                        updates.put(Constants.FIELD_JADWAL_HARI, day);
                                    }
                                    if (jadwalId == null || jadwalId.isEmpty()) {
                                        updates.put(Constants.FIELD_JADWAL_ID, generateJadwalId(idRt));
                                    }

                                    db.collection(Constants.COLLECTION_USERS).document(uid)
                                            .update(updates)
                                            .addOnSuccessListener(v -> {
                                                String newJadwalId = (String) updates.get(Constants.FIELD_JADWAL_ID);
                                                if (newJadwalId == null)
                                                    newJadwalId = jadwalId;
                                                String newJadwalHari = (String) updates
                                                        .get(Constants.FIELD_JADWAL_HARI);
                                                if (newJadwalHari == null)
                                                    newJadwalHari = jadwalHari;
                                                saveSession(uid, role, nama, idRt, newJadwalId, newJadwalHari);
                                                redirectDashboard(role, nama);
                                            })
                                            .addOnFailureListener(e -> {
                                                // Even if update fails, continue to dashboard
                                                saveSession(uid, role, nama, idRt, jadwalId, jadwalHari);
                                                redirectDashboard(role, nama);
                                            });
                                });
                            } else {
                                // User sudah punya jadwal, lanjut normal
                                saveSession(uid, role, nama, idRt, jadwalId, jadwalHari);
                                redirectDashboard(role, nama);
                            }
                        } else {
                            showLoading(false);
                            // Kasus langka: Auth ada tapi data Firestore hilang
                            Toast.makeText(this, getString(R.string.toast_profile_not_found), Toast.LENGTH_LONG)
                                    .show();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, getString(R.string.toast_fetch_user_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSession(String id, String role, String nama, String idRt, String jadwalId, String jadwalHari) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);

        // Get remember me state:
        // - Jika auto-login, SELALU preserve existing value (jangan pakai checkbox
        // karena di-reset ke false)
        // - Jika fresh login, gunakan nilai dari checkbox
        boolean rememberMe;
        if (isAutoLogin) {
            // Auto-login flow - preserve existing value, jangan ambil dari checkbox
            rememberMe = prefs.getBoolean(Constants.PREF_KEY_REMEMBER_ME, true);
        } else if (checkRemember != null) {
            // Fresh login - use checkbox value
            rememberMe = checkRemember.isChecked();
        } else {
            // Fallback - preserve existing value
            rememberMe = prefs.getBoolean(Constants.PREF_KEY_REMEMBER_ME, true);
        }

        prefs.edit()
                .putString(Constants.PREF_KEY_ID, id)
                .putString(Constants.PREF_KEY_ID_RT, idRt)
                .putString(Constants.PREF_KEY_NAMA, nama)
                .putString(Constants.PREF_KEY_ROLE, role)
                .putString(Constants.PREF_KEY_JADWAL_ID, jadwalId)
                .putString(Constants.PREF_KEY_JADWAL_HARI, jadwalHari)
                .putBoolean(Constants.PREF_KEY_REMEMBER_ME, rememberMe)
                .apply();
    }

    private void redirectDashboard(String role, String namaUser) {
        showLoading(false);

        // Setup notifikasi setelah login berhasil
        setupNotifications();

        Intent intent;
        if (role != null && role.equalsIgnoreCase(Constants.ROLE_KETUA_RT)) {
            intent = new Intent(this, DashboardRtActivity.class);
        } else if (role != null && role.equalsIgnoreCase(Constants.ROLE_KETUA_RW)) {
            intent = new Intent(this, DashboardRwActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
        intent.putExtra(Constants.EXTRA_NAMA_USER, namaUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Setup semua komponen notifikasi setelah login berhasil
     */
    private void setupNotifications() {
        // 1. Request notification permission (Android 13+)
        requestNotificationPermission();

        // 2. Buat notification channels
        NotificationHelper.createNotificationChannels(this);

        // 3. Subscribe ke FCM Topic berdasarkan RT
        String idRt = PrefUtils.getIdRt(this);
        if (idRt != null) {
            String topic = "rt_" + idRt;
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FCM", "Subscribed to topic: " + topic);
                        } else {
                            Log.e("FCM", "Failed to subscribe to topic: " + topic);
                        }
                    });
        }

        // 4. Schedule ronda reminder (hanya untuk Warga)
        String role = PrefUtils.getRole(this);
        String jadwalHari = PrefUtils.getJadwalHari(this);
        if (Constants.ROLE_WARGA.equals(role) && jadwalHari != null) {
            RondaReminderManager.scheduleWeeklyReminder(this, jadwalHari);
            Log.d("Ronda", "Scheduled reminder for: " + jadwalHari);
        }

        // 5. Start tukar jadwal listener (hanya untuk Warga)
        String userId = PrefUtils.getIdWarga(this);
        if (Constants.ROLE_WARGA.equals(role) && userId != null) {
            TukarJadwalListener.startListening(this, userId);
        }
    }

    /**
     * Request POST_NOTIFICATIONS permission untuk Android 13+
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Notification permission granted");
            } else {
                Log.d("Permission", "Notification permission denied");
            }
        }
    }

    // Helper loading sederhana pakai Toast (bisa diganti ProgressDialog)
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            Toast.makeText(this, getString(R.string.toast_loading), Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(false);
            btnRegisterAction.setEnabled(false);
        } else {
            btnLogin.setEnabled(true);
            btnRegisterAction.setEnabled(true);
        }
    }
}