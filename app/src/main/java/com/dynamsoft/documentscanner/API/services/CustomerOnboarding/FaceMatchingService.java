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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FaceMatchingService {
    private static final String TAG = "FaceMatchingService";
    private static final String BASE_URL = "https://8076175a5137.ngrok-free.app/api/v1";
    private static final String BEARER_TOKEN = "aW5rX2I5YWU5MjY5N2JlMjQ0Y2YwZmQ3NDNiNjMwMjE1ODVlOmluc19leUp0WlhSaFpHRjBZU0k2SUhzaVkyeHBaVzUwSWpvZ2V5SnBaQ0k2SUNJNVpUWmpOR1JpWWkxbU5qRm1MVFExWVRndFlUQTFaaTB3WkdVek1qUXlOamhpTm1FaUxDQWlibUZ0WlNJNklDSlRUVXhVVUNKOUxDQWliR2xqWlc1elpWOWpkWE4wYjIxZmNISnZjR1Z5ZEdsbGN5STZJSHNpTDJOdmJuUnlZV04wTDJSdmRDOWthWE12Wlc1aFlteGxaQ0k2SUNKMGNuVmxJaXdnSWk5amIyNTBjbUZqZEM5a2IzUXZaWFpoYkhWaGRHbHZiaUk2SUNKMGNuVmxJaXdnSWk5amIyNTBjbUZqZEM5a2IzUXZaR2x6TDJ4cFkyVnVjMlZXWlhKemFXOXVJam9nSWpNaUxDQWlMMk52Ym5SeVlXTjBMM050WVhKMFptRmpaVjlsYldKbFpHUmxaQzlsYm1GaWJHVWlPaUFpZEhKMVpTSXNJQ0l2WTI5dWRISmhZM1F2YzIxaGNuUm1ZV05sWDJWdFltVmtaR1ZrTDNCaGJHMGlPaUFpZEhKMVpTSXNJQ0l2WTI5dWRISmhZM1F2YzIxaGNuUm1ZV05sWDJWdFltVmtaR1ZrTDNCaGJHMWZiR2wyWlc1bGMzTWlPaUFpZEhKMVpTSjlMQ0FpWTNKbFlYUnBiMjVmZEdsdFpYTjBZVzF3SWpvZ0lqQTNMekV4THpJd01qVWdNRGs2TVRRNk1qQWdWVlJESWl3Z0luWmhiR2xrWDNSdklqb2dJakE1THpBNUx6SXdNalVnTURBNk1EQTZNREFnVlZSREluMHNJQ0p6YVdkdVlYUjFjbVVpT2lBaVNHbzFRVXB1Y0c4MFIyTjZMemhVVjJoNVdWQm9NVGc1U2xORlVqSklaV2xsV0hWblZqUlpPR1prVEU5UGRtOW5ZM3BDUVRsdWQzb3lSVGhaV0RGYUwxaEtaVXBQWjJ0T2IzWktSVFZpV21SbFZWcDJRbWM5UFNKOQ==";
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

    public interface ProbeCallback {
        void onProbeCreated(String probeFaceId);
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

    public void createProbeFace(Bitmap probeBitmap, ProbeCallback callback) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            probeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
                        String probeFaceId = response.optString("id", null);
                        if (probeFaceId == null) {
                            Log.e(TAG, "ID de face non reçu dans la réponse");
                        }
                        callback.onProbeCreated(probeFaceId);
                    },
                    error -> {
                        String errorMsg = error.getMessage();
                        if (error.networkResponse != null) {
                            errorMsg = "Code: " + error.networkResponse.statusCode;
                            try {
                                errorMsg += ", " + new String(error.networkResponse.data, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, "Erreur request: " + errorMsg);
                        callback.onProbeCreated(null);
                    });

            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(TAG, "Erreur création probe: " + e.getMessage());
            callback.onProbeCreated(null);
        }
    }

    public void createReferenceFace(Bitmap referenceBitmap, ProbeCallback callback) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            referenceBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
                        String referenceFaceId = response.optString("id", null);
                        callback.onProbeCreated(referenceFaceId);
                    },
                    error -> {
                        Log.e(TAG, "Erreur request reference: " + error.getMessage());
                        callback.onProbeCreated(null);
                    });

            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(TAG, "Erreur création reference: " + e.getMessage());
            callback.onProbeCreated(null);
        }
    }

    public void matchFaces(String probeFaceId, String referenceFaceId, MatchCallback callback) {
        try {
            JSONObject body = new JSONObject();
            // Format correct selon la documentation
            body.put("referenceFace", "/api/v1/faces/" + referenceFaceId);  // Format complet attendu

            JsonObjectRequest request = createAuthenticatedRequest(
                    Request.Method.POST,
                    BASE_URL + "/faces/" + probeFaceId + "/similarity",
                    body,
                    response -> {
                        try {
                            // Attendre un peu pour simuler le temps de traitement
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
                            }, 1000); // Délai d'1 seconde
                        } catch (Exception e) {
                            callback.onError("Erreur: " + e.getMessage());
                        }
                    },
                    error -> {
                        String errorMsg = "Erreur inconnue";
                        if (error.networkResponse != null) {
                            errorMsg = "Code: " + error.networkResponse.statusCode;
                            try {
                                String responseBody = new String(error.networkResponse.data, "UTF-8");
                                errorMsg += ", Réponse: " + responseBody;
                                Log.e(TAG, "Détails erreur: " + responseBody);

                                // Si erreur 400, on peut réessayer après un délai
                                if (error.networkResponse.statusCode == 400) {
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        matchFaces(probeFaceId, referenceFaceId, callback);
                                    }, 2000); // Réessai après 2 secondes
                                    return;
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        callback.onError(errorMsg);
                    });

            // Ajout de timeout plus long
            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000, // 15 secondes timeout
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            requestQueue.add(request);

        } catch (JSONException e) {
            callback.onError("Erreur création requête: " + e.getMessage());
        }
    }
}
