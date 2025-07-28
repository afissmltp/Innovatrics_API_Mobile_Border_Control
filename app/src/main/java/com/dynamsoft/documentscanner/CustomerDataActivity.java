package com.dynamsoft.documentscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Added for @NonNull annotation
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerDataActivity extends AppCompatActivity {
    private static final String TAG = "CustomerDataActivity";

    private TextView tvName;
    private TextView tvSurname;
    private TextView tvDateOfBirth;
    private TextView tvGender;
    private TextView tvNationality;
    private TextView tvDocumentType;
    private TextView tvDocumentNumber;
    private TextView tvDateOfExpiry;
    private TextView tvIssuingAuthority;
    private TextView tvMrzRaw;
    private CustomerService customerService;
    private ImageView imageView; // For document portrait
    private ImageView imageView2; // For customer selfie

    private Button openCambtn;
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private String customerId;

    private String selfieImagePath;
    private TextView tvSimilarityScore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customerdata);
        customerService = new CustomerService();

        // Initialize TextViews
        tvName = findViewById(R.id.tvName);
        tvSurname = findViewById(R.id.tvSurname);
        tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
        tvGender = findViewById(R.id.tvGender);
        tvNationality = findViewById(R.id.tvNationality);
        tvDocumentType = findViewById(R.id.tvDocumentType);
        tvDocumentNumber = findViewById(R.id.tvDocumentNumber);
        tvDateOfExpiry = findViewById(R.id.tvDateOfExpiry);
        tvIssuingAuthority = findViewById(R.id.tvIssuingAuthority);
        tvMrzRaw = findViewById(R.id.tvMrzRaw);
        tvSimilarityScore = findViewById(R.id.tvSimilarityScore);


        imageView = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        openCambtn = findViewById(R.id.openCambtn);

        customerId = getIntent().getStringExtra("customerId");

        if (customerId != null && !customerId.isEmpty()) {
            // Only fetch customer data if it's the first time onCreate is called
            // OR if you explicitly need to refresh data (not the case here for selfie capture)
            if (savedInstanceState == null) { // This check ensures it only runs on initial creation
                fetchCustomerData();
                loadDocumentPortrait(); // Load portrait only once too
            }

        } else {
            Toast.makeText(this, "No customer ID available. Cannot fetch data.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Customer ID is null or empty.");
            finish(); // Finish activity if no customer ID
        }

        openCambtn.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                startCamera();
            } else {
                requestPermission();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This is where CustomerDataActivity returns from FaceCameraActivity
        // Check if an updated selfie path exists from the Intent
        // We use getIntent() here to get the latest Intent that brought the activity to foreground
        String newSelfieImagePath = getIntent().getStringExtra("selfieImagePath");

        // Only load the selfie if it's a NEW path or if it wasn't loaded before
        // This prevents reloading the same selfie if onResume is called for other reasons
        if (newSelfieImagePath != null && !newSelfieImagePath.equals(selfieImagePath)) {
            selfieImagePath = newSelfieImagePath; // Update the stored path
            loadCustomerSelfie(selfieImagePath);
        } else if (selfieImagePath != null && imageView2.getDrawable() == null) {
            // Re-attempt to load if image view is empty but path exists (e.g., failed first time)
            loadCustomerSelfie(selfieImagePath);
        }
    }

    // --- New/Updated Methods ---

    private void fetchCustomerData() {
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Customer data fetched: " + response.toString());
                    Toast.makeText(CustomerDataActivity.this, "Customer data loaded.", Toast.LENGTH_SHORT).show();
                    try {
                        displayCustomerData(response);
                        fetchSimilarityScore();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing or displaying customer data", e);
                        Toast.makeText(CustomerDataActivity.this, "Error parsing customer data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerDataActivity.this,
                            "Error fetching customer data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to fetch customer data", e);
                });
            }
        });
    }

    private void loadCustomerSelfie(@Nullable String path) {
        if (path != null && !path.isEmpty()) {
            File selfieFile = new File(path);
            if (selfieFile.exists()) {
                try {
                    Bitmap selfieBitmap = BitmapFactory.decodeFile(selfieFile.getAbsolutePath());
                    if (selfieBitmap != null) {
                        imageView2.setImageBitmap(selfieBitmap);
                        Toast.makeText(this, "Selfie chargé avec succès !", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erreur de décodage du selfie.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erreur: Le bitmap du selfie est null après décodage.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du chargement du selfie: " + e.getMessage(), e);
                    Toast.makeText(this, "Impossible de charger le selfie.", Toast.LENGTH_SHORT).show();
                } finally {
                    // *** ADD THIS BLOCK TO DELETE THE FILE AFTER ATTEMPTING TO READ IT ***
                    // Ensure you removed the delete from FaceCameraActivity as per previous instructions
                    if (selfieFile.exists()) {
                        boolean deleted = selfieFile.delete();
                        if (!deleted) {
                            Log.w(TAG, "Échec de la suppression du fichier temporaire dans CustomerDataActivity: " + selfieFile.getAbsolutePath());
                        } else {
                            Log.d(TAG, "Fichier selfie temporaire supprimé avec succès: " + selfieFile.getAbsolutePath());
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Fichier selfie introuvable.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Fichier selfie à l'emplacement " + path + " n'existe pas.");
            }
        } else {
            Toast.makeText(this, "Aucun selfie reçu.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "selfieImagePath est null ou vide dans l'Intent.");
        }
    }

    private void loadDocumentPortrait() {
        try {
            Log.d(TAG, "Attempting to load document portrait for customerId: " + customerId);
            customerService.getDocumentPortrait(customerId, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    String base64Image = response.optString("data");

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

                    executor.execute(() -> {
                        try {
                            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                            handler.post(() -> {
                                if (bitmap != null) {
                                    imageView.setImageBitmap(bitmap);
                                    Toast.makeText(CustomerDataActivity.this, "Image portrait du document chargée avec succès.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(CustomerDataActivity.this, "Erreur de décodage du portrait du document.", Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Erreur: Le bitmap du portrait du document est null après décodage.");
                                }
                            });

                        } catch (Exception e) {
                            handler.post(() -> {
                                Toast.makeText(CustomerDataActivity.this, "Erreur lors du traitement de l'image du portrait du document.", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Erreur traitement portrait: " + e.getMessage(), e);
                            });
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Erreur API portrait : " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(CustomerDataActivity.this, "Erreur réseau (portrait) : " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initiating document portrait loading: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading document portrait: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void displayCustomerData(JSONObject customerData) {
        try {
            JSONObject customer = customerData.getJSONObject("customer");

            // Champs de base (nom, prénom, etc.)
            JSONObject givenNames = customer.optJSONObject("givenNames");
            String name = (givenNames != null && givenNames.has("mrz")) ? givenNames.getString("mrz") : "N/A";

            JSONObject surname = customer.optJSONObject("surname");
            String surName = (surname != null && surname.has("mrz")) ? surname.getString("mrz") : "N/A";

            JSONObject dateOfBirth = customer.optJSONObject("dateOfBirth");
            String dob = (dateOfBirth != null && dateOfBirth.has("mrz")) ? dateOfBirth.getString("mrz") : "N/A";

            JSONObject gender = customer.optJSONObject("gender");
            String gen = (gender != null && gender.has("mrz")) ? gender.getString("mrz") : "N/A";

            JSONObject nationality = customer.optJSONObject("nationality");
            String nation = (nationality != null && nationality.has("mrz")) ? nationality.getString("mrz") : "N/A";

            JSONObject document = customer.getJSONObject("document");

            // Type du document (ex : TD3)
            String docType = "N/A";
            JSONObject mrz = document.optJSONObject("mrz");
            if (mrz != null) {
                JSONObject td3 = mrz.optJSONObject("td3");
                if (td3 != null && td3.has("documentCode")) {
                    docType = td3.getString("documentCode");
                }
            }

            JSONObject documentNumber = document.optJSONObject("documentNumber");
            String docNum = (documentNumber != null && documentNumber.has("mrz")) ? documentNumber.getString("mrz") : "N/A";

            JSONObject dateOfExpiry = document.optJSONObject("dateOfExpiry");
            String expiry = (dateOfExpiry != null && dateOfExpiry.has("mrz")) ? dateOfExpiry.getString("mrz") : "N/A";

            JSONObject issuingAuthority = document.optJSONObject("issuingAuthority");
            String issuingAuth = (issuingAuthority != null && issuingAuthority.has("mrz")) ? issuingAuthority.getString("mrz") : "N/A";

            // MRZ brut (texte)
            String mrzText = "N/A";
            JSONObject additionalTexts = document.optJSONObject("additionalTexts");
            if (additionalTexts != null) {
                JSONObject machineReadableZone = additionalTexts.optJSONObject("machineReadableZone");
                if (machineReadableZone != null && machineReadableZone.has("visualZone")) {
                    mrzText = machineReadableZone.getString("visualZone");
                }
            }

            // Affichage dans les TextViews
            tvName.setText("Name: " + name);
            tvSurname.setText("Surname: " + surName);
            tvDateOfBirth.setText("Date of Birth: " + dob);
            tvGender.setText("Gender: " + gen);
            tvNationality.setText("Nationality: " + nation);
            tvDocumentType.setText("Document Type: " + docType);
            tvDocumentNumber.setText("Document Number: " + docNum);
            tvDateOfExpiry.setText("Date of Expiry: " + expiry);
            tvIssuingAuthority.setText("Issuing Authority: " + issuingAuth);
            tvMrzRaw.setText("MRZ:\n" + mrzText);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error extracting data from JSON", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Veuillez accorder la permission caméra pour continuer.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        Intent intent = new Intent(this, FaceCameraActivity.class);
        intent.putExtra("customerId", customerId);
        // Important: Remove selfieImagePath from here, it will be passed FROM FaceCameraActivity
        startActivity(intent);
    }

    // Nouvelle méthode pour appeler l'inspection de similarité
    private void fetchSimilarityScore() {
        if (customerId == null || customerId.isEmpty()) {
            Log.e(TAG, "Impossible de récupérer le score de similarité: customerId est null ou vide.");
            tvSimilarityScore.setText("Score de similarité: N/A (ID client manquant)");
            return;
        }

        // Vérifiez si le selfie a déjà été chargé et affiché
        // Si imageView2.getDrawable() est null, cela signifie que le selfie n'est pas encore affiché.
        // Vous pourriez vouloir attendre que le selfie soit chargé avant d'appeler l'inspection.
        // Ou, si l'inspection est purement côté serveur, elle peut être appelée indépendamment.
        // Ici, nous supposons qu'elle peut être appelée dès que les données client sont là.

        customerService.inspectCustomerDisclose(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        // Extraire le score du JSONObject retourné par CustomerService
                        double score = response.getDouble("similarityScore");
                        String scoreText = String.format("Score de similarité: %.2f%%", score * 100);
                        tvSimilarityScore.setText(scoreText);
                        Log.d(TAG, "Score de similarité affiché: " + score);

                        // Optionnel: Ajouter de la logique basée sur le score
                        if (score > 0.80) { // Exemple de seuil
                            Toast.makeText(CustomerDataActivity.this, "La correspondance du selfie est ÉLEVÉE!", Toast.LENGTH_SHORT).show();
                        } else if (score > 0.60) {
                            Toast.makeText(CustomerDataActivity.this, "La correspondance du selfie est MODÉRÉE.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CustomerDataActivity.this, "La correspondance du selfie est FAIBLE. Veuillez vérifier.", Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Erreur lors de l'extraction du score de similarité du JSON: " + e.getMessage(), e);
                        tvSimilarityScore.setText("Score de similarité: Erreur de parsing");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Échec de l'obtention du score de similarité: " + e.getMessage(), e);
                    tvSimilarityScore.setText("Score de similarité: Erreur de chargement");
                    Toast.makeText(CustomerDataActivity.this,
                            "Échec de l'obtention du score de similarité: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

}