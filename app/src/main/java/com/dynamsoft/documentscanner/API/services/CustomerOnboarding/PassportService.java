package com.dynamsoft.documentscanner.API.services.CustomerOnboarding;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
public class PassportService {

    private final OkHttpClient client = new OkHttpClient();
    private static final String baseUrl = "http://146.59.237.231:8090";
    public interface PassportImageCallback {
        void onSuccess(Bitmap bitmap);
        void onError(Exception e);
    }

    public void fetchPassportImage(String countryCode, String passportType,PassportImageCallback callback) {
        String url = baseUrl + "/api/passport/" + countryCode+"/"+passportType;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] imageBytes = response.body().bytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    callback.onSuccess(bitmap);
                } else {
                    callback.onError(new IOException("Erreur HTTP: " + response.code()));
                }
            }
        });
    }
}