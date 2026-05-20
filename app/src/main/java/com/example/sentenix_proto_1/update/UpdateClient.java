package com.example.sentenix_proto_1.update;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class UpdateClient {
    public static final String ENDPOINT = "http://13.126.161.25:8080/check-update";
    public static final String HEADER_ROLE = "X-Client-Role";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    public interface Listener {
        void onResult(@NonNull Result result);
        void onFailure(@NonNull Throwable error);
    }

    public static final class Result {
        public final int statusCode;
        @Nullable public final String body;

        public Result(int statusCode, @Nullable String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean hasUpdate() {
            return statusCode == 200 && body != null && !body.isEmpty();
        }
    }

    private final OkHttpClient http;

    public UpdateClient() {
        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public void checkForUpdate(@NonNull String role, @NonNull Listener listener) {
        Request req = new Request.Builder()
                .url(ENDPOINT)
                .get()
                .header(HEADER_ROLE, role)
                .header("Accept", "application/json")
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String text = body != null ? body.string() : null;
                    listener.onResult(new Result(response.code(), text));
                }
            }
        });
    }
}
