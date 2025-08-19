package com.dynamsoft.documentscanner.API.services.CustomerOnboarding;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomerService {
    //private static final String BASE_URL = "http://10.0.2.2:8080/api/v1";
    private static final String BASE_URL = "https://17e4263434f9.ngrok-free.app/api/v1";
    private final OkHttpClient client;
    private static final String BEARER_TOKEN = "aW5rX2I5YWU5MjY5N2JlMjQ0Y2YwZmQ3NDNiNjMwMjE1ODVlOmluc19leUp0WlhSaFpHRjBZU0k2SUhzaVkyeHBaVzUwSWpvZ2V5SnBaQ0k2SUNJNVpUWmpOR1JpWWkxbU5qRm1MVFExWVRndFlUQTFaaTB3WkdVek1qUXlOamhpTm1FaUxDQWlibUZ0WlNJNklDSlRUVXhVVUNKOUxDQWliR2xqWlc1elpWOWpkWE4wYjIxZmNISnZjR1Z5ZEdsbGN5STZJSHNpTDJOdmJuUnlZV04wTDJSdmRDOWthWE12Wlc1aFlteGxaQ0k2SUNKMGNuVmxJaXdnSWk5amIyNTBjbUZqZEM5a2IzUXZaWFpoYkhWaGRHbHZiaUk2SUNKMGNuVmxJaXdnSWk5amIyNTBjbUZqZEM5a2IzUXZaR2x6TDJ4cFkyVnVjMlZXWlhKemFXOXVJam9nSWpNaUxDQWlMMk52Ym5SeVlXTjBMM050WVhKMFptRmpaVjlsYldKbFpHUmxaQzlsYm1GaWJHVWlPaUFpZEhKMVpTSXNJQ0l2WTI5dWRISmhZM1F2YzIxaGNuUm1ZV05sWDJWdFltVmtaR1ZrTDNCaGJHMGlPaUFpZEhKMVpTSXNJQ0l2WTI5dWRISmhZM1F2YzIxaGNuUm1ZV05sWDJWdFltVmtaR1ZrTDNCaGJHMWZiR2wyWlc1bGMzTWlPaUFpZEhKMVpTSjlMQ0FpWTNKbFlYUnBiMjVmZEdsdFpYTjBZVzF3SWpvZ0lqQTNMekV4THpJd01qVWdNRGs2TVRRNk1qQWdWVlJESWl3Z0luWmhiR2xrWDNSdklqb2dJakE1THpBNUx6SXdNalVnTURBNk1EQTZNREFnVlZSREluMHNJQ0p6YVdkdVlYUjFjbVVpT2lBaVNHbzFRVXB1Y0c4MFIyTjZMemhVVjJoNVdWQm9NVGc1U2xORlVqSklaV2xsV0hWblZqUlpPR1prVEU5UGRtOW5ZM3BDUVRsdWQzb3lSVGhaV0RGYUwxaEtaVXBQWjJ0T2IzWktSVFZpV21SbFZWcDJRbWM5UFNKOQ==";
    private final Gson gson;

    public CustomerService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 30 seconds to establish connection
                .readTimeout(60, TimeUnit.SECONDS)    // 60 seconds to read data
                .writeTimeout(60, TimeUnit.SECONDS)   // 60 seconds to write data
                .build();
        this.gson = new Gson();

    }

    public void createCustomer(final ApiCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/customers")
                .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                .post(RequestBody.create(null, new byte[0])) // Body none
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonData = new JSONObject(responseBody);
                        callback.onSuccess(jsonData);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new IOException("Unexpected code " + response));
                }
            }
        });
    }

    public void createDocument(String customerId, JSONObject requestBody, ApiCallback callback) {
        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/customers/" + customerId + "/document")
                    .put(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonData = new JSONObject(responseBody);
                        callback.onSuccess(jsonData);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void createDocumentFrontPage(String customerId, JSONObject requestBody, ApiCallback callback) {
        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/customers/" + customerId + "/document/pages")
                    .put(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonData = new JSONObject(responseBody);
                        callback.onSuccess(jsonData);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void getCustomer(String customerId, ApiCallback callback) {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/customers/" + customerId)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                    .build();

            executeRequest(request, callback);
    }

    private void executeRequest(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new IOException("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.body().string());
                    }
                    String responseBody = response.body().string();
                    JSONObject jsonData = new JSONObject(responseBody);
                    callback.onSuccess(jsonData);
                } catch (Exception e) {
                    callback.onFailure(e);
                } finally {
                    response.close();
                }
            }
        });
    }
    public void getDocumentPortrait(String customerId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/customers/" + customerId + "/document/portrait")
                .get()
                .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new IOException("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.body().string());
                    }
                    String responseBody = response.body().string();
                    JSONObject jsonData = new JSONObject(responseBody);
                    callback.onSuccess(jsonData);
                } catch (Exception e) {
                    callback.onFailure(e);
                } finally {
                    response.close();
                }
            }
        });
    }

    public void getQualityDocumentFrontPage(String customerId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/customers/" + customerId + "/document/pages/front/quality")
                .get()
                .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new IOException("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.body().string());
                    }
                    String responseBody = response.body().string();
                    JSONObject jsonData = new JSONObject(responseBody);
                    callback.onSuccess(jsonData);
                } catch (Exception e) {
                    callback.onFailure(e);
                } finally {
                    response.close();
                }
            }
        });
    }

    public void provideCustomerSelfie(String customerId, JSONObject requestBody, ApiCallback callback) {
        final MediaType mediaTypeJson = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(requestBody.toString(), mediaTypeJson);

        Request request = new Request.Builder()
                .url(BASE_URL + "/customers/" + customerId + "/selfie")
                .put(body)
                .addHeader("Authorization", BEARER_TOKEN) // Idéalement passer le token en paramètre
                .addHeader("Content-Type", "application/json") // Bonne pratique même si OkHttp le fait déjà
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new IOException("Erreur réseau : " + e.getMessage(), e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.body() == null) {
                        callback.onFailure(new IOException("Réponse vide du serveur"));
                        return;
                    }

                    String bodyString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(bodyString);

                    if (response.isSuccessful()) {
                        callback.onSuccess(jsonResponse);
                    } else {
                        String errorMsg = "Erreur API (HTTP " + response.code() + "): " + jsonResponse.toString();
                        callback.onFailure(new Exception(errorMsg));
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                } finally {
                    response.close(); // Toujours fermer la réponse pour libérer les ressources
                }
            }
        });
    }

    public void inspectCustomerDisclose(String customerId, ApiCallback callback) {
        Request request = new Request.Builder()

                .url(BASE_URL + "/customers/" + customerId + "/inspect/disclose")
                .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                .post(RequestBody.create(null, new byte[0])) // Body none
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonData = new JSONObject(responseBody);

                        // --- Extraction du score de similarité ---
                        double similarityScore = -1.0; // Valeur par défaut si non trouvée
                        if (jsonData.has("selfieInspection")) {
                            JSONObject selfieInspection = jsonData.getJSONObject("selfieInspection");
                            if (selfieInspection.has("similarityWith")) {
                                JSONObject similarityWith = selfieInspection.getJSONObject("similarityWith");
                                if (similarityWith.has("documentPortrait")) {
                                    JSONObject documentPortrait = similarityWith.getJSONObject("documentPortrait");
                                    if (documentPortrait.has("score")) {
                                        similarityScore = documentPortrait.getDouble("score");
                                    }
                                }
                            }
                        }

                        JSONObject result = new JSONObject();
                        result.put("similarityScore", similarityScore); // Ajoutez le score ici
                        result.put("fullResponse", jsonData); // Optionnel: garder la réponse complète

                        callback.onSuccess(result); // Passez le JSONObject contenant le score

                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new IOException("Unexpected code " + response.code() + " - " + response.message()));
                }
            }
        });
    }
    public void getDocumentFrontImage(String customerId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/customers/" + customerId + "/document/pages/front?width=600")
                .get()
                .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new IOException("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.body().string());
                    }
                    String responseBody = response.body().string();
                    JSONObject jsonData = new JSONObject(responseBody);
                    callback.onSuccess(jsonData);
                } catch (Exception e) {
                    callback.onFailure(e);
                } finally {
                    response.close();
                }
            }
        });
    }

    public void uploadDocument(String customerId, String base64Image, ApiCallback callback) {
        try {
            // Créer le corps JSON de la requête
            JSONObject requestBody = new JSONObject();
            JSONObject imageObject = new JSONObject();
            imageObject.put("data", base64Image);
            requestBody.put("image", imageObject);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // Construire la requête
            Request request = new Request.Builder()
                    .url(BASE_URL + "/customers/" + customerId + "/document/pages")
                    .put(body)
                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Exécuter la requête de manière asynchrone
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onFailure(new Exception("Upload failed: " + response.code() + " - " + response.message()));
                        }
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void getFrontDocument(String customerId, int width, ApiCallback callback) {
        try {
            String fullUrl = BASE_URL + "/customers/" + customerId + "/document/pages/front?width=" + width;

            // Log de l'URL et des infos
            Log.d("API_DEBUG", "GET " + fullUrl);
            Log.d("API_DEBUG", "Authorization: Bearer " + BEARER_TOKEN);

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .get()
                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("API_DEBUG", "Request failed", e);
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("API_DEBUG", "HTTP " + response.code() + " - " + response.message());

                    try {
                        String responseData = response.body().string();
                        Log.d("API_DEBUG", "Response body: " + responseData);

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onFailure(new Exception("Request failed: " + response.code() + " - " + response.message()));
                        }
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void inspectDocument(String customerId, ApiCallback callback) {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/customers/" + customerId + "/document/inspect")
                    .post(RequestBody.create(null, new byte[0]))  // Corps vide
                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonData = new JSONObject(responseBody);
                        callback.onSuccess(jsonData);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(Exception e);
    }
}
