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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terima_laporan);

        db = FirebaseFirestore.getInstance();

        // Inisialisasi View
        rvLaporan = findViewById(R.id.rvLaporanMasuk);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        rvLaporan.setLayoutManager(new LinearLayoutManager(this));

        // Tombol Kembali
        btnBack.setOnClickListener(v -> finish());

        // Load Data
        loadLaporan();
    }

    private void loadLaporan() {
        String idRt = PrefUtils.getIdRt(this);
        if (idRt == null) {
            Toast.makeText(this, "ID RT tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("laporan")
                .whereEqualTo("id_rt", idRt)
                .orderBy("tanggal", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Belum ada laporan masuk.", Toast.LENGTH_SHORT).show();
                    }

                    List<Map<String, Object>> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        list.add(doc.getData());
                    }
                    LaporanAdapter adapter = new LaporanAdapter(list);
                    rvLaporan.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("FIRESTORE_LAPORAN", e.getMessage());
                    Toast.makeText(this, "Gagal muat laporan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // === ADAPTER ===
    class LaporanAdapter extends RecyclerView.Adapter<LaporanAdapter.Holder> {
        List<Map<String, Object>> data;

        public LaporanAdapter(List<Map<String, Object>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_laporan_masuk, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            try {
                Map<String, Object> item = data.get(position);

                holder.tvJenis.setText((String) item.get("jenis_laporan"));
                holder.tvNama.setText("Oleh: " + item.get("nama_pelapor"));
                holder.tvDeskripsi.setText((String) item.get("isi_laporan"));

                Timestamp ts = (Timestamp) item.get("tanggal");
                if (ts != null) {
                    Date date = ts.toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
                    holder.tvWaktu.setText(sdf.format(date));
                } else {
                    holder.tvWaktu.setText("-");
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

            public Holder(@NonNull View itemView) {
                super(itemView);
                tvJenis = itemView.findViewById(R.id.tvJenisLaporan);
                tvNama = itemView.findViewById(R.id.tvNamaPelapor);
                tvDeskripsi = itemView.findViewById(R.id.tvDeskripsi);
                tvWaktu = itemView.findViewById(R.id.tvWaktu);
            }
        }
    }
}
