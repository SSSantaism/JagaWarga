package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.PrefUtils;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.utils.Constants;
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
            String nama = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE)
                    .getString(Constants.PREF_KEY_NAMA, getString(R.string.fallback_name_warga));

            if (idWarga != null && idRt != null) {
                kirimAbsen(idWarga, idRt, nama);
            }
        });
    }

    private void initViews() {
        btnBackAbsen = findViewById(R.id.btnBackAbsen);
        btnKirimAbsen = findViewById(R.id.btnUploadLaporan);
        insert_absenID = findViewById(R.id.insert_absenID);
        textTanggalAbsen = findViewById(R.id.Tanggal_absen);

        // Auto-fill ID Jadwal dari session
        String jadwalId = PrefUtils.getJadwalId(this);
        if (jadwalId != null) {
            insert_absenID.setText(jadwalId);
            insert_absenID.setEnabled(false); // Read-only
            insert_absenID.setFocusable(false);
        } else {
            insert_absenID.setHint(getString(R.string.hint_jadwal_not_found));
            insert_absenID.setEnabled(false);
        }
    }

    private void setupDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        textTanggalAbsen.setText(sdf.format(calendar.getTime()));
    }

    // --- LOGIC KIRIM ABSEN DENGAN VERIFIKASI USER ---
    private void kirimAbsen(String idWarga, String idRt, String nama) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage(getString(R.string.toast_absen_verifying));
        loading.setCancelable(false);
        loading.show();

        // Check if today is the user's schedule day
        Calendar calendar = Calendar.getInstance();
        String hariIni = new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(calendar.getTime());

        db.collection(Constants.COLLECTION_USERS).document(idWarga).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String jadwalHari = documentSnapshot.getString(Constants.FIELD_JADWAL_HARI);

                    if (jadwalHari != null && jadwalHari.equalsIgnoreCase(hariIni)) {
                        // Correct Day -> Submit Absen
                        submitAbsenToFirestore(idWarga, idRt, nama, loading);
                    } else {
                        loading.dismiss();
                        Toast.makeText(this,
                                getString(R.string.toast_absen_wrong_day, hariIni, jadwalHari),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loading.dismiss();
                    Toast.makeText(this, getString(R.string.toast_verify_profile_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void submitAbsenToFirestore(String idWarga, String idRt, String nama, ProgressDialog loading) {
        String catatan = insert_absenID.getText().toString().trim();
        String waktuStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> absenData = new HashMap<>();
        absenData.put(Constants.FIELD_ID_WARGA, idWarga);
        absenData.put(Constants.FIELD_NAMA, nama);
        absenData.put(Constants.FIELD_ID_RT, idRt);
        absenData.put(Constants.FIELD_TANGGAL, Timestamp.now());
        absenData.put(Constants.FIELD_WAKTU, waktuStr);
        absenData.put(Constants.FIELD_CATATAN, catatan);
        absenData.put(Constants.FIELD_STATUS, Constants.STATUS_PENDING);

        db.collection(Constants.COLLECTION_ABSENSI).add(absenData)
                .addOnSuccessListener(documentReference -> {
                    loading.dismiss();
                    Toast.makeText(this, getString(R.string.toast_absen_success), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loading.dismiss();
                    Toast.makeText(this, getString(R.string.toast_absen_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
