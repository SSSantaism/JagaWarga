package com.example.jagawarga;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class PrefUtils {

    // Ambil ID RT
    public static String getIdRt(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        // Pastikan key ini sesuai dengan yang disimpan di LoginActivity ("id_rt")
        String idRt = prefs.getString("id_rt", null);

        if (idRt == null) {
            Toast.makeText(context, "ID RT tidak ditemukan, silakan login ulang!", Toast.LENGTH_LONG).show();
        } else {
            Log.d("DEBUG_RT", "ID RT dari SharedPreferences = " + idRt);
        }

        return idRt;
    }

    // Ambil ID Warga
    public static String getIdWarga(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);

        // PERBAIKAN: Ubah "id_warga" menjadi "id" agar sesuai dengan LoginActivity
        String idWarga = prefs.getString("id", null);

        if (idWarga == null) {
            // Coba cari key lama "id_warga" buat jaga-jaga (backward compatibility)
            idWarga = prefs.getString("id_warga", null);
        }

        if (idWarga == null) {
            Toast.makeText(context, "ID Warga tidak ditemukan (Sesi Habis). Silakan Logout & Login ulang.",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d("DEBUG_WARGA", "ID Warga ditemukan: " + idWarga);
        }

        return idWarga;
    }

    // Ambil Jadwal ID
    public static String getJadwalId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String jadwalId = prefs.getString("jadwal_id", null);

        if (jadwalId == null) {
            Log.d("DEBUG_JADWAL", "Jadwal ID tidak ditemukan di session");
        } else {
            Log.d("DEBUG_JADWAL", "Jadwal ID ditemukan: " + jadwalId);
        }

        return jadwalId;
    }

    // Ambil Jadwal Hari
    public static String getJadwalHari(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        return prefs.getString("jadwal_hari", null);
    }

    // Simpan Jadwal Hari
    public static void setJadwalHari(Context context, String jadwalHari) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        prefs.edit().putString("jadwal_hari", jadwalHari).apply();
    }

    // Ambil Nama User
    public static String getNama(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        return prefs.getString("nama", null);
    }

    // Ambil Role
    public static String getRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        return prefs.getString("role", null);
    }
}