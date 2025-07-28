package com.dynamsoft.documentscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceCameraActivity extends AppCompatActivity {
    private static final String TAG = "FaceCameraActivity";

    private PreviewView previewView;
    private Button captureButton;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    private String customerId;
    private CustomerService customerService;
    public static Bitmap customerSelfieBitmap; // Consider if this is truly needed as static

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_camera);
        customerService = new CustomerService();

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        cameraExecutor = Executors.newSingleThreadExecutor();
        customerId = getIntent().getStringExtra("customerId");

        startCamera();
        captureButton.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Erreur de configuration de la caméra", e);
                runOnUiThread(() -> {
                    Toast.makeText(FaceCameraActivity.this,
                            "Erreur de configuration de la caméra: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "La caméra n'est pas prête.", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = new File(getCacheDir(), "selfie_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // IMMEDIATELY NAVIGATE TO THE NEXT ACTIVITY
                        navigateToCustomerDataActivity(photoFile.getAbsolutePath(), customerId);

                        // THEN, UPLOAD THE SELFIE TO THE SERVER IN THE BACKGROUND
                        uploadSelfieToServer(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Erreur de capture: " + exception.getMessage(), exception);
                        runOnUiThread(() -> {
                            Toast.makeText(FaceCameraActivity.this,
                                    "Erreur de capture: " + exception.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
    }

    // Renamed for clarity: this method only handles the upload
    private void uploadSelfieToServer(File photoFile) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String base64Image = null;
            try {
                if (!photoFile.exists() || photoFile.length() == 0) {
                    throw new IOException("Le fichier selfie est vide ou n'existe pas.");
                }

                base64Image = encodeImageToBase64(photoFile);

                JSONObject imageObject = new JSONObject();
                imageObject.put("data", base64Image);

                JSONObject requestBody = new JSONObject();
                requestBody.put("image", imageObject);

                if (customerId == null || customerId.isEmpty()) {
                    Log.e(TAG, "customerId est null ou vide, impossible d'envoyer le selfie.");
                    // You might want to log this or handle it, but don't Toast as the activity is gone.
                    return; // Stop processing if customerId is missing
                }

                customerService.provideCustomerSelfie(customerId, requestBody, new CustomerService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        // This Toast won't be visible if FaceCameraActivity is already finished
                        // You might want to use a different notification mechanism (e.g., a background service notification)
                        Log.d(TAG, "Selfie enregistré avec succès sur le serveur. Réponse: " + response.toString());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // This Toast won't be visible either
                        Log.e(TAG, "Échec de l'envoi du selfie au serveur: " + e.getMessage(), e);
                    }
                });

            } catch (Exception e) {
                String errorMessage = "Erreur lors de l'envoi du selfie en arrière-plan: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
            }
            // IMPORTANT: If you remove the finally block from here,
            // ensure CustomerDataActivity handles the file deletion.
            // If CustomerDataActivity cannot read it (e.g., if it crashes),
            // this file might persist. Consider a robust cleanup strategy.
        });
    }

    private void navigateToCustomerDataActivity(String selfieImagePath, String customerId) {
        Intent intent = new Intent(FaceCameraActivity.this, CustomerDataActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("selfieImagePath", selfieImagePath);
        startActivity(intent);
        finish(); // Finish FaceCameraActivity
    }

    private String encodeImageToBase664(File imageFile) throws IOException { // Corrected method name to Base64
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(imageFile);
            outputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int length;
            while ((length = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, length);
            }
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }
    private String encodeImageToBase64(File imageFile) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(imageFile);
            outputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int length;
            while ((length = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, length);
            }
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}