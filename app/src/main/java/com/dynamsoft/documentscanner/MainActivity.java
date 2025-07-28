package com.dynamsoft.documentscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.license.LicenseManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Button startScanButton;
    private TextView apiResponseTextView;
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private CustomerService customerService;
    private String customerId; // Variable membre pour stocker l'ID client

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation du service CustomerService
        customerService = new CustomerService();

        startScanButton = findViewById(R.id.startScanButton);
        apiResponseTextView = findViewById(R.id.apiResponseTextView);

        // Au démarrage, le bouton de scan est désactivé jusqu'à la création du client
        startScanButton.setEnabled(false);

        startScanButton.setOnClickListener(v -> {
            // Vérifie si l'ID client est disponible avant de commencer le scan
            if (customerId != null && !customerId.isEmpty()) {
                if (hasCameraPermission()) {
                    startScan(); // Démarre le scan et crée le document
                } else {
                    requestPermission(); // Demande la permission caméra
                }
            } else {
                // Si l'ID client n'est pas encore disponible, informe l'utilisateur
                Toast.makeText(MainActivity.this, "Client non créé. Veuillez patienter.", Toast.LENGTH_SHORT).show();
                createCustomer(); // Tente de recréer le client si nécessaire
            }
        });

        initDynamsoftLicense();
        createCustomer(); // Lance la création du client au démarrage de l'activité
    }

    private void createCustomer() {
        customerService.createCustomer(new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        // Extraction de l'ID client depuis la réponse JSON
                        customerId = response.getString("id");
                        String message = "Client créé avec ID: " + customerId;
                        apiResponseTextView.setText(message);

                        // Activation du bouton scan maintenant qu'on a l'ID client
                        startScanButton.setEnabled(true);

                        // Stockage de l'ID client dans les préférences partagées pour usage ultérieur
                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("customerId", customerId)
                                .apply();

                        Log.d("CustomerCreation", "ID client: " + customerId);

                    } catch (JSONException e) {
                        apiResponseTextView.setText("Erreur d'analyse de la réponse de création de client.");
                        Log.e("CustomerCreation", "Erreur JSON lors de l'extraction de l'ID client", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    apiResponseTextView.setText("Erreur lors de la création du client: " + e.getMessage());
                    startScanButton.setEnabled(false); // Désactive le bouton si la création échoue
                    Log.e("CustomerCreation", "Erreur réseau ou API lors de la création du client", e);
                });
            }
        });
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si la permission est accordée, démarre le scan
                startScan();
            } else {
                Toast.makeText(this, "Veuillez accorder la permission caméra pour continuer.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startScan(){
        // Assure que customerId n'est pas nul avant de démarrer l'activité et de créer le document
        if (customerId != null && !customerId.isEmpty()) {
            Intent intent = new Intent(this, CameraActivity.class);
            // Passe customerId à CameraActivity via l'Intent
            intent.putExtra("customerId", customerId);
            startActivity(intent);
            createDocument(); // Crée le document API après le démarrage de CameraActivity
        } else {
            Toast.makeText(this, "ID client non disponible. Réessayez la création du client.", Toast.LENGTH_SHORT).show();
            // Peut-être relancer createCustomer() ici si souhaité
        }
    }
    private void initDynamsoftLicense(){
        LicenseManager.initLicense("DLS2eyJoYW5kc2hha2VDb2RlIjoiMjAwMDAxLTE2NDk4Mjk3OTI2MzUiLCJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSIsInNlc3Npb25QYXNzd29yZCI6IndTcGR6Vm05WDJrcEQ5YUoifQ==", this, (isSuccess, error) -> {
            if (!isSuccess) {
                Log.e("DDN", "Erreur d'initialisation de la licence: " + error);
            }else{
                Log.d("DDN","Licence Dynamsoft valide.");
            }
        });
    }
    private void createDocument() {
        // Cette méthode utilise la variable membre `customerId` qui doit avoir été définie par `createCustomer()`
        if (customerId == null || customerId.isEmpty()) {
            Log.e("CreateDocument", "customerId est nul ou vide. Impossible de créer le document.");
            apiResponseTextView.setText("Erreur: ID client manquant pour la création du document.");
            return; // Sort de la méthode si customerId n'est pas disponible
        }

        try {
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

            customerService.createDocument(customerId, requestBody, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        try {
                            apiResponseTextView.setText("Document créé: " + response.toString(2));
                            Toast.makeText(MainActivity.this, "Document créé avec succès!", Toast.LENGTH_SHORT).show();
                            Log.d("CreateDocument", "Réponse de création de document: " + response.toString());
                        } catch (JSONException e) {
                            apiResponseTextView.setText("Création du document réussie mais erreur d'affichage de la réponse.");
                            Log.e("CreateDocument", "Erreur JSON lors de l'affichage de la réponse", e);
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        apiResponseTextView.setText("Erreur lors de la création du document: " + e.getMessage());
                        Log.e("CreateDocument", "Erreur réseau ou API lors de la création du document", e);
                    });
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur lors de la construction de la requête de document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("CreateDocument", "Erreur JSON lors de la construction du requestBody", e);
        }
    }
}