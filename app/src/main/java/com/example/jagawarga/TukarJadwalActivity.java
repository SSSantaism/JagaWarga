package com.example.jagawarga;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity untuk mengajukan permintaan tukar jadwal ronda dengan warga lain.
 * Saat permintaan dikirim, target user akan menerima notifikasi melalui
 * TukarJadwalListener.
 */
public class TukarJadwalActivity extends AppCompatActivity {

    private static final String TAG = "TukarJadwal";

    private EditText inputIdJadwalSaya, inputIdJadwalTujuan;
    private Button btnTukar;
    private ImageButton btnBack;

    private FirebaseFirestore db;

    private String myUserId;
    private String myNama;
    private String myJadwalId;
    private String myJadwalHari;
    private String myIdRt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tukar_jadwal);

        db = FirebaseFirestore.getInstance();

        // Ambil data user dari SharedPreferences
        myUserId = PrefUtils.getIdWarga(this);
        myNama = PrefUtils.getNama(this);
        myJadwalId = PrefUtils.getJadwalId(this);
        myJadwalHari = PrefUtils.getJadwalHari(this);
        myIdRt = PrefUtils.getIdRt(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        inputIdJadwalSaya = findViewById(R.id.inputIdJadwalSaya);
        inputIdJadwalTujuan = findViewById(R.id.inputIdJadwalTujuan);
        btnTukar = findViewById(R.id.btnTukarAbsen);
        btnBack = findViewById(R.id.btnBackAbsen);

        // Pre-fill ID Jadwal Saya
        if (myJadwalId != null) {
            inputIdJadwalSaya.setText(myJadwalId);
            inputIdJadwalSaya.setEnabled(false); // Tidak bisa diubah
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnTukar.setOnClickListener(v -> {
            String idJadwalTujuan = inputIdJadwalTujuan.getText().toString().trim().toUpperCase();

            if (idJadwalTujuan.isEmpty()) {
                Toast.makeText(this, "Masukkan ID Jadwal tujuan!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (idJadwalTujuan.equals(myJadwalId)) {
                Toast.makeText(this, "Tidak bisa tukar dengan jadwal sendiri!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cari user dengan jadwal_id tersebut
            findTargetUserAndSendRequest(idJadwalTujuan);
        });
    }

    private void findTargetUserAndSendRequest(String targetJadwalId) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Mencari jadwal...");
        pd.show();

        btnTukar.setEnabled(false);

        // Cari user dengan jadwal_id yang sesuai dan RT yang sama
        db.collection("users")
                .whereEqualTo("jadwal_id", targetJadwalId)
                .whereEqualTo("id_rt", myIdRt)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        pd.dismiss();
                        btnTukar.setEnabled(true);
                        Toast.makeText(this, "ID Jadwal tidak ditemukan di RT Anda!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Ambil data target user
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String targetUserId = doc.getId();
                        String targetNama = doc.getString("nama");
                        String targetJadwalHari = doc.getString("jadwal_hari");

                        // Kirim permintaan tukar jadwal
                        sendSwapRequest(pd, targetUserId, targetNama, targetJadwalHari, targetJadwalId);
                        break; // Hanya ambil satu
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    btnTukar.setEnabled(true);
                    Log.e(TAG, "Error finding target: " + e.getMessage());
                    Toast.makeText(this, "Gagal mencari jadwal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSwapRequest(ProgressDialog pd, String targetUserId, String targetNama,
            String targetJadwalHari, String targetJadwalId) {
        pd.setMessage("Mengirim permintaan...");

        // Cek apakah sudah ada permintaan pending yang sama
        db.collection("tukar_jadwal")
                .whereEqualTo("dari_id", myUserId)
                .whereEqualTo("kepada_id", targetUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        pd.dismiss();
                        btnTukar.setEnabled(true);
                        Toast.makeText(this, "Anda sudah mengirim permintaan ke user ini!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Buat document permintaan tukar jadwal
                    Map<String, Object> swapRequest = new HashMap<>();
                    swapRequest.put("dari_id", myUserId);
                    swapRequest.put("dari_nama", myNama);
                    swapRequest.put("dari_jadwal_id", myJadwalId);
                    swapRequest.put("hari_dari", myJadwalHari);
                    swapRequest.put("kepada_id", targetUserId);
                    swapRequest.put("kepada_nama", targetNama);
                    swapRequest.put("kepada_jadwal_id", targetJadwalId);
                    swapRequest.put("hari_kepada", targetJadwalHari);
                    swapRequest.put("id_rt", myIdRt);
                    swapRequest.put("status", "pending");
                    swapRequest.put("created_at", Timestamp.now());

                    db.collection("tukar_jadwal")
                            .add(swapRequest)
                            .addOnSuccessListener(documentReference -> {
                                pd.dismiss();
                                Log.d(TAG, "Swap request created: " + documentReference.getId());
                                Toast.makeText(this,
                                        "Permintaan tukar jadwal dikirim ke " + targetNama + "!",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                pd.dismiss();
                                btnTukar.setEnabled(true);
                                Log.e(TAG, "Error sending request: " + e.getMessage());
                                Toast.makeText(this, "Gagal mengirim permintaan: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    btnTukar.setEnabled(true);
                    Log.e(TAG, "Error checking existing: " + e.getMessage());
                    Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
