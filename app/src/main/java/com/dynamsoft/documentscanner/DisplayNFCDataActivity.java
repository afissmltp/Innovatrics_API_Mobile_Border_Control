package com.dynamsoft.documentscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DisplayNFCDataActivity extends AppCompatActivity {

    private TextView tvResult;
    private ImageView ivPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_nfc_data);

        tvResult = findViewById(R.id.text_result);
        ivPhoto = findViewById(R.id.view_photo);

        // Récupérer les données passées par Intent
        String resultText = getIntent().getStringExtra("resultText");
        byte[] faceImageBytes = getIntent().getByteArrayExtra("faceImage");

        if (resultText != null) {
            tvResult.setText(resultText);
        }

        if (faceImageBytes != null) {
            Bitmap faceImage = BitmapFactory.decodeByteArray(faceImageBytes, 0, faceImageBytes.length);
            ivPhoto.setImageBitmap(faceImage);
        }
    }
}
