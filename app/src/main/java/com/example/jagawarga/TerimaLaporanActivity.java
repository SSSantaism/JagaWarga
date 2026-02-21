package com.example.jagawarga;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TerimaLaporanActivity extends AppCompatActivity {

    private RecyclerView rvLaporan;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String idRt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terima_laporan);

        db = FirebaseFirestore.getInstance();
        idRt = PrefUtils.getIdRt(this);

        rvLaporan = findViewById(R.id.rvLaporanMasuk);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        rvLaporan.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        if (idRt != null) {
            loadLaporan();
        }
    }

    private void loadLaporan() {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        db.collection(Constants.COLLECTION_LAPORAN)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .orderBy(Constants.FIELD_TANGGAL, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, getString(R.string.toast_no_reports), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Map<String, Object>> dataList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        dataList.add(doc.getData());
                    }
                    rvLaporan.setAdapter(new LaporanAdapter(dataList));
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.toast_load_laporan_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // --- ADAPTER ---
    class LaporanAdapter extends RecyclerView.Adapter<LaporanAdapter.Holder> {
        List<Map<String, Object>> data;

        LaporanAdapter(List<Map<String, Object>> data) {
            this.data = data;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_laporan_masuk, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            try {
                Map<String, Object> item = data.get(position);

                holder.tvJenis.setText((String) item.get(Constants.FIELD_JENIS_LAPORAN));
                holder.tvNama.setText((String) item.get(Constants.FIELD_NAMA_PELAPOR));
                holder.tvDeskripsi.setText((String) item.get(Constants.FIELD_ISI_LAPORAN));

                // Format tanggal
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) item
                        .get(Constants.FIELD_TANGGAL);
                if (timestamp != null) {
                    Date date = timestamp.toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
                    holder.tvWaktu.setText(sdf.format(date));
                } else {
                    holder.tvWaktu.setText(getString(R.string.text_dash));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvJenis, tvNama, tvDeskripsi, tvWaktu;

            Holder(View v) {
                super(v);
                tvJenis = v.findViewById(R.id.tvJenisLaporan);
                tvNama = v.findViewById(R.id.tvNamaPelapor);
                tvDeskripsi = v.findViewById(R.id.tvDeskripsi);
                tvWaktu = v.findViewById(R.id.tvWaktu);
            }
        }
    }
}
