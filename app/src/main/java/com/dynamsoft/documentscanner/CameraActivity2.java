package com.dynamsoft.documentscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import java.io.File;
import android.os.Environment;
import androidx.core.content.FileProvider;
import android.net.Uri;


public class CameraActivity2 extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 100;
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
                        Intent intent = new Intent(CameraActivity2.this, DisplayActivity.class);
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
    }
}