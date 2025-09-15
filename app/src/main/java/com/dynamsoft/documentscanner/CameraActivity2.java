package com.dynamsoft.documentscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import android.os.Environment;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
public class CameraActivity2 extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;

    private CustomerService customerService;
    private String customerId;
    private ImageCapture imageCapture;
    private File photoFile; // Garder une référence au fichier photo

    private PreviewView previewView;
    private ImageView previewImageView; // Nouvelle ImageView pour prévisualisation
    private LinearLayout confirmationLayout; // Layout avec boutons OK/Réessayer
    private ImageView docUpload;
    private TextView uploadText;
    private ProgressBar progressBar;
    private FloatingActionButton captureButton;
    private Button retryButton, confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_upload);

        customerService = new CustomerService();
        customerId = getIntent().getStringExtra("customerId");

        // Views
        previewView = findViewById(R.id.previewView);
        previewImageView = findViewById(R.id.previewImageView);
        confirmationLayout = findViewById(R.id.confirmationLayout);
        docUpload = findViewById(R.id.doc_upload);
        uploadText = findViewById(R.id.upload_text);
        progressBar = findViewById(R.id.progressBar);
        captureButton = findViewById(R.id.captureButton);
        retryButton = findViewById(R.id.retryButton);
        confirmButton = findViewById(R.id.confirmButton);

        // Au départ, masquer ImageView, texte et ProgressBar
        previewImageView.setVisibility(View.GONE);
        confirmationLayout.setVisibility(View.GONE);
        docUpload.setVisibility(View.GONE);
        uploadText.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Charger le GIF pour l'upload (mais ne sera affiché qu'après confirmation)
        Glide.with(this)
                .asGif()
                .load(R.drawable.telecharger)
                .into(docUpload);

        // Permission caméra
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }

        // Bouton capture
        captureButton.setOnClickListener(v -> takePhoto());

        // Bouton réessayer
        retryButton.setOnClickListener(v -> retryCapture());

        // Bouton confirmer
        confirmButton.setOnClickListener(v -> confirmAndUpload());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
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

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "document_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Masquer caméra et bouton de capture
                        previewView.setVisibility(View.GONE);
                        captureButton.setVisibility(View.GONE);

                        // Afficher la prévisualisation et les boutons de confirmation
                        previewImageView.setVisibility(View.VISIBLE);
                        confirmationLayout.setVisibility(View.VISIBLE);

                        // Charger l'image capturée dans l'ImageView de prévisualisation
                        Glide.with(CameraActivity2.this)
                                .load(photoFile)
                                .into(previewImageView);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity2.this,
                                "Erreur capture : " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void retryCapture() {
        // Cacher la prévisualisation et les boutons de confirmation
        previewImageView.setVisibility(View.GONE);
        confirmationLayout.setVisibility(View.GONE);

        // Réafficher la caméra et le bouton de capture
        previewView.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
    }

    private void confirmAndUpload() {
        // Cacher la prévisualisation et les boutons de confirmation
        previewImageView.setVisibility(View.GONE);
        confirmationLayout.setVisibility(View.GONE);

        // Afficher GIF loader et ProgressBar
        docUpload.setVisibility(View.VISIBLE);
        uploadText.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        // Charger le GIF pour l'upload
        Glide.with(CameraActivity2.this)
                .asGif()
                .load(R.drawable.telecharger)
                .into(docUpload);

        try {
            // Lire le fichier photo
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

            // Redimensionner pour Base64
            Bitmap resized = resizeBitmap(bitmap, 1080, 1080);
            String base64Image = convertBitmapToBase64(resized);

            // Envoyer l'image
            uploadDocument(base64Image);

        } catch (Exception e) {
            Toast.makeText(CameraActivity2.this, "Erreur traitement image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // En cas d'erreur, revenir à l'écran de capture
            retryCapture();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void uploadDocument(String base64Image) {
        customerService.uploadDocument(customerId, base64Image, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    Log.d("API_RESPONSE", response.toString());
                    Toast.makeText(CameraActivity2.this, "Traitement réussi", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CameraActivity2.this, CustomerDataActivity.class)
                            .putExtra("customerId", customerId));
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity2.this, "Échec upload: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
}