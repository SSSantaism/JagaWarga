package com.example.jagawarga;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

        // Cek jika user sudah login sebelumnya (Session check)
        if (mAuth.getCurrentUser() != null) {
            checkUserRole(mAuth.getCurrentUser().getUid());
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

        // Register Inputs
        inputNamaReg = findViewById(R.id.inputNamaReg);
        inputPhoneReg = findViewById(R.id.inputPhoneReg);
        inputRtReg = findViewById(R.id.inputRtReg);
        inputPassReg = findViewById(R.id.inputPassReg);
        btnRegisterAction = findViewById(R.id.btnRegisterAction);

        // Pastikan field password aktif (karena sebelumnya mungkin di-disable utk OTP)
        inputPasswordLogin.setEnabled(true);
        inputPasswordLogin.setHint("Password");
    }

    private void setupSpinnerRt() {
        // Data dummy RT, sesuaikan dengan kebutuhan
        String[] rtOptions = {"01", "02", "03", "04"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rtOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputRtReg.setAdapter(adapter);
    }


    private void setupTabs() {
        btnMasukTab.setOnClickListener(v -> {
            if (layoutLogin.getVisibility() == View.VISIBLE) return;
            TransitionManager.beginDelayedTransition(mainContainer);
            layoutRegister.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
            updateTabStyle(true);
        });

        btnDaftarTab.setOnClickListener(v -> {
            if (layoutRegister.getVisibility() == View.VISIBLE) return;
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
                Toast.makeText(this, "Isi Nomor HP dan Password!", Toast.LENGTH_SHORT).show();
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
                            String error = task.getException() != null ? task.getException().getMessage() : "Login Gagal";
                            Toast.makeText(this, "Gagal Masuk: " + error, Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
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
                            String error = task.getException() != null ? task.getException().getMessage() : "Register Gagal";
                            Toast.makeText(this, "Gagal Daftar: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    // --- HELPER METHODS ---

    /**
     * Mengubah input user (misal: 0812345) menjadi email (misal: +62812345@jagawarga.app)
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
     * Simpan data detail (Nama, RT, NoHP asli) ke Firestore setelah register Auth berhasil
     */
    private void saveUserDataToFirestore(String uid, String phone, String nama, String rt) {
        Map<String, Object> user = new HashMap<>();
        user.put("nama", nama);
        user.put("telepon", phone);
        user.put("id_rt", rt);
        user.put("role", "Warga");

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);

                    // === PERUBAHAN DI SINI ===

                    // 1. Tampilkan pesan sukses
                    Toast.makeText(this, "Pendaftaran Berhasil! Silakan Login.", Toast.LENGTH_LONG).show();

                    // 2. Logout dari sesi register (karena createUser otomatis login)
                    mAuth.signOut();

                    // 3. Pindah UI ke Tab Masuk secara otomatis
                    btnMasukTab.performClick();

                    // 4. (Opsional) Bantu user mengisi No HP di form login agar tidak ketik ulang
                    // Kembalikan format +62 ke 0 agar natural
                    String displayPhone = phone.startsWith("+62") ? "0" + phone.substring(3) : phone;
                    inputPhoneLogin.setText(displayPhone);
                    inputPasswordLogin.setText(""); // Kosongkan password biar user ketik sendiri
                    inputPasswordLogin.requestFocus(); // Arahkan kursor ke password

                    // 5. Bersihkan form register
                    inputNamaReg.setText("");
                    inputPhoneReg.setText("");
                    inputPassReg.setText("");

                    // === SELESAI PERUBAHAN ===
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Gagal simpan data profil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cek role user di Firestore saat Login
     */
    private void checkUserRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // User ditemukan di DB
                            String role = document.getString("role");
                            String nama = document.getString("nama");
                            String idRt = document.getString("id_rt");

                            // Simpan ke SharedPreferences
                            saveSession(uid, role, nama, idRt);

                            // Pindah ke Dashboard
                            redirectDashboard(role, nama);
                        } else {
                            showLoading(false);
                            // Kasus langka: Auth ada tapi data Firestore hilang
                            Toast.makeText(this, "Data profil tidak ditemukan. Hubungi Admin.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Gagal mengambil data user (Koneksi/Error)", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSession(String id, String role, String nama, String idRt) {
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        prefs.edit()
                .putString("id", id)
                .putString("id_rt", idRt)
                .putString("nama", nama)
                .putString("role", role)
                .apply();
    }

    private void redirectDashboard(String role, String namaUser) {
        showLoading(false);
        Intent intent;
        if (role != null && role.equalsIgnoreCase("KetuaRT")) {
            intent = new Intent(this, DashboardRtActivity.class);
        } else if (role != null && role.equalsIgnoreCase("KetuaRW")) {
            intent = new Intent(this, DashboardRwActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
        intent.putExtra("nama_user", namaUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Helper loading sederhana pakai Toast (bisa diganti ProgressDialog)
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            Toast.makeText(this, "Memproses...", Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(false);
            btnRegisterAction.setEnabled(false);
        } else {
            btnLogin.setEnabled(true);
            btnRegisterAction.setEnabled(true);
        }
    }
}