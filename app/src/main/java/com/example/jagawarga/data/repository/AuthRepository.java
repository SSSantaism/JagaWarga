package com.example.jagawarga.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Repository untuk semua operasi Firebase Authentication.
 * Mengenkapsulasi login, register, logout, dan helper konversi nomor HP.
 */
public class AuthRepository {

    private static final String EMAIL_DOMAIN = "@jagawarga.app";
    private final FirebaseAuth auth;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
    }

    // ========================================================================
    // Auth Operations
    // ========================================================================

    /**
     * Login user dengan email (fake) dan password.
     */
    public void login(String email, String password,
            @NonNull OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    /**
     * Register user baru dengan email (fake) dan password.
     */
    public void register(String email, String password,
            @NonNull OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    /**
     * Logout user dari Firebase Auth.
     */
    public void logout() {
        auth.signOut();
    }

    /**
     * Get current Firebase user (null jika belum login).
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // ========================================================================
    // Phone/Email Helpers
    // ========================================================================

    /**
     * Mengubah input user (misal: 0812345) menjadi email (misal:
     * +62812345@jagawarga.app).
     * Trik agar bisa pakai Password Auth tanpa OTP.
     */
    public String createFakeEmail(String rawPhone) {
        String formatted = formatPhoneNumber(rawPhone);
        return formatted + EMAIL_DOMAIN;
    }

    /**
     * Standarisasi nomor HP ke format +62.
     */
    public String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }
}
