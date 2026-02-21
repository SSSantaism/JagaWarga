package com.example.jagawarga;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.jagawarga.utils.Constants;

public class PrefUtils {

    // Ambil ID RT
    public static String getIdRt(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
        String idRt = prefs.getString(Constants.PREF_KEY_ID_RT, null);

        if (idRt == null) {
            Toast.makeText(context, context.getString(R.string.toast_id_rt_not_found), Toast.LENGTH_LONG).show();
        } else {
            Log.d("DEBUG_RT", "ID RT dari SharedPreferences = " + idRt);
        }

        return idRt;
    }

    // Ambil ID Warga
    public static String getIdWarga(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);

        String idWarga = prefs.getString(Constants.PREF_KEY_ID, null);

        if (idWarga == null) {
            // Coba cari key lama "id_warga" buat jaga-jaga (backward compatibility)
            idWarga = prefs.getString(Constants.PREF_KEY_ID_WARGA_LEGACY, null);
        }

        if (idWarga == null) {
            Toast.makeText(context, context.getString(R.string.toast_id_warga_not_found),
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d("DEBUG_WARGA", "ID Warga ditemukan: " + idWarga);
        }

        return idWarga;
    }

    // Ambil Jadwal ID
    public static String getJadwalId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
        String jadwalId = prefs.getString(Constants.PREF_KEY_JADWAL_ID, null);

        if (jadwalId == null) {
            Log.d("DEBUG_JADWAL", "Jadwal ID tidak ditemukan di session");
        } else {
            Log.d("DEBUG_JADWAL", "Jadwal ID ditemukan: " + jadwalId);
        }

        return jadwalId;
    }

    // Ambil Jadwal Hari
    public static String getJadwalHari(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_KEY_JADWAL_HARI, null);
    }

    // Simpan Jadwal Hari
    public static void setJadwalHari(Context context, String jadwalHari) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREF_KEY_JADWAL_HARI, jadwalHari).apply();
    }

    // Ambil Nama User
    public static String getNama(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_KEY_NAMA, null);
    }

    // Ambil Role
    public static String getRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_USER_DATA, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_KEY_ROLE, null);
    }
}