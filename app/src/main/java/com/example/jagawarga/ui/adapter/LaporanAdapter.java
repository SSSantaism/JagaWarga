package com.example.jagawarga.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.R;
import com.example.jagawarga.utils.Constants;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter untuk menampilkan daftar laporan masuk di RecyclerView.
 * Diekstrak dari inner class TerimaLaporanActivity.
 */
public class LaporanAdapter extends RecyclerView.Adapter<LaporanAdapter.Holder> {

    private List<Map<String, Object>> data;

    public LaporanAdapter(List<Map<String, Object>> data) {
        this.data = data;
    }

    public void updateData(List<Map<String, Object>> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_laporan_masuk, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        try {
            Map<String, Object> item = data.get(position);

            holder.tvJenis.setText((String) item.get(Constants.FIELD_JENIS_LAPORAN));
            holder.tvNama.setText((String) item.get(Constants.FIELD_NAMA_PELAPOR));
            holder.tvDeskripsi.setText((String) item.get(Constants.FIELD_ISI_LAPORAN));

            Timestamp timestamp = (Timestamp) item.get(Constants.FIELD_TANGGAL);
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm",
                        new Locale("id", "ID"));
                holder.tvWaktu.setText(sdf.format(date));
            } else {
                holder.tvWaktu.setText(holder.itemView.getContext().getString(R.string.text_dash));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvJenis, tvNama, tvDeskripsi, tvWaktu;

        Holder(@NonNull View v) {
            super(v);
            tvJenis = v.findViewById(R.id.tvJenisLaporan);
            tvNama = v.findViewById(R.id.tvNamaPelapor);
            tvDeskripsi = v.findViewById(R.id.tvDeskripsi);
            tvWaktu = v.findViewById(R.id.tvWaktu);
        }
    }
}
