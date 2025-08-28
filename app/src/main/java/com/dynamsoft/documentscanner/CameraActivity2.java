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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutionException;
public class CameraActivity2 extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;

    private CustomerService customerService;
    private String customerId;
    private ImageCapture imageCapture;

    private PreviewView previewView;
    private ImageView docUpload;
    private TextView uploadText;
    private ProgressBar progressBar;
    private Button captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_upload);

        customerService = new CustomerService();
        customerId = getIntent().getStringExtra("customerId");

        // Views
        previewView = findViewById(R.id.previewView);
        docUpload = findViewById(R.id.doc_upload);
        uploadText = findViewById(R.id.upload_text);
        progressBar = findViewById(R.id.progressBar);
        captureButton = findViewById(R.id.captureButton);

        // Au départ, masquer ImageView, texte et ProgressBar
        docUpload.setVisibility(View.GONE);
        uploadText.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Charger le GIF pour l'upload (mais ne sera affiché qu'après capture)
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
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "document_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                        // Masquer caméra et bouton
                        previewView.setVisibility(View.GONE);
                        captureButton.setVisibility(View.GONE);

                        // Afficher GIF loader et ProgressBar
                        docUpload.setVisibility(View.VISIBLE);
                        uploadText.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);

                        // Charger le GIF pour l’upload
                        Glide.with(CameraActivity2.this)
                                .asGif()
                                .load(R.drawable.telecharger)
                                .into(docUpload);

                        try {
                            // Lire le fichier photo mais ne pas l’afficher
                            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                            // Redimensionner pour Base64
                            Bitmap resized = resizeBitmap(bitmap, 1080, 1080);
                            String base64Image = convertBitmapToBase64(resized);

                            // Envoyer l’image
                            uploadDocument(base64Image);

                        } catch (Exception e) {
                            Toast.makeText(CameraActivity2.this, "Erreur traitement image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity2.this,
                                "Erreur capture : " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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
                    Toast.makeText(CameraActivity2.this, "Upload réussi", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CameraActivity2.this, Display2Activity.class)
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
  /*  private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private CustomerService customerService;
    private String customerId ;
    private Uri photoUri; // Uri vers le fichier temporaire


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_upload); // Vous devrez créer ce layout

        customerService = new CustomerService();
        customerId = getIntent().getStringExtra("customerId");

        // Vérifier et demander la permission de la caméra
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }

        ImageView imageView = findViewById(R.id.doc_upload);
        Glide.with(this) // 'this' peut être Activity ou Fragment
                .asGif()    // indique que c'est un GIF animé
                .load(R.drawable.telecharger) // ton GIF dans res/drawable
                .into(imageView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "La permission de la caméra est nécessaire", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void openCamera() {
        try {
            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "document_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Aucune application caméra trouvée", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur ouverture caméra: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (photoUri != null) {
                try {
                    // Lire le fichier en pleine résolution
                    Bitmap fullPhoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);

                    // Redimensionner l'image pour accélérer l'upload
                    Bitmap resizedPhoto = resizeBitmap(fullPhoto, 1080, 1080);

                    // Convertir en base64
                    String base64Image = convertBitmapToBase64(resizedPhoto);

                    // Envoyer l'image
                    uploadDocument(base64Image);

                } catch (Exception e) {
                    Toast.makeText(this, "Erreur lecture image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } else {
            finish();
        }
    }

    // Redimensionne un bitmap en conservant les proportions
    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    // Convertit un bitmap en Base64 avec compression 80%
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
                    try {
                        Log.d("API_RESPONSE", response.toString());
                        Toast.makeText(CameraActivity2.this, "Upload réussi", Toast.LENGTH_SHORT).show();

                        // Lancer l'activité d'affichage
                        Intent intent = new Intent(CameraActivity2.this, Display2Activity.class);
                        intent.putExtra("customerId", customerId);
                        startActivity(intent);

                        finish();
                    } catch (Exception e) {
                        Toast.makeText(CameraActivity2.this, "Erreur affichage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity2.this, "Échec upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }*/
}