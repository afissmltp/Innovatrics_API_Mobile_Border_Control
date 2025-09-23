package com.dynamsoft.documentscanner.API.services.CustomerOnboarding;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FaceMatchingService {
    private static final String TAG = "FaceMatchingService";
    private static final String BASE_URL = "https://1487bd3339b0.ngrok-free.app/api/v1";
    private static final String BEARER_TOKEN = "aW5rXzI4YTlkYTI2MTI0ZTg2MzgxYzEyY2RlZmQ2NzVlMGIzOmluc19leUp0WlhSaFpHRjBZU0k2SUhzaVkyeHBaVzUwSWpvZ2V5SnBaQ0k2SUNJNVpUWmpOR1JpWWkxbU5qRm1MVFExWVRndFlUQTFaaTB3WkdVek1qUXlOamhpTm1FaUxDQWlibUZ0WlNJNklDSlRUVXhVVUNKOUxDQWliR2xqWlc1elpWOWpkWE4wYjIxZmNISnZjR1Z5ZEdsbGN5STZJSHNpTDJOdmJuUnlZV04wTDJSdmRDOWthWE12Wlc1aFlteGxaQ0k2SUNKMGNuVmxJaXdnSWk5amIyNTBjbUZqZEM5a2IzUXZaWFpoYkhWaGRHbHZiaUk2SUNKMGNuVmxJaXdnSWk5amIyNTBjbUZqZEM5a2IzUXZaR2x6TDJ4cFkyVnVjMlZXWlhKemFXOXVJam9nSWpNaUxDQWlMMk52Ym5SeVlXTjBMM050WVhKMFptRmpaVjlsYldKbFpHUmxaQzlsYm1GaWJHVWlPaUFpZEhKMVpTSXNJQ0l2WTI5dWRISmhZM1F2YzIxaGNuUm1ZV05sWDJWdFltVmtaR1ZrTDNCaGJHMGlPaUFpZEhKMVpTSXNJQ0l2WTI5dWRISmhZM1F2YzIxaGNuUm1ZV05sWDJWdFltVmtaR1ZrTDNCaGJHMWZiR2wyWlc1bGMzTWlPaUFpZEhKMVpTSjlMQ0FpWTNKbFlYUnBiMjVmZEdsdFpYTjBZVzF3SWpvZ0lqQTVMekE1THpJd01qVWdNRGs2TURnNk1USWdWVlJESWl3Z0luWmhiR2xrWDNSdklqb2dJakV4THpBNEx6SXdNalVnTURBNk1EQTZNREFnVlZSREluMHNJQ0p6YVdkdVlYUjFjbVVpT2lBaU1qZGtPSFUwWkdFMU1uQlhhbmhJZUc1VllUVnRabXB0U1ZoVE9FWlhiMFZ0YlhFeGJWRTViVzlSUVhZemJqQnVWRXRpTWs5RU1tVmFZa1E0Vmt4UmRreG5UVE5XSzI1bFZVdzBMMmg1Ym1SMmVXUkZRV2M5UFNKOQ==";
    private final RequestQueue requestQueue;
    private final Context context;

    public FaceMatchingService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface MatchCallback {
        void onSuccess(double score);

        void onError(String error);
    }

    public interface FaceCreationCallback {
        void onFaceCreated(String faceId);

        void onError(String error);
    }

    private JsonObjectRequest createAuthenticatedRequest(int method, String url, JSONObject body,
                                                         Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new JsonObjectRequest(method, url, body, listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + BEARER_TOKEN);
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
    }

    public void createProbeFace(Bitmap probeBitmap, FaceCreationCallback callback) {
        createFace(probeBitmap, "probe", callback);
    }

    public void createReferenceFace(Bitmap referenceBitmap, FaceCreationCallback callback) {
        createFace(referenceBitmap, "reference", callback);
    }

    private void createFace(Bitmap bitmap, String type, FaceCreationCallback callback) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Compression optimisée pour réduire la taille
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            JSONObject body = new JSONObject();
            JSONObject image = new JSONObject();
            image.put("data", base64);
            body.put("image", image);

            JSONObject detection = new JSONObject();
            detection.put("mode", "STRICT");
            JSONObject faceSizeRatio = new JSONObject();
            faceSizeRatio.put("min", 0.05);
            faceSizeRatio.put("max", 0.5);
            detection.put("faceSizeRatio", faceSizeRatio);
            body.put("detection", detection);

            JsonObjectRequest request = createAuthenticatedRequest(
                    Request.Method.POST,
                    BASE_URL + "/faces",
                    body,
                    response -> {
                        try {
                            String faceId = response.getString("id");
                            if (faceId != null && !faceId.isEmpty()) {
                                callback.onFaceCreated(faceId);
                            } else {
                                Log.e(TAG, "ID de face non reçu dans la réponse");
                                callback.onError("ID de face non reçu");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur parsing réponse création face: " + e.getMessage());
                            callback.onError("Erreur parsing réponse");
                        }
                    },
                    error -> {
                        String errorMsg = extractErrorMessage(error);
                        Log.e(TAG, "Erreur création " + type + " face: " + errorMsg);
                        callback.onError(errorMsg);
                    });

            // Configuration du timeout
            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(TAG, "Erreur création " + type + " face: " + e.getMessage());
            callback.onError("Erreur création requête: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Erreur compression image: " + e.getMessage());
            callback.onError("Erreur traitement image");
        }
    }

    private String extractErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                return "Code: " + error.networkResponse.statusCode + ", Réponse: " + responseBody;
            } catch (UnsupportedEncodingException e) {
                return "Code: " + error.networkResponse.statusCode + ", Erreur encoding";
            }
        }
        return error.getMessage() != null ? error.getMessage() : "Erreur inconnue";
    }

    public void matchFaces(String probeFaceId, String referenceFaceId, MatchCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("referenceFace", "/api/v1/faces/" + referenceFaceId);

            JsonObjectRequest request = createAuthenticatedRequest(
                    Request.Method.POST,
                    BASE_URL + "/faces/" + probeFaceId + "/similarity",
                    body,
                    response -> {
                        try {
                            if (response.has("score")) {
                                double score = response.getDouble("score");
                                callback.onSuccess(score);
                            } else {
                                Log.e(TAG, "Clé 'score' manquante dans la réponse: " + response.toString());
                                callback.onError("Format de réponse inattendu");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur parsing réponse: " + e.getMessage());
                            callback.onError("Erreur traitement réponse");
                        }
                    },
                    error -> {
                        String errorMsg = extractErrorMessage(error);
                        Log.e(TAG, "Erreur comparaison faciale: " + errorMsg);

                        // Réessai automatique pour les erreurs 400
                        if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                matchFaces(probeFaceId, referenceFaceId, callback);
                            }, 2000);
                        } else {
                            callback.onError(errorMsg);
                        }
                    });

            request.setRetryPolicy(new DefaultRetryPolicy(
                    20000, // 20 secondes timeout pour la comparaison
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            requestQueue.add(request);

        } catch (JSONException e) {
            callback.onError("Erreur création requête: " + e.getMessage());
        }
    }

}
