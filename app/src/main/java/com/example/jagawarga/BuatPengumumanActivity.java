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

import java.util.HashMap;
import java.util.Map;

public class BuatPengumumanActivity extends AppCompatActivity {

    private EditText inputJudul, inputIsi;
    private Button btnSubmit;
    private ImageButton btnBack;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buat_pengumuman);

        db = FirebaseFirestore.getInstance();

        inputJudul = findViewById(R.id.inputJudul);
        inputIsi = findViewById(R.id.inputIsi);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String judul = inputJudul.getText().toString().trim();
            String isi = inputIsi.getText().toString().trim();

            if (judul.isEmpty() || isi.isEmpty()) {
                Toast.makeText(this, "Judul dan Isi tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            } else {
                kirimPengumuman(judul, isi);
            }
        });
    }

    private void kirimPengumuman(String judul, String isi) {
        // Ambil ID RT otomatis dari user yang login (Ketua RT)
        String myIdRt = PrefUtils.getIdRt(this);

        if (myIdRt == null) {
            Toast.makeText(this, "Sesi habis, login ulang!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Menerbitkan...");
        pd.show();

        Map<String, Object> pengumuman = new HashMap<>();
        pengumuman.put("judul", judul);
        pengumuman.put("isi", isi);
        pengumuman.put("id_rt", myIdRt);
        pengumuman.put("tanggal", Timestamp.now());

        db.collection("pengumuman")
                .add(pengumuman)
                .addOnSuccessListener(documentReference -> {
                    pd.dismiss();
                    Toast.makeText(this, "Berhasil diterbitkan!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Log.e("FIRESTORE_ERR", e.getMessage());
                    Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
