package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.repository.DataRepository;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel untuk AbsenRondaActivity.
 * Mengelola verifikasi hari jadwal dan pengiriman data absensi.
 */
public class AbsenViewModel extends ViewModel {

    private final DataRepository dataRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<AbsenResult> absenResult = new MutableLiveData<>();

    public AbsenViewModel() {
        this.dataRepository = new DataRepository();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<AbsenResult> getAbsenResult() {
        return absenResult;
    }

    /**
     * Verifikasi hari dan kirim absensi jika cocok.
     */
    public void submitAbsen(String idWarga, String idRt, String nama, String catatan) {
        isLoading.setValue(true);

        // Cek hari ini
        Calendar calendar = Calendar.getInstance();
        String hariIni = new SimpleDateFormat("EEEE", new Locale("id", "ID"))
                .format(calendar.getTime());

        dataRepository.verifyScheduleDay(idWarga, hariIni,
                new DataRepository.DayCheckCallback() {
                    @Override
                    public void onMatch() {
                        // Hari cocok, submit absen
                        String waktuStr = new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(new Date());

                        Map<String, Object> absenData = new HashMap<>();
                        absenData.put("id_warga", idWarga);
                        absenData.put("nama", nama);
                        absenData.put("id_rt", idRt);
                        absenData.put("tanggal", Timestamp.now());
                        absenData.put("waktu", waktuStr);
                        absenData.put("catatan", catatan);
                        absenData.put("status", "pending");

                        dataRepository.submitAbsensi(absenData,
                                new DataRepository.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        isLoading.setValue(false);
                                        absenResult.setValue(AbsenResult.success());
                                    }

                                    @Override
                                    public void onError(String msg) {
                                        isLoading.setValue(false);
                                        absenResult.setValue(AbsenResult.error(msg));
                                    }
                                });
                    }

                    @Override
                    public void onMismatch(String hariIni, String jadwalHari) {
                        isLoading.setValue(false);
                        absenResult.setValue(AbsenResult.wrongDay(hariIni, jadwalHari));
                    }

                    @Override
                    public void onError(String msg) {
                        isLoading.setValue(false);
                        absenResult.setValue(AbsenResult.error(msg));
                    }
                });
    }

    // ========================================================================
    // Result wrapper
    // ========================================================================

    public static class AbsenResult {
        public enum Status {
            SUCCESS, WRONG_DAY, ERROR
        }

        private final Status status;
        private final String hariIni;
        private final String jadwalHari;
        private final String errorMessage;

        private AbsenResult(Status status, String hariIni, String jadwalHari, String errorMessage) {
            this.status = status;
            this.hariIni = hariIni;
            this.jadwalHari = jadwalHari;
            this.errorMessage = errorMessage;
        }

        public static AbsenResult success() {
            return new AbsenResult(Status.SUCCESS, null, null, null);
        }

        public static AbsenResult wrongDay(String hariIni, String jadwalHari) {
            return new AbsenResult(Status.WRONG_DAY, hariIni, jadwalHari, null);
        }

        public static AbsenResult error(String msg) {
            return new AbsenResult(Status.ERROR, null, null, msg);
        }

        public Status getStatus() {
            return status;
        }

        public String getHariIni() {
            return hariIni;
        }

        public String getJadwalHari() {
            return jadwalHari;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
