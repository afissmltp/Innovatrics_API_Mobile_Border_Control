package com.dynamsoft.documentscanner;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.FaceMatchingService;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Page4Fragment extends Fragment {
    private static final String TAG = "Page4Fragment";
    private static final String KEY_COMPARISON_DONE = "comparison_done";
    private static final String KEY_PORTRAIT_LOADED = "portrait_loaded";
    private static final String KEY_SCORE = "similarity_score";
    private static final String KEY_PORTRAIT = "portrait_bitmap";
    private static final String KEY_FACE = "face_bitmap";

    // Variables membres
    private ImageView faceImageView, docPortrait;
    private Bitmap cachedPortraitBitmap, faceImageBitmap;
    private TextView similarityScoreTextView, nameTextView, surnameTextView,
            genderTextView, birthDateTextView, expiryDateTextView,
            serialNumberTextView, nationalityTextView, docTypeTextView,
            issuerAuthorityTextView;

    private CustomerService customerService;
    private FaceMatchingService faceMatchingService;
    private String customerId;
    private boolean comparisonInProgress = false;
    private boolean comparisonDone = false;
    private boolean portraitLoaded = false;
    private ProgressDialog progressDialog;
    private ExecutorService executor;
    private int similarityScore = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restaurer l'état sauvegardé
        if (savedInstanceState != null) {
            comparisonDone = savedInstanceState.getBoolean(KEY_COMPARISON_DONE, false);
            portraitLoaded = savedInstanceState.getBoolean(KEY_PORTRAIT_LOADED, false);
            similarityScore = savedInstanceState.getInt(KEY_SCORE, -1);

            // Restaurer les bitmaps
            byte[] portraitBytes = savedInstanceState.getByteArray(KEY_PORTRAIT);
            if (portraitBytes != null) {
                cachedPortraitBitmap = BitmapFactory.decodeByteArray(portraitBytes, 0, portraitBytes.length);
            }

            byte[] faceBytes = savedInstanceState.getByteArray(KEY_FACE);
            if (faceBytes != null) {
                faceImageBitmap = BitmapFactory.decodeByteArray(faceBytes, 0, faceBytes.length);
            }
        }

        customerService = new CustomerService();
        faceMatchingService = new FaceMatchingService(requireContext());
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page4, container, false);
        initializeViews(view);
        setupFromArguments();

        // Afficher les données restaurées si elles existent
        if (cachedPortraitBitmap != null) {
            docPortrait.setImageBitmap(cachedPortraitBitmap);
        }

        if (faceImageBitmap != null) {
            faceImageView.setImageBitmap(faceImageBitmap);
        }

        if (similarityScore != -1) {
            similarityScoreTextView.setText("Similarité: " + similarityScore + "%");
        }

        // Charger le portrait si nécessaire
        if (!portraitLoaded && customerId != null && cachedPortraitBitmap == null) {
            loadDocumentPortrait();
        } else if (!comparisonDone) {
            checkAndCompareFaces();
        }

        return view;
    }

    @SuppressLint("WrongThread")
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Sauvegarder l'état
        outState.putBoolean(KEY_COMPARISON_DONE, comparisonDone);
        outState.putBoolean(KEY_PORTRAIT_LOADED, portraitLoaded);
        outState.putInt(KEY_SCORE, similarityScore);

        // Sauvegarder les bitmaps
        if (cachedPortraitBitmap != null) {
            ByteArrayOutputStream portraitStream = new ByteArrayOutputStream();
            cachedPortraitBitmap.compress(Bitmap.CompressFormat.PNG, 100, portraitStream);
            outState.putByteArray(KEY_PORTRAIT, portraitStream.toByteArray());
        }

        if (faceImageBitmap != null) {
            ByteArrayOutputStream faceStream = new ByteArrayOutputStream();
            faceImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, faceStream);
            outState.putByteArray(KEY_FACE, faceStream.toByteArray());
        }
    }

    private void initializeViews(View view) {
        faceImageView = view.findViewById(R.id.faceImageView);
        docPortrait = view.findViewById(R.id.docPortrait);
        similarityScoreTextView = view.findViewById(R.id.similarityScoreTextView);

        nameTextView = view.findViewById(R.id.nameTextView);
        surnameTextView = view.findViewById(R.id.surnameTextView);
        genderTextView = view.findViewById(R.id.genderTextView);
        birthDateTextView = view.findViewById(R.id.birthDateTextView);
        expiryDateTextView = view.findViewById(R.id.expiryDateTextView);
        serialNumberTextView = view.findViewById(R.id.serialNumberTextView);
        nationalityTextView = view.findViewById(R.id.nationalityTextView);
        docTypeTextView = view.findViewById(R.id.docTypeTextView);
        issuerAuthorityTextView = view.findViewById(R.id.issuerAuthorityTextView);
    }

    private void setupFromArguments() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            // Charger l'image du visage si elle n'est pas déjà restaurée
            if (faceImageBitmap == null) {
                byte[] faceImageBytes = bundle.getByteArray("faceImage");
                if (faceImageBytes != null) {
                    faceImageBitmap = BitmapFactory.decodeByteArray(faceImageBytes, 0, faceImageBytes.length);
                    faceImageView.setImageBitmap(faceImageBitmap);
                }
            }

            // Mettre à jour les informations textuelles
            nameTextView.setText("Prénom: " + bundle.getString("name", ""));
            surnameTextView.setText("Nom: " + bundle.getString("surname", ""));
            genderTextView.setText("Sexe: " + bundle.getString("gender", ""));
            birthDateTextView.setText("Date de naissance: " + bundle.getString("birthDate", ""));
            expiryDateTextView.setText("Date d'expiration: " + bundle.getString("expiryDate", ""));
            serialNumberTextView.setText("Numéro de série: " + bundle.getString("serialNumber", ""));
            nationalityTextView.setText("Nationalité: " + bundle.getString("nationality", ""));
            docTypeTextView.setText("Type de document: " + bundle.getString("docType", ""));
            issuerAuthorityTextView.setText("Autorité émetrice: " + bundle.getString("issuerAuthority", ""));

            customerId = bundle.getString("customerId");
        }
    }

    private void checkAndCompareFaces() {
        if (faceImageBitmap != null && cachedPortraitBitmap != null && !comparisonInProgress && !comparisonDone) {
            compareFaces();
        }
    }

    private void compareFaces() {
        if (!isAdded()) return;

        comparisonInProgress = true;

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Comparaison automatique des visages en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        faceMatchingService.createProbeFace(faceImageBitmap, probeFaceId -> {
            if (!isAdded()) {
                dismissProgressDialog();
                return;
            }

            if (probeFaceId == null) {
                handleComparisonError("Échec de la création de la face sonde");
                return;
            }

            faceMatchingService.createReferenceFace(cachedPortraitBitmap, referenceFaceId -> {
                if (!isAdded()) {
                    dismissProgressDialog();
                    return;
                }

                if (referenceFaceId == null) {
                    handleComparisonError("Échec de la création de la face référence");
                    return;
                }

                faceMatchingService.matchFaces(probeFaceId, referenceFaceId, new FaceMatchingService.MatchCallback() {
                    @Override
                    public void onSuccess(double score) {
                        if (!isAdded()) {
                            dismissProgressDialog();
                            return;
                        }

                        comparisonInProgress = false;
                        comparisonDone = true;
                        similarityScore = (int) (score * 100);
                        updateSimilarityUI(similarityScore);
                        dismissProgressDialog();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) {
                            dismissProgressDialog();
                            return;
                        }

                        handleComparisonError("Erreur lors de la comparaison: " + error);
                    }
                });
            });
        });
    }

    private void updateSimilarityUI(int percentage) {
        similarityScoreTextView.setText("Similarité: " + percentage + "%");

        if (percentage >= 80) {
            Toast.makeText(getContext(), "Fort taux de similarité (" + percentage + "%)", Toast.LENGTH_SHORT).show();
        } else if (percentage >= 50) {
            Toast.makeText(getContext(), "Similarité moyenne (" + percentage + "%)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Faible taux de similarité (" + percentage + "%)", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleComparisonError(String message) {
        comparisonInProgress = false;
        dismissProgressDialog();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    private void loadDocumentPortrait() {
        try {
            Log.d(TAG, "Chargement du portrait du document pour customerId: " + customerId);
            customerService.getDocumentPortrait(customerId, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    String base64Image = response.optString("data");

                    executor.execute(() -> {
                        try {
                            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (!isAdded()) return;

                                if (bitmap != null) {
                                    cachedPortraitBitmap = bitmap;
                                    portraitLoaded = true;
                                    docPortrait.setImageBitmap(bitmap);

                                    if (!comparisonDone) {
                                        checkAndCompareFaces();
                                    }

                                    Toast.makeText(getContext(), "Image portrait du document chargée avec succès.", Toast.LENGTH_LONG).show();
                                } else {
                                    handlePortraitError("Erreur de décodage du portrait du document");
                                }
                            });

                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (!isAdded()) return;
                                handlePortraitError("Erreur lors du traitement de l'image du portrait du document: " + e.getMessage());
                            });
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    if (!isAdded()) return;
                    handlePortraitError("Erreur réseau (portrait): " + e.getMessage());
                }
            });

        } catch (Exception e) {
            handlePortraitError("Erreur lors du chargement du portrait du document: " + e.getMessage());
        }
    }

    private void handlePortraitError(String message) {
        Log.e(TAG, message);
        if (isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdownNow();
        }
        dismissProgressDialog();

        // Nettoyer les bitmaps si nécessaire
        if (cachedPortraitBitmap != null && !portraitLoaded) {
            cachedPortraitBitmap.recycle();
        }
        if (faceImageBitmap != null) {
            faceImageBitmap.recycle();
        }
    }
}