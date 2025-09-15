package com.dynamsoft.documentscanner;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import com.dynamsoft.documentscanner.model.SessionData;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.jsibbold.zoomage.ZoomageView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class Display2Activity extends AppCompatActivity {
    private ZoomageView documentImageView;
    private ProgressBar progressBar;
    private Button readDocBtn, readNfcBtn;
    // Mise à jour : Remplacer qualityStatus par les nouvelles vues
    private TextView expirationStatus, mrzStatus, textConsistencyStatus, ocrConfidenceStatus, screenshotStatus ,printCopyStatus;
    private ImageView expirationIcon, mrzIcon, textConsistencyIcon, screenshotIcon , printCopyIcon, ocrConfidenceIcon;

    private CustomerService customerService;
    private String customerId;
    private int rotation = 0;
    private Bitmap displayedBitmap;
    private boolean isDocumentInspected = false;
    private ImageView ageIcon, genderIcon;
    private TextView ageStatus, genderStatus;
    private String ageComparisonStatus = "Non vérifié";
    private String genderComparisonStatus = "Non vérifié";
    private String expirationStatusText = "Non inspecté";
    private String mrzStatusText = "Non inspecté";
    private String printCopyStatusText = "Non inspecté";
    private String textConsistencyStatusText = "Non inspecté";
    private String ocrConfidenceStatusText = "Non inspecté";
    private String screenshotStatusText = "Non inspecté";


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

        setupToolbarButtons();
        initializeViews();
        setupCustomerId();
        loadDocumentImage();
        fetchCustomerData();
        setupButtonListeners();
    }
    private void setupToolbarButtons() {
        // Référence à la toolbar
        Toolbar toolbar = findViewById(R.id.simpleToolbar);
        if (toolbar != null) {
            // Masquer le bouton PDF dans Display2Activity
            ImageButton btnGeneratePdf = toolbar.findViewById(R.id.btnGeneratePdf);
            if (btnGeneratePdf != null) {
                btnGeneratePdf.setVisibility(View.GONE);
            }
            // Masquer le bouton PDF dans Display2Activity
            ImageButton selfieBtn = toolbar.findViewById(R.id.selfieBtn);
            if (selfieBtn != null) {
                selfieBtn.setVisibility(View.GONE);
            }

        }
    }
    private void initializeViews() {
        documentImageView = findViewById(R.id.documentImageView);
        progressBar = findViewById(R.id.progressBar);
        readDocBtn = findViewById(R.id.readDocBtn);
        readNfcBtn = findViewById(R.id.readNfcBtn);

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
        ocrConfidenceIcon = findViewById(R.id.ocrConfidenceIcon);
        readDocBtn.setEnabled(false);
        readNfcBtn.setEnabled(false);
        documentImageView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);


        // Coherence panel
        genderIcon = findViewById(R.id.genderIcon);
        genderStatus = findViewById(R.id.genderStatus);
    }

    private void setupCustomerId() {
        customerId = getIntent().getStringExtra("customerId");
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Customer ID manquant", Toast.LENGTH_LONG).show();
            finish();
        }
        customerService = new CustomerService();
    }
        private void fetchCustomerData() {
        showProgress();
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        updateUIWithCustomer(response);
                        hideProgress();
                    } catch (Exception e) {
                        e.printStackTrace();
                        hideProgress();
                        Toast.makeText(Display2Activity.this, "Erreur lecture données", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(Display2Activity.this, "Échec de récupération du client", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUIWithCustomer(JSONObject response) throws JSONException {
        JSONObject customer = response.getJSONObject("customer");
        // ---- Vérification de cohérence : Genre ----
        String genderMRZ = customer.getJSONObject("gender").getString("mrz");
        String genderPortrait = customer.getJSONObject("gender").getString("documentPortrait");
        if (genderMRZ.equalsIgnoreCase(genderPortrait)) {
            genderIcon.setImageResource(R.drawable.ic_check_circle);
            genderIcon.setColorFilter(getResources().getColor(R.color.green));
            genderComparisonStatus = "Genre : (" + genderMRZ + ") ✔";
            genderStatus.setText(genderComparisonStatus);
        } else {
            genderIcon.setImageResource(R.drawable.ic_error_circle);
            genderIcon.setColorFilter(getResources().getColor(R.color.red));
            genderComparisonStatus = "Genre : (MRZ: " + genderMRZ + " vs Portrait: " + genderPortrait + ") ❌";
            genderStatus.setText(genderComparisonStatus);
        }

        readDocBtn.setEnabled(true);
        readNfcBtn.setEnabled(true);
    }

    private void inspectDocument() {
        /*if (isDocumentInspected) {
            navigateToCustomerData();
            return;
        }*/
        showProgress();
        customerService.inspectDocument(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        isDocumentInspected = true;
                        // 1. Statut d'expiration
                        boolean expired = response.optBoolean("expired", true);
                        expirationStatusText = expired ? "EXPIRÉ" : "VALIDE";
                        updateStatus(expirationStatus, expirationIcon,
                                "Expiration : " + (expired ? "EXPIRÉ" : "VALIDE"), !expired);

                        // 2. Inspection MRZ
                        JSONObject mrzInspection = response.optJSONObject("mrzInspection");
                        if (mrzInspection != null) {
                            boolean mrzValid = mrzInspection.optBoolean("valid", false);
                            mrzStatusText = mrzValid ? "VALIDE" : "INVALIDE";
                            updateStatus(mrzStatus, mrzIcon,
                                    "MRZ : " + (mrzValid ? "VALIDE" : "INVALIDE"), mrzValid);
                        }

                        // 3. Zone visuelle et texte
                        JSONObject visualZone = response.optJSONObject("visualZoneInspection");
                        if (visualZone != null) {
                            boolean textConsistent = visualZone.optJSONObject("textConsistency") != null
                                    && visualZone.optJSONObject("textConsistency").optBoolean("consistent", false);
                            textConsistencyStatusText = textConsistent ? "COHÉRENT" : "INCOHÉRENT";
                            updateStatus(textConsistencyStatus, textConsistencyIcon,
                                    "Cohérence du texte : " + (textConsistent ? "COHÉRENT" : "INCOHÉRENT"), textConsistent);

                            JSONObject ocrConfidence = visualZone.optJSONObject("ocrConfidence");
                            if (ocrConfidence != null) {
                                double confidence = ocrConfidence.optDouble("confidence", 0) * 100;

                                String text = String.format(Locale.getDefault(),
                                        "Confiance OCR : %.2f%%", confidence);
                                ocrConfidenceStatusText = String.format(Locale.getDefault(), "%.2f%%", confidence);
                                // Définir un seuil
                                boolean isHighConfidence = confidence >= 90.0;
                                // Utiliser updateStatus pour icône + texte
                                updateStatus(ocrConfidenceStatus, ocrConfidenceIcon, text, isHighConfidence);
                            }
                        }

                        // 4. Altération de page (séparée)
                        JSONObject pageTampering = response.optJSONObject("pageTampering");
                        if (pageTampering != null) {
                            JSONObject front = pageTampering.optJSONObject("front");
                            if (front != null) {
                                // Screenshot
                                boolean isScreenshot = front.optBoolean("looksLikeScreenshot", false);
                                screenshotStatusText = isScreenshot ? "OUI" : "NON";
                                updateStatus(screenshotStatus, screenshotIcon,
                                        "Capture d'écran : " + (isScreenshot ? "OUI" : "NON"), !isScreenshot);

                                // Print copy
                                boolean isPrintCopy = front.optBoolean("looksLikePrintCopy", false);
                                printCopyStatusText = isPrintCopy ? "OUI" : "NON";
                                updateStatus(printCopyStatus, printCopyIcon,
                                        "Copie imprimée : " + (isPrintCopy ? "OUI" : "NON"), !isPrintCopy);
                            }
                        }

                        enableControls();
                        hideProgress();
                        //navigateToCustomerData();

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
    private void setupButtonListeners() {

        readDocBtn.setOnClickListener(v -> {
            if (!isDocumentInspected) {
                inspectDocument();
                Toast.makeText(this, "Inspection d'authenticité du document en cours...", Toast.LENGTH_SHORT).show();
                return;
            }else {
                navigateToCustomerData();
            }
        });

        findViewById(R.id.readNfcBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReadNFCActivity.class);
            intent.putExtra("customerId", customerId);
            startActivityForResult(intent, 1001);
        });

        documentImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) documentImageView.getDrawable()).getBitmap();
            FullscreenImageDialog.show(this, bitmap, () -> {
                // Optionnel : action après la sauvegarde
                Log.d("FullscreenDialog", "Image saved callback");
            });
        });

        ImageButton homebtn = findViewById(R.id.homeButton);
        homebtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
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
                updateStatus(screenshotStatus, screenshotIcon, "Capture d'écran : Inconnu", false);
            }
            if (printCopyStatus != null && printCopyIcon != null) {
                updateStatus(printCopyStatus, printCopyIcon, "Copie imprimée : Inconnu", false);
            }

            Toast.makeText(this, message + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void navigateToCustomerData() {
        if (customerId != null) {
            SessionData data = SessionData.getInstance();
            data.expirationStatus = expirationStatusText;
            data.mrzStatus = mrzStatusText;
            data.printCopyStatus = printCopyStatusText;
            data.textConsistencyStatus = textConsistencyStatusText;
            data.ocrConfidenceStatus = ocrConfidenceStatusText;
            data.screenshotStatus = screenshotStatusText;
            data.ageComparison = ageComparisonStatus;
            data.genderComparison = genderComparisonStatus;

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
    private void enableControls() {
        readDocBtn.setEnabled(true);
        readNfcBtn.setEnabled(true);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
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
    private void updateStatus(TextView statusText, ImageView statusIcon, String text, boolean isOk) {
        statusText.setText(text);
        int color = ContextCompat.getColor(this, isOk ? R.color.green : R.color.red);
        int iconResId = isOk ? R.drawable.ic_check_circle : R.drawable.ic_error_circle;
        statusIcon.setColorFilter(color);
        statusIcon.setImageResource(iconResId);
    }


}