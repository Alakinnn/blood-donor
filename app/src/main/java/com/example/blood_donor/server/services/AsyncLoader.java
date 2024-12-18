package com.example.blood_donor.server.services;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncLoader {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LoadCallback<T> {
        void onLoaded(T data);
        void onError(Exception e);
    }

    public static <T> void loadInBackground(Task<T> task, LoadCallback<T> callback) {
        executor.submit(() -> {
            try {
                T result = task.execute();
                mainHandler.post(() -> callback.onLoaded(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public interface Task<T> {
        T execute() throws Exception;
    }
}
