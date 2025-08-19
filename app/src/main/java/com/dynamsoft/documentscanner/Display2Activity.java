package com.dynamsoft.documentscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;

import org.json.JSONObject;

import java.util.Locale;

public class Display2Activity extends AppCompatActivity {

    private ImageView documentImageView;
    private ProgressBar progressBar;
    private Button readDocBtn, rotateButton;
    private TextView qualityStatus;
    private CustomerService customerService;
    private String customerId;
    private int rotation = 0;
    private Bitmap displayedBitmap;
    private boolean isDocumentInspected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display2);

        initializeViews();
        setupCustomerId();
        setupButtonListeners();
        loadDocumentImage();
    }

    private void initializeViews() {
        documentImageView = findViewById(R.id.documentImageView);
        progressBar = findViewById(R.id.progressBar);
        readDocBtn = findViewById(R.id.readDocBtn);
        rotateButton = findViewById(R.id.rotateButton);
        qualityStatus = findViewById(R.id.qualityStatus);

        readDocBtn.setEnabled(false);
        rotateButton.setEnabled(false);
        documentImageView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setupCustomerId() {
        customerId = getIntent().getStringExtra("customerId");
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Customer ID manquant", Toast.LENGTH_LONG).show();
            finish();
        }
        customerService = new CustomerService();
    }

    private void setupButtonListeners() {
        rotateButton.setOnClickListener(v -> rotateImage());

        readDocBtn.setOnClickListener(v -> {
            if (!isDocumentInspected) {
                Toast.makeText(this, "Inspection du document en cours...", Toast.LENGTH_SHORT).show();
                return;
            }
            navigateToCustomerData();
        });

        findViewById(R.id.readNfcBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReadNFCActivity.class);
            intent.putExtra("customerId", customerId);
            startActivityForResult(intent, 1001);
        });
    }

    private void rotateImage() {
        rotation = (rotation + 90) % 360;
        documentImageView.setRotation(rotation);
    }

    private void navigateToCustomerData() {
        if (customerId != null) {
            Intent intent = new Intent(this, CustomerDataActivity.class);
            intent.putExtra("customerId", customerId);
            startActivity(intent);
        }
    }

    private void loadDocumentImage() {
        showProgress();
        customerService.getFrontDocument(customerId, 600, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    String base64Image = response.getString("data");
                    displayedBitmap = decodeBase64ToBitmap(base64Image);
                    updateDocumentImage();
                    inspectDocument();
                } catch (Exception e) {
                    handleImageError("Format d'image invalide", e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                handleImageError("Échec du chargement", e);
            }
        });
    }

    private void updateDocumentImage() {
        runOnUiThread(() -> {
            documentImageView.setImageBitmap(displayedBitmap);
            documentImageView.setVisibility(View.VISIBLE);
            hideProgress();
        });
    }

    private void inspectDocument() {
        if (isDocumentInspected) return;

        showProgress();
        customerService.inspectDocument(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        isDocumentInspected = true;

                        // Construire le message détaillé
                        StringBuilder inspectionDetails = new StringBuilder();

                        // 1. Statut d'expiration
                        boolean expired = response.optBoolean("expired", true);
                        inspectionDetails.append("Expiration: ")
                                .append(expired ? "EXPIRÉ" : "VALIDE")
                                .append("\n\n");

                        // 2. Inspection MRZ
                        JSONObject mrzInspection = response.optJSONObject("mrzInspection");
                        if (mrzInspection != null) {
                            boolean mrzValid = mrzInspection.optBoolean("valid", false);
                            inspectionDetails.append("MRZ: ")
                                    .append(mrzValid ? "VALIDE" : "INVALIDE")
                                    .append("\n\n");
                        }

                        // 3. Inspection portrait
                        JSONObject portraitInspection = response.optJSONObject("portraitInspection");
                        if (portraitInspection != null) {
                            String genderEstimate = portraitInspection.optString("genderEstimate", "N/A");
                            JSONObject genderConsistency = portraitInspection.optJSONObject("genderConsistency");
                            boolean mrzConsistent = genderConsistency != null && genderConsistency.optBoolean("mrz", false);

                            inspectionDetails.append("Portrait:\n")
                                    .append("- Genre estimé: ").append(genderEstimate).append("\n")
                                    .append("- Cohérence MRZ: ").append(mrzConsistent ? "OK" : "INCOHERENT")
                                    .append("\n\n");
                        }

                        // 4. Zone visuelle
                        JSONObject visualZone = response.optJSONObject("visualZoneInspection");
                        if (visualZone != null) {
                            JSONObject ocrConfidence = visualZone.optJSONObject("ocrConfidence");
                            double confidence = ocrConfidence != null ? ocrConfidence.optDouble("confidence", 0) * 100 : 0;
                            boolean textConsistent = visualZone.optJSONObject("textConsistency") != null
                                    && visualZone.optJSONObject("textConsistency").optBoolean("consistent", false);

                            inspectionDetails.append("Zone visuelle:\n")
                                    .append("- Confiance OCR: ").append(String.format(Locale.getDefault(), "%.2f%%", confidence)).append("\n")
                                    .append("- Texte cohérent: ").append(textConsistent ? "OUI" : "NON")
                                    .append("\n\n");
                        }

                        // 5. Altération de page
                        JSONObject pageTampering = response.optJSONObject("pageTampering");
                        if (pageTampering != null) {
                            JSONObject front = pageTampering.optJSONObject("front");
                            if (front != null) {
                                boolean isScreenshot = front.optBoolean("looksLikeScreenshot", false);
                                boolean isPrintCopy = front.optBoolean("looksLikePrintCopy", false);

                                inspectionDetails.append("Authenticité:\n")
                                        .append("- Screenshot: ").append(isScreenshot ? "OUI" : "NON").append("\n")
                                        .append("- Copie imprimée: ").append(isPrintCopy ? "OUI" : "NON");
                            }
                        }

                        // Afficher le résultat
                        qualityStatus.setText(inspectionDetails.toString());
                        qualityStatus.setTextColor(ContextCompat.getColor(Display2Activity.this,
                                expired ? R.color.red : R.color.green));

                        enableControls();
                        hideProgress();

                    } catch (Exception e) {
                        handleInspectionError("Erreur de traitement", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                handleInspectionError("Échec de l'inspection", e);
            }
        });
    }
    private void handleInspectionError(String message, Exception e) {
        Log.e("DisplayActivity", message, e);
        runOnUiThread(() -> {
            hideProgress();
            qualityStatus.setText("Échec de l'inspection");
            qualityStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
            Toast.makeText(this, message + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void enableControls() {
        readDocBtn.setEnabled(true);
        rotateButton.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, CustomerDataActivity.class);
            intent.putExtra("customerId", customerId);
            intent.putExtra("nfcData", data.getSerializableExtra("nfcData"));
            startActivity(intent);
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64Image) {
        try {
            String base64Data = base64Image.contains(",") ? base64Image.split(",")[1] : base64Image;
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de décodage d'image", e);
        }
    }

    private void showProgress() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            documentImageView.setAlpha(0.5f);
        });
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            documentImageView.setAlpha(1f);
        });
    }

    private void handleImageError(String message, Exception e) {
        Log.e("DisplayActivity", message, e);
        runOnUiThread(() -> {
            hideProgress();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
}