package com.example.jagawarga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// Import Firebase
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private ViewGroup mainContainer;
    private LinearLayout layoutLogin, layoutRegister;
    private Button btnMasukTab, btnDaftarTab;
    private EditText inputPhoneLogin, inputPasswordLogin; // Password tidak dipakai di Auth OTP
    private ImageView btnTogglePassLogin;
    private Button btnLogin;
    private TextView textForgot;
    private EditText inputNamaReg, inputPhoneReg, inputPassReg;
    private Spinner inputRtReg;
    private ImageView btnTogglePassReg;
    private Button btnRegisterAction;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String mVerificationId;
    private String pendingNama = "";
    private String pendingRt = "";
    private String pendingPhone = "";
    private boolean isRegisterFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Cek jika user sudah login sebelumnya
        if (mAuth.getCurrentUser() != null) {
            checkUserRole(mAuth.getCurrentUser().getUid());
        }

        initViews();
        setupUI(); // Sembunyikan kolom password login karena pakai OTP
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
        inputPhoneLogin = findViewById(R.id.inputPhoneLogin);
        inputPasswordLogin = findViewById(R.id.inputPasswordLogin);
        btnTogglePassLogin = findViewById(R.id.btnTogglePassLogin);
        btnLogin = findViewById(R.id.btnLogin);
        textForgot = findViewById(R.id.textForgot);
        inputNamaReg = findViewById(R.id.inputNamaReg);
        inputPhoneReg = findViewById(R.id.inputPhoneReg);
        inputRtReg = findViewById(R.id.inputRtReg);
        inputPassReg = findViewById(R.id.inputPassReg);
        btnTogglePassReg = findViewById(R.id.btnTogglePassReg);
        btnRegisterAction = findViewById(R.id.btnRegisterAction);
    }

    private void setupUI() {
        // Karena login pakai OTP, kita sembunyikan kolom password di Tab Login
        // inputPasswordLogin.setVisibility(View.GONE);
        // btnTogglePassLogin.setVisibility(View.GONE);
        // textForgot.setVisibility(View.GONE);

        // ATAU biarkan saja tapi abaikan isinya.
        // Agar user tidak bingung, sebaiknya di layout XML nanti diubah.
        // Untuk sekarang via kodingan kita "disable" visualnya:
        inputPasswordLogin.setHint("Password tidak diperlukan (OTP)");
        inputPasswordLogin.setEnabled(false);
    }

    private void setupSpinnerRt() {
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
        // --- JALUR LOGIN (Hanya untuk user lama) ---
        btnLogin.setOnClickListener(v -> {
            String rawPhone = inputPhoneLogin.getText().toString().trim();
            if (rawPhone.isEmpty()) {
                inputPhoneLogin.setError("Masukkan nomor telepon");
                return;
            }
            String formattedPhone = formatPhoneNumber(rawPhone);

            // Cek dulu: Apakah nomor ini ada di database?
            checkUserExists(formattedPhone, true);
        });

        // --- JALUR REGISTER (Hanya untuk user baru) ---
        btnRegisterAction.setOnClickListener(v -> {
            String nama = inputNamaReg.getText().toString().trim();
            String rawPhone = inputPhoneReg.getText().toString().trim();
            String rt = inputRtReg.getSelectedItem().toString(); // Ambil dari Spinner

            if (nama.isEmpty() || rawPhone.isEmpty()) {
                Toast.makeText(this, "Nama dan No HP harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            String formattedPhone = formatPhoneNumber(rawPhone);

            // Simpan data di variabel sementara
            pendingNama = nama;
            pendingRt = rt;
            pendingPhone = formattedPhone;

            // Cek dulu: Apakah nomor ini SUDAH ada? (Kalau sudah, jangan daftar lagi)
            checkUserExists(formattedPhone, false);
        });


        // --- LOGIC REGISTER (DAFTAR) ---
        // Saat ini fokus ke Login dulu, register nanti disesuaikan
        btnRegisterAction.setOnClickListener(v -> {
            Toast.makeText(this, "Silakan gunakan menu Masuk untuk Login OTP", Toast.LENGTH_SHORT).show();
        });
    }

    // isLoginAction = true (Tombol Masuk), false (Tombol Daftar)
    private void checkUserExists(String phone, boolean isLoginAction) {
        // Tampilkan loading (opsional, pakai Toast dulu biar simpel)
        Toast.makeText(this, "Mengecek data...", Toast.LENGTH_SHORT).show();

        db.collection("users")
                .whereEqualTo("telepon", phone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = !task.getResult().isEmpty();

                        if (isLoginAction) {
                            // --- KASUS LOGIN ---
                            if (exists) {
                                // Benar, user ada. Lanjut OTP.
                                isRegisterFlow = false; // Set mode ke Login
                                startPhoneNumberVerification(phone);
                            } else {
                                // Salah, user belum terdaftar.
                                Toast.makeText(this, "Nomor belum terdaftar! Silakan ke menu Daftar.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // --- KASUS REGISTER ---
                            if (exists) {
                                // Salah, user sudah ada. Jangan daftar lagi.
                                Toast.makeText(this, "Nomor sudah terdaftar! Silakan Login.", Toast.LENGTH_LONG).show();
                            } else {
                                // Benar, user baru. Lanjut OTP.
                                isRegisterFlow = true; // Set mode ke Register
                                startPhoneNumberVerification(phone);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Gagal koneksi database.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // Ubah 08xxx jadi +628xxx
    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }

    // =================================================================
    // 1. KIRIM KODE OTP
    // =================================================================
    private void startPhoneNumberVerification(String phoneNumber) {
        Toast.makeText(this, "Mengirim OTP ke " + phoneNumber, Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(false); // Cegah klik ganda

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // Callback status pengiriman OTP
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Terjadi jika verifikasi otomatis berhasil (jarang di beberapa device)
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Verifikasi Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("AUTH_FAIL", "Error", e);
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    // Kode terkirim! Simpan ID verifikasi
                    mVerificationId = verificationId;
                    btnLogin.setEnabled(true);

                    // Tampilkan Dialog Input OTP
                    showOtpDialog();
                }
            };

    // =================================================================
    // 2. DIALOG INPUT OTP
    // =================================================================
    private void showOtpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verifikasi OTP");
        builder.setMessage("Masukkan 6 digit kode yang dikirim via SMS");

        // Input field di dalam dialog
        final EditText inputCode = new EditText(this);
        inputCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCode.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setView(inputCode);

        builder.setPositiveButton("Verifikasi", (dialog, which) -> {
            String code = inputCode.getText().toString().trim();
            if (!code.isEmpty()) {
                verifyPhoneNumberWithCode(mVerificationId, code);
            }
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    // =================================================================
    // 3. LOGIN KE FIREBASE
    // =================================================================
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {

                            // DISINI PERCABANGANNYA
                            if (isRegisterFlow) {
                                // Jika ini proses REGISTER, simpan data yang tadi diinput
                                saveNewUserToFirestore(user.getUid());
                            } else {
                                // Jika ini proses LOGIN, langsung ambil data & masuk
                                checkUserRole(user.getUid());
                            }

                        }
                    } else {
                        if (task.getException() != null) {
                            Toast.makeText(LoginActivity.this, "Kode OTP Salah.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // =================================================================
    // 4. CEK DATA USER DI FIRESTORE (PENGGANTI PHP LOGIN)
    // =================================================================
    private void checkUserRole(String uid) {
        // Ambil data dari koleksi 'users' berdasarkan UID
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // User terdaftar, ambil datanya
                            String role = document.getString("role");
                            String nama = document.getString("nama");
                            String idRt = document.getString("id_rt");

                            // Simpan ke SharedPreferences (agar logika activity lain tetap jalan)
                            saveSession(uid, role, nama, idRt);

                            // Redirect
                            redirectDashboard(role, nama);
                        } else {
                            // HAPUS Toast lama, GANTI jadi ini:
                            showRegisterDialog(uid, mAuth.getCurrentUser().getPhoneNumber());
                        }
                    } else {
                        Toast.makeText(this, "Gagal mengambil data user", Toast.LENGTH_SHORT).show();
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
    private void showRegisterDialog(String uid, String phoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lengkapi Data Diri");
        builder.setCancelable(false); // Gak bisa ditutup paksa

        // Bikin Layout Dialog secara program (biar gak ribet bikin XML baru)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputNama = new EditText(this);
        inputNama.setHint("Nama Lengkap");
        layout.addView(inputNama);

        final EditText inputRT = new EditText(this);
        inputRT.setHint("Nomor RT (Contoh: 01)");
        inputRT.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputRT);

        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String nama = inputNama.getText().toString();
            String rt = inputRT.getText().toString();

            if (nama.isEmpty() || rt.isEmpty()) {
                Toast.makeText(this, "Nama dan RT harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            saveUserData(uid, phoneNumber, nama, rt);
        });

        builder.show();
    }

    private void saveNewUserToFirestore(String uid) {
        java.util.Map<String, Object> user = new java.util.HashMap<>();
        user.put("nama", pendingNama);      // Dari inputan Tab Daftar
        user.put("id_rt", pendingRt);       // Dari inputan Tab Daftar
        user.put("telepon", pendingPhone);
        user.put("role", "Warga");          // Default

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show();
                    // Simpan sesi & Redirect
                    saveSession(uid, "Warga", pendingNama, pendingRt);
                    redirectDashboard("Warga", pendingNama);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menyimpan data.", Toast.LENGTH_SHORT).show();
                });
    }
    private void saveUserData(String uid, String phone, String nama, String rt) {
        // Siapkan data untuk disimpan
        java.util.Map<String, Object> user = new java.util.HashMap<>();
        user.put("nama", nama);
        user.put("telepon", phone);
        user.put("id_rt", rt);
        user.put("role", "Warga"); // Default role

        // Simpan ke Firestore
        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Data tersimpan!", Toast.LENGTH_SHORT).show();
                    // Simpan sesi lokal
                    saveSession(uid, "Warga", nama, rt);
                    // Masuk ke Dashboard
                    redirectDashboard("Warga", nama);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menyimpan data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}