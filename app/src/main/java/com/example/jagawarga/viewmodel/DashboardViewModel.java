package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.model.PosRonda;
import com.example.jagawarga.data.repository.DataRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;

/**
 * ViewModel untuk DashboardActivity, DashboardRtActivity, DashboardRwActivity.
 * Mengelola data Pos Ronda dan Pengumuman (realtime).
 *
 * ListenerRegistration di-detach otomatis di onCleared() — aman dari leak.
 */
public class DashboardViewModel extends ViewModel {

    private final DataRepository dataRepository;

    private final MutableLiveData<PosRonda> posRondaData = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> pengumumanList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Snapshot listener — dikelola lifecycle-safe
    private ListenerRegistration pengumumanListener;

    public DashboardViewModel() {
        this.dataRepository = new DataRepository();
    }

    public LiveData<PosRonda> getPosRondaData() {
        return posRondaData;
    }

    public LiveData<List<Map<String, Object>>> getPengumumanList() {
        return pengumumanList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Load data Pos Ronda untuk RT tertentu (one-shot).
     */
    public void loadPosRonda(String idRt) {
        dataRepository.fetchPosRonda(idRt, new DataRepository.PosRondaCallback() {
            @Override
            public void onSuccess(PosRonda posRonda) {
                posRondaData.setValue(posRonda);
            }

            @Override
            public void onNotFound() {
                posRondaData.setValue(new PosRonda(idRt, null, null));
            }

            @Override
            public void onError(String msg) {
                errorMessage.setValue(msg);
            }
        });
    }

    /**
     * Listen pengumuman terbaru (max 3) secara realtime.
     * Hanya pasang 1 listener — jika sudah ada, skip.
     */
    public void listenPengumuman() {
        if (pengumumanListener != null)
            return; // sudah aktif

        pengumumanListener = dataRepository.listenPengumuman(
                new DataRepository.PengumumanCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> dataList) {
                        pengumumanList.setValue(dataList);
                    }

                    @Override
                    public void onError(String msg) {
                        errorMessage.setValue(msg);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pengumumanListener != null) {
            pengumumanListener.remove();
            pengumumanListener = null;
        }
    }
}
