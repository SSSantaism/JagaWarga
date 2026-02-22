package com.example.jagawarga.utils;

/**
 * Kumpulan konstanta yang digunakan di seluruh aplikasi JagaWarga.
 * Menghindari hardcoded string dan magic words.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // ========================================================================
    // SharedPreferences
    // ========================================================================
    public static final String PREF_USER_DATA = "user_data";

    // SharedPreferences Keys
    public static final String PREF_KEY_ID = "id";
    public static final String PREF_KEY_ID_RT = "id_rt";
    public static final String PREF_KEY_NAMA = "nama";
    public static final String PREF_KEY_ROLE = "role";
    public static final String PREF_KEY_JADWAL_ID = "jadwal_id";
    public static final String PREF_KEY_JADWAL_HARI = "jadwal_hari";
    public static final String PREF_KEY_REMEMBER_ME = "remember_me";
    public static final String PREF_KEY_ID_WARGA_LEGACY = "id_warga"; // backward compatibility

    // ========================================================================
    // Firestore Collections
    // ========================================================================
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PENGUMUMAN = "pengumuman";
    public static final String COLLECTION_ABSENSI = "absensi";
    public static final String COLLECTION_DATA_RT = "data_rt";
    public static final String COLLECTION_LAPORAN = "laporan";
    public static final String COLLECTION_TUKAR_JADWAL = "tukar_jadwal";

    // ========================================================================
    // Firestore Fields
    // ========================================================================
    public static final String FIELD_NAMA = "nama";
    public static final String FIELD_TELEPON = "telepon";
    public static final String FIELD_ID_RT = "id_rt";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_STATUS_WARGA = "status_warga";
    public static final String FIELD_JADWAL_HARI = "jadwal_hari";
    public static final String FIELD_JADWAL_ID = "jadwal_id";
    public static final String FIELD_TANGGAL = "tanggal";
    public static final String FIELD_JUDUL = "judul";
    public static final String FIELD_ISI = "isi";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_ID_WARGA = "id_warga";
    public static final String FIELD_NAMA_PELAPOR = "nama_pelapor";
    public static final String FIELD_ISI_LAPORAN = "isi_laporan";
    public static final String FIELD_JENIS_LAPORAN = "jenis_laporan";
    public static final String FIELD_WAKTU = "waktu";
    public static final String FIELD_CATATAN = "catatan";
    public static final String FIELD_POS_PHONE = "pos_phone";
    public static final String FIELD_LOKASI = "lokasi";
    public static final String FIELD_CREATED_AT = "createdAt";

    // Tukar Jadwal Fields
    public static final String FIELD_DARI_ID = "dari_id";
    public static final String FIELD_DARI_NAMA = "dari_nama";
    public static final String FIELD_DARI_JADWAL_ID = "dari_jadwal_id";
    public static final String FIELD_HARI_DARI = "hari_dari";
    public static final String FIELD_KEPADA_ID = "kepada_id";
    public static final String FIELD_KEPADA_NAMA = "kepada_nama";
    public static final String FIELD_KEPADA_JADWAL_ID = "kepada_jadwal_id";
    public static final String FIELD_HARI_KEPADA = "hari_kepada";

    // ========================================================================
    // Intent Extras
    // ========================================================================
    public static final String EXTRA_NAMA_USER = "nama_user";
    public static final String EXTRA_ID_RT = "id_rt";
    public static final String EXTRA_ID_WARGA = "id_warga";
    public static final String EXTRA_NAMA = "nama";
    public static final String EXTRA_ID_RW = "id_rw";
    public static final String EXTRA_NAMA_RW = "nama_rw";
    public static final String EXTRA_ID_RT_RW = "id_rt_rw";

    // ========================================================================
    // Role Values
    // ========================================================================
    public static final String ROLE_WARGA = "Warga";
    public static final String ROLE_KETUA_RT = "KetuaRT";
    public static final String ROLE_KETUA_RW = "KetuaRW";

    // ========================================================================
    // Status Values
    // ========================================================================
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_ACCEPTED = "accepted";

    // ========================================================================
    // FCM
    // ========================================================================
    public static final String FCM_DATA_TYPE = "type";
    public static final String FCM_TYPE_PENGUMUMAN = "pengumuman";
    public static final String FCM_DATA_JUDUL = "judul";
    public static final String FCM_DATA_ISI = "isi";
}
