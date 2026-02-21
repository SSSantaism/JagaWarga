package com.example.jagawarga;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.utils.Constants;
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
                Toast.makeText(this, getString(R.string.toast_judul_isi_required), Toast.LENGTH_SHORT).show();
                return;
            }

            String idRt = PrefUtils.getIdRt(this);
            if (idRt == null) {
                Toast.makeText(this, getString(R.string.toast_session_expired), Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage(getString(R.string.progress_publishing));
            pd.setCancelable(false);
            pd.show();

            Map<String, Object> pengumuman = new HashMap<>();
            pengumuman.put(Constants.FIELD_JUDUL, judul);
            pengumuman.put(Constants.FIELD_ISI, isi);
            pengumuman.put(Constants.FIELD_ID_RT, idRt);
            pengumuman.put(Constants.FIELD_TANGGAL, Timestamp.now());

            db.collection(Constants.COLLECTION_PENGUMUMAN)
                    .add(pengumuman)
                    .addOnSuccessListener(ref -> {
                        pd.dismiss();
                        Toast.makeText(this, getString(R.string.toast_publish_success), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(this, getString(R.string.toast_publish_failed, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
