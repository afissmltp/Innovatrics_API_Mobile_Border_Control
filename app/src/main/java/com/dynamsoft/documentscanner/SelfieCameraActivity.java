package com.dynamsoft.documentscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SelfieCameraActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private String customerId;
    private String parentActivity;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private LinearLayout textContainer;
    private MaterialButton btnLaunchCamera;
    private ImageButton btnClose;
    private FloatingActionButton btnCapture;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);

        customerId = getIntent().getStringExtra("customerId");
        parentActivity = getIntent().getStringExtra("parentActivity");

        previewView = findViewById(R.id.previewView);
        textContainer = findViewById(R.id.text_container);
        btnLaunchCamera = findViewById(R.id.btnLaunchCamera);
        btnClose = findViewById(R.id.btnClose);
        btnCapture = findViewById(R.id.btnCapture);

        // Initialement cacher la preview et le bouton capture
        previewView.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);

        // Fermer l'activité
        btnClose.setOnClickListener(v -> navigateBack());

        // Lancer caméra
        btnLaunchCamera.setOnClickListener(v -> checkCameraPermission());

        // Bouton de capture
        btnCapture.setOnClickListener(v -> takePicture());

        ImageView imageView = findViewById(R.id.imageViewPortrait);
        Glide.with(this)
                .asGif()
                .load(R.drawable.portrait)
                .into(imageView);
    }

    private void navigateBack() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        // Cacher l'interface initiale et montrer la preview + bouton capture
        textContainer.setVisibility(View.GONE);
        btnLaunchCamera.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        btnCapture.setVisibility(View.VISIBLE);

        // Configurer CameraX
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Configuration de la preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Configuration de la capture d'image
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Sélectionner la caméra arrière
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Désactiver les use cases existants et lancer la caméra
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Désactiver le bouton pendant la capture
        btnCapture.setEnabled(false);

        // Créer le fichier de sortie
        File photoFile = createImageFile();
        if (photoFile == null) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            btnCapture.setEnabled(true);
            return;
        }

        // Sauvegarder le chemin du fichier pour l'utiliser plus tard
        String imagePath = photoFile.getAbsolutePath();

        // Options de sortie
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Prendre la photo
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Retourner le chemin du fichier au lieu du Bitmap
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("imagePath", imagePath);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(SelfieCameraActivity.this,
                                "Error taking picture: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnCapture.setEnabled(true);
                    }
                });
    }
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les ressources CameraX
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}