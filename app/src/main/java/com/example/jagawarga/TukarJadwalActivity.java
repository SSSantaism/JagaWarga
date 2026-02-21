package com.example.jagawarga;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.utils.Constants;
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
                Toast.makeText(this, getString(R.string.toast_enter_target_jadwal),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (idJadwalTujuan.equals(myJadwalId)) {
                Toast.makeText(this, getString(R.string.toast_cannot_swap_self),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Cari user dengan jadwal_id tersebut
            findTargetUserAndSendRequest(idJadwalTujuan);
        });
    }

    private void findTargetUserAndSendRequest(String targetJadwalId) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.progress_searching_jadwal));
        pd.show();

        btnTukar.setEnabled(false);

        // Cari user dengan jadwal_id yang sesuai dan RT yang sama
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_JADWAL_ID, targetJadwalId)
                .whereEqualTo(Constants.FIELD_ID_RT, myIdRt)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        pd.dismiss();
                        btnTukar.setEnabled(true);
                        Toast.makeText(this, getString(R.string.toast_jadwal_not_found_rt),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Ambil data target user
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String targetUserId = doc.getId();
                        String targetNama = doc.getString(Constants.FIELD_NAMA);
                        String targetJadwalHari = doc.getString(Constants.FIELD_JADWAL_HARI);

                        // Kirim permintaan tukar jadwal
                        sendSwapRequest(pd, targetUserId, targetNama, targetJadwalHari, targetJadwalId);
                        break; // Hanya ambil satu
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    btnTukar.setEnabled(true);
                    Log.e(TAG, "Error finding target: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.toast_swap_search_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSwapRequest(ProgressDialog pd, String targetUserId, String targetNama,
            String targetJadwalHari, String targetJadwalId) {
        pd.setMessage(getString(R.string.progress_sending_request));

        // Cek apakah sudah ada permintaan pending yang sama
        db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                .whereEqualTo(Constants.FIELD_DARI_ID, myUserId)
                .whereEqualTo(Constants.FIELD_KEPADA_ID, targetUserId)
                .whereEqualTo(Constants.FIELD_STATUS, Constants.STATUS_PENDING)
                .get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        pd.dismiss();
                        btnTukar.setEnabled(true);
                        Toast.makeText(this, getString(R.string.toast_swap_request_exists),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Buat document permintaan tukar jadwal
                    Map<String, Object> swapRequest = new HashMap<>();
                    swapRequest.put(Constants.FIELD_DARI_ID, myUserId);
                    swapRequest.put(Constants.FIELD_DARI_NAMA, myNama);
                    swapRequest.put(Constants.FIELD_DARI_JADWAL_ID, myJadwalId);
                    swapRequest.put(Constants.FIELD_HARI_DARI, myJadwalHari);
                    swapRequest.put(Constants.FIELD_KEPADA_ID, targetUserId);
                    swapRequest.put(Constants.FIELD_KEPADA_NAMA, targetNama);
                    swapRequest.put(Constants.FIELD_KEPADA_JADWAL_ID, targetJadwalId);
                    swapRequest.put(Constants.FIELD_HARI_KEPADA, targetJadwalHari);
                    swapRequest.put(Constants.FIELD_ID_RT, myIdRt);
                    swapRequest.put(Constants.FIELD_STATUS, Constants.STATUS_PENDING);
                    swapRequest.put(Constants.FIELD_CREATED_AT, Timestamp.now());

                    db.collection(Constants.COLLECTION_TUKAR_JADWAL)
                            .add(swapRequest)
                            .addOnSuccessListener(documentReference -> {
                                pd.dismiss();
                                Log.d(TAG, "Swap request created: " + documentReference.getId());
                                Toast.makeText(this,
                                        getString(R.string.toast_swap_request_sent, targetNama),
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                pd.dismiss();
                                btnTukar.setEnabled(true);
                                Log.e(TAG, "Error sending request: " + e.getMessage());
                                Toast.makeText(this,
                                        getString(R.string.toast_swap_request_failed, e.getMessage()),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    btnTukar.setEnabled(true);
                    Log.e(TAG, "Error checking existing: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.toast_generic_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
