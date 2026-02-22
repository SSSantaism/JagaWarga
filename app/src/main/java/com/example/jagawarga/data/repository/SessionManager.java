package com.example.jagawarga.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.jagawarga.data.model.User;
import com.example.jagawarga.utils.Constants;

/**
 * Manager untuk sesi user via SharedPreferences.
 * Menggantikan PrefUtils dengan API yang lebih lengkap dan terstruktur.
 */
public class SessionManager {

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
    }

    // ========================================================================
    // Save / Clear Session
    // ========================================================================

    /**
     * Simpan data user ke sesi (SharedPreferences).
     */
    public void saveSession(User user) {
        prefs.edit()
                .putString(Constants.PREF_KEY_ID, user.getId())
                .putString(Constants.PREF_KEY_ID_RT, user.getIdRt())
                .putString(Constants.PREF_KEY_NAMA, user.getNama())
                .putString(Constants.PREF_KEY_ROLE, user.getRole())
                .putString(Constants.PREF_KEY_JADWAL_ID, user.getJadwalId())
                .putString(Constants.PREF_KEY_JADWAL_HARI, user.getJadwalHari())
                .apply();
    }

    /**
     * Simpan data user ke sesi dari parameter individual.
     */
    public void saveSession(String id, String role, String nama, String idRt,
            String jadwalId, String jadwalHari) {
        prefs.edit()
                .putString(Constants.PREF_KEY_ID, id)
                .putString(Constants.PREF_KEY_ID_RT, idRt)
                .putString(Constants.PREF_KEY_NAMA, nama)
                .putString(Constants.PREF_KEY_ROLE, role)
                .putString(Constants.PREF_KEY_JADWAL_ID, jadwalId)
                .putString(Constants.PREF_KEY_JADWAL_HARI, jadwalHari)
                .apply();
    }

    /**
     * Hapus semua data sesi.
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }

    // ========================================================================
    // Read User Data
    // ========================================================================

    /**
     * Ambil user saat ini dari sesi sebagai object User.
     * Mengembalikan null jika tidak ada sesi tersimpan.
     */
    public User getUser() {
        String id = prefs.getString(Constants.PREF_KEY_ID, null);
        if (id == null) {
            // Coba key lama untuk backward compatibility
            id = prefs.getString(Constants.PREF_KEY_ID_WARGA_LEGACY, null);
        }
        if (id == null)
            return null;

        return new User(
                id,
                prefs.getString(Constants.PREF_KEY_NAMA, null),
                null, // telepon tidak disimpan di session
                prefs.getString(Constants.PREF_KEY_ID_RT, null),
                prefs.getString(Constants.PREF_KEY_ROLE, null),
                null, // statusWarga tidak disimpan di session
                prefs.getString(Constants.PREF_KEY_JADWAL_HARI, null),
                prefs.getString(Constants.PREF_KEY_JADWAL_ID, null));
    }

    // Convenience getters (backward compatible dengan PrefUtils)
    public String getIdWarga() {
        String id = prefs.getString(Constants.PREF_KEY_ID, null);
        if (id == null) {
            id = prefs.getString(Constants.PREF_KEY_ID_WARGA_LEGACY, null);
        }
        return id;
    }

    public String getIdRt() {
        return prefs.getString(Constants.PREF_KEY_ID_RT, null);
    }

    public String getNama() {
        return prefs.getString(Constants.PREF_KEY_NAMA, null);
    }

    public String getRole() {
        return prefs.getString(Constants.PREF_KEY_ROLE, null);
    }

    public String getJadwalId() {
        return prefs.getString(Constants.PREF_KEY_JADWAL_ID, null);
    }

    public String getJadwalHari() {
        return prefs.getString(Constants.PREF_KEY_JADWAL_HARI, null);
    }

    public void setJadwalHari(String jadwalHari) {
        prefs.edit().putString(Constants.PREF_KEY_JADWAL_HARI, jadwalHari).apply();
    }

    // ========================================================================
    // Remember Me
    // ========================================================================

    public boolean isRememberMe() {
        return prefs.getBoolean(Constants.PREF_KEY_REMEMBER_ME, true);
    }

    public void setRememberMe(boolean rememberMe) {
        prefs.edit().putBoolean(Constants.PREF_KEY_REMEMBER_ME, rememberMe).apply();
    }
}
