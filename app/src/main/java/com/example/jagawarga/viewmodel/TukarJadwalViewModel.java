package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.repository.TukarJadwalRepository;

/**
 * ViewModel untuk TukarJadwalActivity.
 * Mengelola state pencarian target dan pengiriman swap request.
 *
 * Menggunakan Task Chaining — tidak ada nested callback.
 */
public class TukarJadwalViewModel extends ViewModel {

    private final TukarJadwalRepository repository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SwapResult> swapResult = new MutableLiveData<>();

    public TukarJadwalViewModel() {
        this.repository = new TukarJadwalRepository();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<SwapResult> getSwapResult() {
        return swapResult;
    }

    /**
     * Cari target user dan kirim swap request.
     * Satu panggilan Task chain ke repository — flat, tanpa nested callback.
     */
    public void sendSwapRequest(String myUserId, String myNama, String myJadwalId,
            String myJadwalHari, String myIdRt,
            String targetJadwalId) {
        isLoading.setValue(true);

        repository.sendSwapRequest(myUserId, myNama, myJadwalId,
                myJadwalHari, myIdRt, targetJadwalId)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);

                    if (task.isSuccessful()) {
                        swapResult.setValue(SwapResult.success(task.getResult()));
                        return;
                    }

                    // Map custom exceptions ke SwapResult states
                    Exception e = task.getException();
                    if (e instanceof TukarJadwalRepository.TargetNotFoundException) {
                        swapResult.setValue(SwapResult.targetNotFound());
                    } else if (e instanceof TukarJadwalRepository.SwapAlreadyExistsException) {
                        swapResult.setValue(SwapResult.alreadyExists());
                    } else {
                        String msg = e != null ? e.getMessage() : "Gagal mengirim permintaan";
                        swapResult.setValue(SwapResult.error(msg));
                    }
                });
    }

    // ========================================================================
    // Result wrapper
    // ========================================================================

    public static class SwapResult {
        public enum Status {
            SUCCESS, TARGET_NOT_FOUND, ALREADY_EXISTS, ERROR
        }

        private final Status status;
        private final String targetNama;
        private final String errorMessage;

        private SwapResult(Status status, String targetNama, String errorMessage) {
            this.status = status;
            this.targetNama = targetNama;
            this.errorMessage = errorMessage;
        }

        public static SwapResult success(String nama) {
            return new SwapResult(Status.SUCCESS, nama, null);
        }

        public static SwapResult targetNotFound() {
            return new SwapResult(Status.TARGET_NOT_FOUND, null, null);
        }

        public static SwapResult alreadyExists() {
            return new SwapResult(Status.ALREADY_EXISTS, null, null);
        }

        public static SwapResult error(String msg) {
            return new SwapResult(Status.ERROR, null, msg);
        }

        public Status getStatus() {
            return status;
        }

        public String getTargetNama() {
            return targetNama;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
