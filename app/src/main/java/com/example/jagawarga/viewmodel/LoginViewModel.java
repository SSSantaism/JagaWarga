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
 *
 * Menggunakan Task Chaining — tidak ada nested callback.
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
            String uid = authRepository.getCurrentUser().getUid();

            authRepository.checkUserRole(uid)
                    .addOnCompleteListener(task -> handleLoginTask(task));
        }
    }

    // ========================================================================
    // Login — Task Chain: login() → checkUserRole()
    // ========================================================================

    public void login(String rawPhone, String password) {
        isLoading.setValue(true);

        authRepository.login(rawPhone, password)
                .continueWithTask(loginTask -> {
                    // Auth berhasil, chain ke cek role & status
                    String uid = loginTask.getResult();
                    return authRepository.checkUserRole(uid);
                })
                .addOnCompleteListener(task -> handleLoginTask(task));
    }

    // ========================================================================
    // Register — Task Chain: register() → saveNewUser()
    // ========================================================================

    public void register(String nama, String rawPhone, String password, String rt) {
        isLoading.setValue(true);

        authRepository.register(rawPhone, password)
                .continueWithTask(regTask -> {
                    // Auth berhasil, chain ke simpan data ke Firestore
                    String uid = regTask.getResult();
                    return authRepository.saveNewUser(uid, rawPhone, nama, rt);
                })
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        registerResult.setValue(RegisterResult.success());
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registrasi gagal";
                        registerResult.setValue(RegisterResult.error(msg));
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
    // Internal — Unified handler for login/autoLogin tasks
    // ========================================================================

    /**
     * Handle hasil akhir Task<User> dari login/autoLogin.
     * Mapping custom exceptions ke LoginResult states.
     */
    private void handleLoginTask(com.google.android.gms.tasks.Task<User> task) {
        isLoading.setValue(false);

        if (task.isSuccessful()) {
            loginResult.setValue(LoginResult.success(task.getResult()));
            return;
        }

        Exception e = task.getException();
        if (e instanceof AuthRepository.UserPendingException) {
            loginResult.setValue(LoginResult.pending());
        } else if (e instanceof AuthRepository.UserRejectedException) {
            loginResult.setValue(LoginResult.rejected());
        } else if (e instanceof AuthRepository.UserNotFoundException) {
            loginResult.setValue(LoginResult.notFound());
        } else {
            String msg = e != null ? e.getMessage() : "Login gagal";
            loginResult.setValue(LoginResult.error(msg));
        }
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
