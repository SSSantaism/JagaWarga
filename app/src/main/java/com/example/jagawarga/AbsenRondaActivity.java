package com.example.jagawarga;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AbsenRondaActivity extends AppCompatActivity {

    private ImageButton btnBackAbsen;
    private Button btnKirimAbsen;
    // EditText insert_absenID; // No longer needed if we check day automatically, but let's keep it as "Code" or remove logic?
    // Plan said: "Check: Is today == User's jadwal_hari?"
    // If we rely on automatic check, we don't need ID Jadwal input.
    // However, the UI might still have it. I should probably ignore it or use it as "Notes".
    // Let's hide or ignore the ID input for now and focus on day verification.
    private EditText insert_absenID;

    private TextView textTanggalAbsen;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absen_ronda);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupDate();

        btnBackAbsen.setOnClickListener(v -> finish());

        btnKirimAbsen.setOnClickListener(v -> {
            // SECURITY: Ambil ID Warga dari session login
            String idWarga = PrefUtils.getIdWarga(this);
            String idRt = PrefUtils.getIdRt(this);
            String nama = getSharedPreferences("user_data", MODE_PRIVATE).getString("nama", "Warga");

            if (idWarga != null && idRt != null) {
                kirimAbsen(idWarga, idRt, nama);
            }
        });
    }

    private void initViews() {
        btnBackAbsen = findViewById(R.id.btnBackAbsen);
        btnKirimAbsen = findViewById(R.id.btnUploadLaporan);
        insert_absenID = findViewById(R.id.insert_absenID); // Optional now
        textTanggalAbsen = findViewById(R.id.Tanggal_absen);

        insert_absenID.setHint("Masukan ID"); // Repurpose input
    }

    private void setupDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        textTanggalAbsen.setText(sdf.format(calendar.getTime()));
    }

    // --- LOGIC KIRIM ABSEN DENGAN VERIFIKASI USER ---
    private void kirimAbsen(String idWarga, String idRt, String nama) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Memverifikasi jadwal...");
        loading.setCancelable(false);
        loading.show();

        // Check if today is the user's schedule day
        Calendar calendar = Calendar.getInstance();
        String hariIni = new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(calendar.getTime());

        db.collection("users").document(idWarga).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String jadwalHari = documentSnapshot.getString("jadwal_hari");

                    if (jadwalHari != null && jadwalHari.equalsIgnoreCase(hariIni)) {
                        // Correct Day -> Submit Absen
                        submitAbsenToFirestore(idWarga, idRt, nama, loading);
                    } else {
                        loading.dismiss();
                        Toast.makeText(this, "Maaf, hari ini (" + hariIni + ") bukan jadwal ronda Anda (" + jadwalHari + ").", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loading.dismiss();
                    Toast.makeText(this, "Gagal verifikasi profil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void submitAbsenToFirestore(String idWarga, String idRt, String nama, ProgressDialog loading) {
        String catatan = insert_absenID.getText().toString().trim();
        String waktuStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> absenData = new HashMap<>();
        absenData.put("id_warga", idWarga);
        absenData.put("nama", nama); // Store name to avoid extra queries later
        absenData.put("id_rt", idRt);
        absenData.put("tanggal", Timestamp.now());
        absenData.put("waktu", waktuStr);
        absenData.put("catatan", catatan);
        absenData.put("status", "pending");

        db.collection("absensi").add(absenData)
                .addOnSuccessListener(documentReference -> {
                    loading.dismiss();
                    Toast.makeText(this, "Absen berhasil dikirim! Menunggu validasi RT.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loading.dismiss();
                    Toast.makeText(this, "Gagal kirim absen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
