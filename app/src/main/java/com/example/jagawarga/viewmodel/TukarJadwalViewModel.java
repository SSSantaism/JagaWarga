package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.repository.TukarJadwalRepository;

/**
 * ViewModel untuk TukarJadwalActivity.
 * Mengelola state pencarian target dan pengiriman swap request.
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
     */
    public void sendSwapRequest(String myUserId, String myNama, String myJadwalId,
            String myJadwalHari, String myIdRt,
            String targetJadwalId) {
        isLoading.setValue(true);

        repository.findTargetUser(targetJadwalId, myIdRt,
                new TukarJadwalRepository.FindTargetCallback() {
                    @Override
                    public void onFound(String targetUserId, String targetNama, String targetJadwalHari) {
                        // Target ditemukan, kirim request
                        repository.sendSwapRequest(
                                myUserId, myNama, myJadwalId, myJadwalHari, myIdRt,
                                targetUserId, targetNama, targetJadwalHari, targetJadwalId,
                                new TukarJadwalRepository.SwapRequestCallback() {
                                    @Override
                                    public void onSuccess(String nama) {
                                        isLoading.setValue(false);
                                        swapResult.setValue(SwapResult.success(nama));
                                    }

                                    @Override
                                    public void onAlreadyExists() {
                                        isLoading.setValue(false);
                                        swapResult.setValue(SwapResult.alreadyExists());
                                    }

                                    @Override
                                    public void onError(String msg) {
                                        isLoading.setValue(false);
                                        swapResult.setValue(SwapResult.error(msg));
                                    }
                                });
                    }

                    @Override
                    public void onNotFound() {
                        isLoading.setValue(false);
                        swapResult.setValue(SwapResult.targetNotFound());
                    }

                    @Override
                    public void onError(String msg) {
                        isLoading.setValue(false);
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
