package com.dynamsoft.documentscanner;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.jsibbold.zoomage.ZoomageView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class Display2Activity extends AppCompatActivity {

    private ZoomageView documentImageView;
    private ProgressBar progressBar;
    private Button readDocBtn, rotateButton;
    // Mise à jour : Remplacer qualityStatus par les nouvelles vues
    private TextView expirationStatus, mrzStatus, textConsistencyStatus, ocrConfidenceStatus, screenshotStatus ,printCopyStatus;
    private ImageView expirationIcon, mrzIcon, authenticityIcon, textConsistencyIcon, screenshotIcon , printCopyIcon;

    private CustomerService customerService;
    private String customerId;
    private int rotation = 0;
    private Bitmap displayedBitmap;
    private boolean isDocumentInspected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display2);

        Toolbar toolbar = findViewById(R.id.simpleToolbar);
        setSupportActionBar(toolbar);

        // Supprimer le titre par défaut
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Définir le titre perso
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Vérification de document");

        ImageButton homebtn = findViewById(R.id.homeButton);
        homebtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        ImageButton docScanButton = findViewById(R.id.docScanButton);
        homebtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        docScanButton.setOnClickListener(v -> onScanButtonClicked());


        initializeViews();
        setupCustomerId();
        setupButtonListeners();
        loadDocumentImage();

        findViewById(R.id.selfieBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelfieCameraActivity.class);
            intent.putExtra("customerId", customerId);
            intent.putExtra("parentActivity", "Display2Activity");
            startActivityForResult(intent, 1001);
        });

        findViewById(R.id.selfieBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelfieCameraActivity.class);
            intent.putExtra("customerId", customerId);
            intent.putExtra("parentActivity", "CustomerDataActivity");
            startActivityForResult(intent, 1001);
        });
        documentImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) documentImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        View bottomControlBar = findViewById(R.id.bottomControlBar);
        if (bottomControlBar != null) {
            ImageButton selfieBtn = bottomControlBar.findViewById(R.id.selfieBtn);
            if (selfieBtn != null) {
                selfieBtn.setVisibility(View.GONE); // Cacher le bouton
            }
        }
    }

    private void onScanButtonClicked() {
        Intent intent = new Intent(this, CameraActivity2.class);
        intent.putExtra("customerId", customerId);
        startActivity(intent);
    }
    private void showImageFullscreen(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView imageView = dialog.findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(bitmap);

        // Fermer au clic
        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void initializeViews() {
        documentImageView = findViewById(R.id.documentImageView);
        progressBar = findViewById(R.id.progressBar);
        readDocBtn = findViewById(R.id.readDocBtn);
        rotateButton = findViewById(R.id.rotateButton);
        // Suppression de qualityStatus, non nécessaire dans le nouveau design
        // qualityStatus = findViewById(R.id.qualityStatus);

        // Initialisation des nouvelles vues pour les statuts
        expirationStatus = findViewById(R.id.expirationStatus);
        mrzStatus = findViewById(R.id.mrzStatus);
        textConsistencyStatus = findViewById(R.id.textConsistencyStatus);
        ocrConfidenceStatus = findViewById(R.id.ocrConfidenceStatus);
        screenshotStatus =  findViewById(R.id.screenshotStatus);
        printCopyStatus =  findViewById(R.id.printCopyStatus);


        expirationIcon = findViewById(R.id.expirationIcon);
        mrzIcon = findViewById(R.id.mrzIcon);
        textConsistencyIcon = findViewById(R.id.textConsistencyIcon);
        screenshotIcon = findViewById(R.id.screenshotIcon);
        printCopyIcon = findViewById(R.id.printCopyIcon);

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

                        // 1. Statut d'expiration
                        boolean expired = response.optBoolean("expired", true);
                        updateStatus(expirationStatus, expirationIcon,
                                "Expiration : " + (expired ? "EXPIRÉ" : "VALIDE"), !expired);

                        // 2. Inspection MRZ
                        JSONObject mrzInspection = response.optJSONObject("mrzInspection");
                        if (mrzInspection != null) {
                            boolean mrzValid = mrzInspection.optBoolean("valid", false);
                            updateStatus(mrzStatus, mrzIcon,
                                    "MRZ : " + (mrzValid ? "VALIDE" : "INVALIDE"), mrzValid);
                        }

                        // 3. Zone visuelle et texte
                        JSONObject visualZone = response.optJSONObject("visualZoneInspection");
                        if (visualZone != null) {
                            boolean textConsistent = visualZone.optJSONObject("textConsistency") != null
                                    && visualZone.optJSONObject("textConsistency").optBoolean("consistent", false);
                            updateStatus(textConsistencyStatus, textConsistencyIcon,
                                    "Texte : " + (textConsistent ? "COHÉRENT" : "INCOHÉRENT"), textConsistent);

                            JSONObject ocrConfidence = visualZone.optJSONObject("ocrConfidence");
                            double confidence = ocrConfidence != null ? ocrConfidence.optDouble("confidence", 0) * 100 : 0;
                            ocrConfidenceStatus.setText(String.format(Locale.getDefault(),
                                    "Confiance OCR : %.2f%%", confidence));
                        }

                        // 4. Altération de page (séparée)
                        JSONObject pageTampering = response.optJSONObject("pageTampering");
                        if (pageTampering != null) {
                            JSONObject front = pageTampering.optJSONObject("front");
                            if (front != null) {
                                // Screenshot
                                boolean isScreenshot = front.optBoolean("looksLikeScreenshot", false);
                                updateStatus(screenshotStatus, screenshotIcon,
                                        "Screenshot : " + (isScreenshot ? "OUI" : "NON"), !isScreenshot);

                                // Print copy
                                boolean isPrintCopy = front.optBoolean("looksLikePrintCopy", false);
                                updateStatus(printCopyStatus, printCopyIcon,
                                        "PrintCopy : " + (isPrintCopy ? "OUI" : "NON"), !isPrintCopy);
                            }
                        }

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

            // Mettre à jour les vues avec un statut d'erreur
            updateStatus(expirationStatus, expirationIcon, "Expiration : Inconnu", false);
            updateStatus(mrzStatus, mrzIcon, "MRZ : Inconnu", false);
            updateStatus(textConsistencyStatus, textConsistencyIcon, "Texte : Inconnu", false);
            ocrConfidenceStatus.setText("Confiance OCR : N/A");

            // Statuts d'altération
            if (screenshotStatus != null && screenshotIcon != null) {
                updateStatus(screenshotStatus, screenshotIcon, "Screenshot : Inconnu", false);
            }
            if (printCopyStatus != null && printCopyIcon != null) {
                updateStatus(printCopyStatus, printCopyIcon, "PrintCopy : Inconnu", false);
            }

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

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {

            // Récupérer le selfie s'il existe
           /* Bitmap selfie = data.getParcelableExtra("selfieBitmap");
            if (selfie != null) {
                CustomerDataActivity.selfieBitmap = selfie; // stocker pour CustomerDataActivity
            }
*/
            // Rediriger vers CustomerDataActivity
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

    /**
     * Méthode utilitaire pour mettre à jour les TextViews et ImageViews de statut.
     *
     * @param statusText La vue TextView à mettre à jour.
     * @param statusIcon La vue ImageView à mettre à jour.
     * @param text Le texte à afficher.
     * @param isOk True si le statut est positif, false sinon.
     */
    private void updateStatus(TextView statusText, ImageView statusIcon, String text, boolean isOk) {
        statusText.setText(text);
        int color = ContextCompat.getColor(this, isOk ? R.color.green : R.color.red);
        int iconResId = isOk ? R.drawable.ic_check_circle : R.drawable.ic_error_circle;
        statusIcon.setColorFilter(color);
        statusIcon.setImageResource(iconResId);
    }
}