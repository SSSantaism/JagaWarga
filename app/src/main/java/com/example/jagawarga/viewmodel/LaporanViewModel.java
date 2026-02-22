package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.repository.DataRepository;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel untuk LaporanKeamananActivity.
 * Mengelola validasi dan pengiriman laporan keamanan.
 */
public class LaporanViewModel extends ViewModel {

    private final DataRepository dataRepository;

    private final MutableLiveData<LaporanResult> laporanResult = new MutableLiveData<>();

    public LaporanViewModel() {
        this.dataRepository = new DataRepository();
    }

    public LiveData<LaporanResult> getLaporanResult() {
        return laporanResult;
    }

    /**
     * Validasi input dan kirim laporan.
     */
    public void submitLaporan(String idWarga, String idRt, String nama,
            String isi, String jenis) {
        if (isi == null || isi.trim().isEmpty()) {
            laporanResult.setValue(LaporanResult.validationError());
            return;
        }

        if (idWarga == null || idRt == null) {
            laporanResult.setValue(LaporanResult.sessionError());
            return;
        }

        Map<String, Object> laporan = new HashMap<>();
        laporan.put("id_warga", idWarga);
        laporan.put("pelapor", nama);
        laporan.put("id_rt", idRt);
        laporan.put("isi_laporan", isi);
        laporan.put("jenis_laporan", jenis);
        laporan.put("tanggal", Timestamp.now());

        dataRepository.submitLaporan(laporan, new DataRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                laporanResult.setValue(LaporanResult.success());
            }

            @Override
            public void onError(String msg) {
                laporanResult.setValue(LaporanResult.error(msg));
            }
        });
    }

    // ========================================================================
    // Result wrapper
    // ========================================================================

    public static class LaporanResult {
        public enum Status {
            SUCCESS, VALIDATION_ERROR, SESSION_ERROR, ERROR
        }

        private final Status status;
        private final String errorMessage;

        private LaporanResult(Status status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public static LaporanResult success() {
            return new LaporanResult(Status.SUCCESS, null);
        }

        public static LaporanResult validationError() {
            return new LaporanResult(Status.VALIDATION_ERROR, null);
        }

        public static LaporanResult sessionError() {
            return new LaporanResult(Status.SESSION_ERROR, null);
        }

        public static LaporanResult error(String msg) {
            return new LaporanResult(Status.ERROR, msg);
        }

        public Status getStatus() {
            return status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
