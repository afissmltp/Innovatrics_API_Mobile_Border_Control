package com.dynamsoft.documentscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NFCcameraActivity extends AppCompatActivity {
    private static final String TAG = "NFCcameraActivity";

    private PreviewView previewView;
    private Button captureButton;
    private ProgressBar progressBar; // Added ProgressBar
    private TextView loadingText; // Added TextView for loading message
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    private String customerId;
    private CustomerService customerService;
    // public static Bitmap customerSelfieBitmap; // Removed static, pass via Intent if needed, or upload directly

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_camera);

        customerService = new CustomerService();

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        progressBar = findViewById(R.id.progressBar); // Initialize ProgressBar
        loadingText = findViewById(R.id.loadingText); // Initialize TextView
        cameraExecutor = Executors.newSingleThreadExecutor();
        customerId = getIntent().getStringExtra("customerId");

        // Hide progress bar and text initially
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

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
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()) // Improve rotation handling
                        .build();

                // --- Improved Camera Selector ---
                CameraSelector cameraSelector = null;
                try {
                    // Try to get the back camera first
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    // Check if a camera matching this selector exists
                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        Log.w(TAG, "Back camera not found or not available, trying front camera.");
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA; // Fallback to front camera
                        if (!cameraProvider.hasCamera(cameraSelector)) {
                            Log.e(TAG, "Neither back nor front camera found or available.");
                            throw new IllegalStateException("No available camera found on this device.");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error selecting camera: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(NFCcameraActivity.this,
                                "Erreur de sélection de la caméra: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return; // Exit if camera selection fails
                }
                // --- END Improved Camera Selector ---

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException | IllegalStateException e) {
                Log.e(TAG, "Erreur de configuration de la caméra", e);
                runOnUiThread(() -> {
                    Toast.makeText(NFCcameraActivity.this,
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

        // Show loading indicators
        previewView.setVisibility(View.GONE); // Hide camera preview
        captureButton.setVisibility(View.GONE); // Hide capture button
        progressBar.setVisibility(View.VISIBLE);
        loadingText.setText("Envoi du document...");
        loadingText.setVisibility(View.VISIBLE);


        File photoFile = new File(getCacheDir(), "Document_nfc_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Execute compression and upload on a background thread
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                File compressedPhotoFile = compressImage(photoFile); // Compress the image
                                uploadDocFrontToServer(compressedPhotoFile);
                            } catch (IOException e) {
                                Log.e(TAG, "Erreur lors de la compression de l'image: " + e.getMessage(), e);
                                runOnUiThread(() -> {
                                    Toast.makeText(NFCcameraActivity.this,
                                            "Erreur de traitement de l'image.",
                                            Toast.LENGTH_SHORT).show();
                                    resetUIForCamera(); // Show camera again on error
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Erreur de capture: " + exception.getMessage(), exception);
                        runOnUiThread(() -> {
                            Toast.makeText(NFCcameraActivity.this,
                                    "Erreur de capture: " + exception.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            resetUIForCamera(); // Show camera again on error
                        });
                    }
                });
    }

    // Example of how to downscale a bitmap before compression
    private File compressImage(File originalFile) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Just get dimensions, don't load into memory yet
        BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);

        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;

        // Define a max desired size (e.g., 1920px on the longest side)
        int maxDimension = 1920;
        int inSampleSize = 1;

        if (imageHeight > maxDimension || imageWidth > maxDimension) {
            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false; // Now decode the bitmap
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);

        if (bitmap == null) {
            throw new IOException("Failed to decode image file to bitmap.");
        }

        // --- Optional: Further scale if inSampleSize wasn't precise enough ---
        // This ensures the image is exactly within maxDimension if needed
        float ratio = Math.min((float) maxDimension / bitmap.getWidth(), (float) maxDimension / bitmap.getHeight());
        if (ratio < 1.0f) { // Only scale down if larger than maxDimension
            int newWidth = Math.round(ratio * bitmap.getWidth());
            int newHeight = Math.round(ratio * bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        // --- End Optional Scaling ---


        File compressedFile = new File(getCacheDir(), "compressed_doc_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos); // Keep quality at 50 or adjust
            Log.d(TAG, "Image compressed and scaled from " + originalFile.length() / 1024 + "KB to " + compressedFile.length() / 1024 + "KB (original dimensions: " + imageWidth + "x" + imageHeight + ", new dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
            return compressedFile;
        } finally {
            if (fos != null) {
                fos.close();
            }
            bitmap.recycle(); // Important to free up memory
        }
    }


    private void uploadDocFrontToServer(File photoFile) {
        // This method is already running on a background thread (from takePhoto's onImageSaved)
        try {
            if (!photoFile.exists() || photoFile.length() == 0) {
                throw new IOException("Le fichier est vide ou n'existe pas après compression.");
            }

            String base64Image = encodeImageToBase64(photoFile);

            JSONObject imageObject = new JSONObject();
            imageObject.put("data", base64Image);

            JSONObject requestBody = new JSONObject();
            requestBody.put("image", imageObject);

            if (customerId == null || customerId.isEmpty()) {
                Log.e(TAG, "customerId est null ou vide, impossible d'envoyer le doc.");
                runOnUiThread(() -> {
                    Toast.makeText(NFCcameraActivity.this, "Erreur: ID client manquant.", Toast.LENGTH_LONG).show();
                    resetUIForCamera();
                });
                return;
            }

            customerService.createDocumentFrontPage(customerId, requestBody, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Log.d(TAG, "doc front envoyé avec succès: " + response.toString());
                    runOnUiThread(() -> {
                        Toast.makeText(NFCcameraActivity.this, "Document envoyé avec succès !", Toast.LENGTH_SHORT).show();
                        navigateToReadNFCActivity(customerId);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Échec de l'envoi du doc: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(NFCcameraActivity.this,
                                "Échec de l'envoi du document: " + e.getLocalizedMessage(), // Use getLocalizedMessage for user-friendly errors
                                Toast.LENGTH_LONG).show();
                        resetUIForCamera(); // Show camera again on error
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi: " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(NFCcameraActivity.this,
                        "Erreur interne lors de l'envoi: " + e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
                resetUIForCamera(); // Show camera again on error
            });
        } finally {
            // It's a good practice to delete the temporary file after upload or error
            if (photoFile.exists()) {
                photoFile.delete();
            }
        }
    }

    private void navigateToReadNFCActivity(String customerId) {
        Intent intent = new Intent(NFCcameraActivity.this, ReadNFCActivity.class);
        intent.putExtra("customerId", customerId);
        startActivity(intent);
        finish(); // Finish NFCcameraActivity
    }

    private String encodeImageToBase64(File imageFile) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(imageFile);
            outputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096]; // Increased buffer size for potentially faster reading
            int length;
            while ((length = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, length);
            }
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } finally {
            // Close streams in a robust way
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing input stream", e); }
            }
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing output stream", e); }
            }
        }
    }

    // New method to reset UI state after upload attempt (success or failure)
    private void resetUIForCamera() {
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}