package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.repository.DataRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel untuk JadwalRondaActivity.
 * Mengelola navigasi tanggal dan pengambilan data jadwal per hari.
 */
public class JadwalViewModel extends ViewModel {

    private final DataRepository dataRepository;
    private final Calendar calendar;
    private final SimpleDateFormat dateFormatDay;
    private final SimpleDateFormat dateFormatDisplay;

    private final MutableLiveData<String> displayDate = new MutableLiveData<>();
    private final MutableLiveData<List<DataRepository.JadwalItem>> jadwalList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public JadwalViewModel() {
        this.dataRepository = new DataRepository();
        this.calendar = Calendar.getInstance();
        this.dateFormatDay = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
        this.dateFormatDisplay = new SimpleDateFormat("EEEE, dd MMMM", new Locale("id", "ID"));

        updateDisplayDate();
    }

    public LiveData<String> getDisplayDate() {
        return displayDate;
    }

    public LiveData<List<DataRepository.JadwalItem>> getJadwalList() {
        return jadwalList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Pindah ke hari sebelumnya.
     */
    public void prevDay() {
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        updateDisplayDate();
    }

    /**
     * Pindah ke hari berikutnya.
     */
    public void nextDay() {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        updateDisplayDate();
    }

    private void updateDisplayDate() {
        String formatted = dateFormatDisplay.format(calendar.getTime());
        formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        displayDate.setValue(formatted);
    }

    /**
     * Ambil nama hari saat ini (capitalized).
     */
    public String getCurrentDay() {
        String hari = dateFormatDay.format(calendar.getTime());
        return hari.substring(0, 1).toUpperCase() + hari.substring(1).toLowerCase();
    }

    /**
     * Load data jadwal dari Firestore untuk hari yang dipilih.
     */
    public void loadJadwal(String idRt) {
        String hari = getCurrentDay();

        dataRepository.getJadwalByDay(idRt, hari, new DataRepository.JadwalListCallback() {
            @Override
            public void onSuccess(List<DataRepository.JadwalItem> items) {
                jadwalList.setValue(items);
            }

            @Override
            public void onError(String msg) {
                errorMessage.setValue(msg);
            }
        });
    }
}
