package com.dynamsoft.documentscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.dynamsoft.license.LicenseManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int CAMERA_REQUEST_CODE = 10;
    private static final String[] CAMERA_PERMISSIONS = {Manifest.permission.CAMERA};

    private CustomerService customerService;
    private String customerId;

    private LinearLayout clickableLayout;
    private LinearLayout loadingLayout;
    private ProgressBar progressBar;
    private TextView loadingText;

    // Indicateur pour éviter les clics multiples
    private boolean isActionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.simpleToolbar);
        setSupportActionBar(toolbar);

        // Supprimer le titre par défaut
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Définir le titre perso
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("SMLTP@VerifDoc");


        customerService = new CustomerService();

        clickableLayout = findViewById(R.id.clickable_linear_layout);
        loadingLayout = findViewById(R.id.loading_layout);
        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);

        // Ajout de la vérification de l'indicateur dans le listener
        clickableLayout.setOnClickListener(v -> {
            if (!isActionInProgress) {
                onScanButtonClicked();
            }
        });

        showLoadingUI();
        createCustomer();
    }

    private void onScanButtonClicked() {
        // Marquer l'action comme en cours
        isActionInProgress = true;

        if (!isCustomerReady()) {
            isActionInProgress = false; // Réinitialiser si le client n'est pas prêt
            return;
        }

        if (!hasCameraPermission()) {
            requestCameraPermission();
            // Pas besoin de réinitialiser isActionInProgress ici, onRequestPermissionsResult s'en chargera
            return;
        }
        // Si on arrive ici, la permission est accordée
        launchCameraActivity();
    }

    private void launchCameraActivity() {
        createDocument(() -> {
            Intent intent = new Intent(this, CameraActivity2.class);
            intent.putExtra("customerId", customerId);
            startActivity(intent);
            // Réinitialiser l'indicateur après le lancement de l'activité caméra
            isActionInProgress = false;
        });
    }

    private boolean isCustomerReady() {
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Client non disponible. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }



    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, CAMERA_PERMISSIONS, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Important d'appeler super
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onScanButtonClicked(); // La permission est accordée, on relance l'action
            } else {
                Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_SHORT).show();
                isActionInProgress = false; // Réinitialiser l'indicateur si la permission est refusée
            }
        }
    }

    private void createCustomer() {
        customerService.createCustomer(new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        customerId = response.getString("id");
                        Log.d(TAG, "Client créé avec ID: " + customerId);

                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("customerId", customerId)
                                .apply();

                        showMainLayout();
                    } catch (JSONException e) {
                        handleCustomerCreationFailure("Erreur JSON: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> handleCustomerCreationFailure("Échec création client: " + e.getMessage()));
            }
        });
    }

    private void handleCustomerCreationFailure(String error) {
        Log.e(TAG, error);
        Toast.makeText(this, "Impossible de créer le client. Fermez et rouvrez l'application.", Toast.LENGTH_LONG).show();
        showErrorUI();
    }

    private void createDocument(Runnable onSuccessAction) {
        if (!isCustomerReady()) {
            isActionInProgress = false; // Réinitialiser si le client n'est pas prêt
            return;
        }

        try {
            JSONObject requestBody = createDocumentRequestBody();

            customerService.createDocument(customerId, requestBody, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Document créé");
                        if (onSuccessAction != null) onSuccessAction.run();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeActivity.this, "Échec création document. Réessayez.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erreur API création document", e);
                        isActionInProgress = false; // Réinitialiser l'indicateur en cas d'échec de création du document
                    });
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur configuration JSON", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Erreur JSON création document", e);
            isActionInProgress = false; // Réinitialiser l'indicateur en cas d'erreur JSON
        }
    }

    private JSONObject createDocumentRequestBody() throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONObject advice = new JSONObject();
        JSONObject classification = new JSONObject();

        classification.put("countries", new JSONArray());
        classification.put("types", new JSONArray());
        classification.put("editions", new JSONArray());
        classification.put("machineReadableTravelDocuments", new JSONArray());

        advice.put("classification", classification);
        requestBody.put("advice", advice);

        requestBody.put("sources", new JSONArray()
                .put("VIZ")
                .put("MRZ")
                .put("BARCODE")
                .put("DOCUMENT_PORTRAIT"));

        return requestBody;
    }
    private void showLoadingUI() {
        clickableLayout.setVisibility(LinearLayout.GONE);
        loadingLayout.setVisibility(LinearLayout.VISIBLE);
    }

    private void showMainLayout() {
        loadingLayout.setVisibility(LinearLayout.GONE);
        clickableLayout.setVisibility(LinearLayout.VISIBLE);
    }

    private void showErrorUI() {
        loadingLayout.setVisibility(LinearLayout.GONE);
        clickableLayout.setVisibility(LinearLayout.GONE);
    }
}