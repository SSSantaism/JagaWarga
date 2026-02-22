package com.example.jagawarga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jagawarga.data.model.User;
import com.example.jagawarga.data.repository.AuthRepository;

/**
 * ViewModel untuk LoginActivity.
 * Mengelola UI state (loading, error, login result) dan berinteraksi dengan
 * AuthRepository.
 */
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;

    // ========================================================================
    // UI State LiveData
    // ========================================================================
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final MutableLiveData<RegisterResult> registerResult = new MutableLiveData<>();

    public LoginViewModel() {
        this.authRepository = new AuthRepository();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public LiveData<RegisterResult> getRegisterResult() {
        return registerResult;
    }

    // ========================================================================
    // Auto-Login Check
    // ========================================================================

    /**
     * Cek auto-login: jika FirebaseAuth masih ada session, lakukan checkUserRole.
     */
    public void checkAutoLogin() {
        if (authRepository.getCurrentUser() != null) {
            isLoading.setValue(true);
            checkUserRoleInternal(authRepository.getCurrentUser().getUid());
        }
    }

    // ========================================================================
    // Login
    // ========================================================================

    public void login(String rawPhone, String password) {
        isLoading.setValue(true);

        authRepository.login(rawPhone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                // Auth berhasil, cek role & status di Firestore
                checkUserRoleInternal(uid);
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
                loginResult.setValue(LoginResult.error(errorMessage));
            }
        });
    }

    // ========================================================================
    // Register
    // ========================================================================

    public void register(String nama, String rawPhone, String password, String rt) {
        isLoading.setValue(true);

        authRepository.register(rawPhone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                // Auth berhasil, simpan data ke Firestore
                authRepository.saveNewUser(uid, rawPhone, nama, rt,
                        new AuthRepository.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                isLoading.setValue(false);
                                registerResult.setValue(RegisterResult.success());
                            }

                            @Override
                            public void onError(String errorMessage) {
                                isLoading.setValue(false);
                                registerResult.setValue(RegisterResult.error(errorMessage));
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
                registerResult.setValue(RegisterResult.error(errorMessage));
            }
        });
    }

    // ========================================================================
    // Logout (dipanggil saat remember me tidak aktif)
    // ========================================================================

    public void logoutIfNeeded() {
        authRepository.logout();
    }

    // ========================================================================
    // Internal
    // ========================================================================

    private void checkUserRoleInternal(String uid) {
        authRepository.checkUserRole(uid, new AuthRepository.UserDataCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                loginResult.setValue(LoginResult.success(user));
            }

            @Override
            public void onPending() {
                isLoading.setValue(false);
                loginResult.setValue(LoginResult.pending());
            }

            @Override
            public void onRejected() {
                isLoading.setValue(false);
                loginResult.setValue(LoginResult.rejected());
            }

            @Override
            public void onNotFound() {
                isLoading.setValue(false);
                loginResult.setValue(LoginResult.notFound());
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
                loginResult.setValue(LoginResult.error(errorMessage));
            }
        });
    }

    // ========================================================================
    // Result wrapper classes
    // ========================================================================

    public static class LoginResult {
        public enum Status {
            SUCCESS, ERROR, PENDING, REJECTED, NOT_FOUND
        }

        private final Status status;
        private final User user;
        private final String errorMessage;

        private LoginResult(Status status, User user, String errorMessage) {
            this.status = status;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        public static LoginResult success(User user) {
            return new LoginResult(Status.SUCCESS, user, null);
        }

        public static LoginResult error(String msg) {
            return new LoginResult(Status.ERROR, null, msg);
        }

        public static LoginResult pending() {
            return new LoginResult(Status.PENDING, null, null);
        }

        public static LoginResult rejected() {
            return new LoginResult(Status.REJECTED, null, null);
        }

        public static LoginResult notFound() {
            return new LoginResult(Status.NOT_FOUND, null, null);
        }

        public Status getStatus() {
            return status;
        }

        public User getUser() {
            return user;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class RegisterResult {
        private final boolean success;
        private final String errorMessage;

        private RegisterResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static RegisterResult success() {
            return new RegisterResult(true, null);
        }

        public static RegisterResult error(String msg) {
            return new RegisterResult(false, msg);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
