package com.dynamsoft.documentscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;

import org.json.JSONObject;

import androidx.core.content.ContextCompat;
import android.content.Intent;

import android.widget.TextView;
import org.json.JSONException;

public class DisplayActivity extends AppCompatActivity {

    private ImageView documentImageView;
    private ProgressBar progressBar;
    private Button readDocBtn;
    private Button rotateButton;
    private TextView qualityStatus, sharpnessValue, brightnessValue, hotspotsValue;
    private CustomerService customerService;
    private String customerId;
    private int rotation = 0;
    private Bitmap displayedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        documentImageView = findViewById(R.id.documentImageView);
        progressBar = findViewById(R.id.progressBar);
        readDocBtn = findViewById(R.id.readDocBtn);
        rotateButton = findViewById(R.id.rotateButton);
        ImageButton nfcButton = findViewById(R.id.readNfcBtn);

        qualityStatus = findViewById(R.id.qualityStatus);
        sharpnessValue = findViewById(R.id.sharpnessValue);
        brightnessValue = findViewById(R.id.brightnessValue);
        hotspotsValue = findViewById(R.id.hotspotsValue);

        readDocBtn.setEnabled(false);
        rotateButton.setEnabled(false);

        customerService = new CustomerService();
        customerId = getIntent().getStringExtra("customerId");
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Customer ID manquant", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rotateButton.setOnClickListener(v -> {
            rotation += 90;
            if (rotation == 360) rotation = 0;
            documentImageView.setRotation(rotation);
        });

        readDocBtn.setOnClickListener(v -> {
            if (customerId != null && !customerId.isEmpty()) {
                Intent intent = new Intent(DisplayActivity.this, CustomerDataActivity.class);
                intent.putExtra("customerId", customerId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Customer ID manquant", Toast.LENGTH_SHORT).show();
            }
        });

        // Cacher l'image initialement et montrer le progress bar
        documentImageView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        loadDocumentImage();

        nfcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DisplayActivity.this, ReadNFCActivity.class);

                intent.putExtra("customerId", customerId);

                startActivityForResult(intent, 1001); // Utilisez startActivityForResult
                // Animation de transition (optionnelle)
                //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Rediriger vers CustomerDataActivity avec les données NFC
            Intent customerDataIntent = new Intent(this, CustomerDataActivity.class);
            customerDataIntent.putExtra("customerId", customerId);
            customerDataIntent.putExtra("nfcData", data.getSerializableExtra("nfcData"));
            startActivity(customerDataIntent);
        }
    }
    private void loadDocumentImage() {
        showProgress();
        customerService.getFrontDocument(customerId, 600, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        String base64Image = response.getString("data");
                        displayedBitmap = decodeBase64ToBitmap(base64Image);
                        documentImageView.setImageBitmap(displayedBitmap);
                        documentImageView.setVisibility(View.VISIBLE);
                        hideProgress();

                        // Après affichage, récupérer la qualité
                        fetchDocQuality();

                    } catch (Exception e) {
                        Log.e("DisplayActivity", "Erreur de décodage", e);
                        showError("Format d'image invalide");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e("DisplayActivity", "Échec du chargement", e);
                    showError("Échec du chargement: " + e.getMessage());
                });
            }
        });
    }

    private void fetchDocQuality() {
        showProgress();
        customerService.getQualityDocumentFrontPage(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject jsonData) {
                runOnUiThread(() -> {
                    hideProgress();
                    try {
                        boolean isFine = jsonData.getBoolean("fine");
                        JSONObject details = jsonData.getJSONObject("details");

                        JSONObject sharpness = details.getJSONObject("sharpness");
                        double sharpnessScore = sharpness.getDouble("score");
                        String sharpnessLevel = sharpness.getString("level");

                        JSONObject brightness = details.getJSONObject("brightness");
                        double brightnessScore = brightness.getDouble("score");
                        String brightnessLevel = brightness.getString("level");

                        JSONObject hotspots = details.getJSONObject("hotspots");
                        double hotspotsScore = hotspots.getDouble("score");
                        String hotspotsLevel = hotspots.getString("level");

                        updateQualityUI(isFine, sharpnessScore, sharpnessLevel,
                                brightnessScore, brightnessLevel,
                                hotspotsScore, hotspotsLevel);

                        readDocBtn.setEnabled(true);
                        rotateButton.setEnabled(true);

                    } catch (JSONException e) {
                        Log.e("QualityError", "Erreur parsing JSON", e);
                        showError("Erreur parsing qualité document");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    hideProgress();
                    showError("Erreur récupération qualité: " + e.getMessage());
                });
            }
        });
    }

    private void updateQualityUI(boolean isFine,
                                 double sharpnessScore, String sharpnessLevel,
                                 double brightnessScore, String brightnessLevel,
                                 double hotspotsScore, String hotspotsLevel) {
        qualityStatus.setText(isFine ? "✅ Document quality is good" : "⚠️ Document quality issues detected");
        qualityStatus.setTextColor(ContextCompat.getColor(this, isFine ? R.color.green : R.color.orange));

        sharpnessValue.setText(String.format("%.2f (%s)", sharpnessScore, sharpnessLevel));
        applyQualityLevelStyle(sharpnessValue, sharpnessLevel);

        brightnessValue.setText(String.format("%.2f (%s)", brightnessScore, brightnessLevel));
        applyQualityLevelStyle(brightnessValue, brightnessLevel);

        hotspotsValue.setText(String.format("%.2f (%s)", hotspotsScore, hotspotsLevel));
        applyQualityLevelStyle(hotspotsValue, hotspotsLevel);
    }

    private void applyQualityLevelStyle(TextView view, String level) {
        int colorRes;
        switch (level) {
            case "HIGH":
                colorRes = R.color.green;
                break;
            case "MEDIUM":
                colorRes = R.color.orange;
                break;
            case "LOW":
                colorRes = R.color.red;
                break;
            default:
                colorRes = R.color.black;
        }
        view.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private Bitmap decodeBase64ToBitmap(String base64Image) {
        try {
            String base64Data = base64Image.contains(",") ? base64Image.split(",")[1] : base64Image;
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            Log.e("DisplayActivity", "Base64 invalide", e);
            throw e;
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(DisplayActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showProgress() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            if (documentImageView != null) documentImageView.setAlpha(0.5f);
            readDocBtn.setEnabled(false);
            rotateButton.setEnabled(false);
        });
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            if (documentImageView != null) documentImageView.setAlpha(1f);
            readDocBtn.setEnabled(true);
            rotateButton.setEnabled(true);
        });
    }
}
