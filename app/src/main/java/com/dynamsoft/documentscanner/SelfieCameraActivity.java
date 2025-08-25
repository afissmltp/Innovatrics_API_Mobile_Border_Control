package com.dynamsoft.documentscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;


public class SelfieCameraActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;

    private String customerId;
    private String parentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_capture);

        customerId = getIntent().getStringExtra("customerId");
        parentActivity = getIntent().getStringExtra("parentActivity");

        ImageButton btnClose = findViewById(R.id.btnClose);
        Button btnLaunchCamera = findViewById(R.id.btnLaunchCamera);

        // Fermer l'activité et retourner vers l'activité parent
        btnClose.setOnClickListener(v -> navigateBack());

        // Lancer caméra
        btnLaunchCamera.setOnClickListener(v -> checkCameraPermission());


        ImageView imageView = findViewById(R.id.imageViewPortrait);
        Glide.with(this) // 'this' peut être Activity ou Fragment
                .asGif()    // indique que c'est un GIF animé
                .load(R.drawable.portrait) // ton GIF dans res/drawable
                .into(imageView);
    }

    private void navigateBack() {
        setResult(RESULT_CANCELED); // Aucun selfie retourné
        finish(); // Fermer SelfieCameraActivity
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap selfieBitmap = (Bitmap) extras.get("data");

            if (selfieBitmap != null) {
                // Retourner le selfie à l'activité parent
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selfieBitmap", selfieBitmap);
                setResult(RESULT_OK, resultIntent);
            }
            finish(); // Fermer SelfieCameraActivity
        }
    }
}
