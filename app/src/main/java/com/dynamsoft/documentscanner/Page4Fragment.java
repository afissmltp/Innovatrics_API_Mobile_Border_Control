package com.dynamsoft.documentscanner;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.FaceMatchingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private JSONObject customerData;

    private CustomerService customerService;
    private FaceMatchingService faceMatchingService;
    private String customerId;
    private boolean comparisonInProgress = false;
    private boolean comparisonDone = false;
    private boolean portraitLoaded = false;
    private ProgressDialog progressDialog;
    private ExecutorService executor;
    private int similarityScore = -1;
    private TextView tvGivenName, tvSurnameMrz, tvDobMrz, tvNationalityMrz, tvGenderMrz,
            tvDocumentNumberMrz, tvDateOfExpiryMrz, tvDocumentTypeMrz, tvIssuingAuthorityMrz;
    private ImageView iconSurname, iconGivenName, iconGender, iconDob, iconExpiryDate,
            iconDocNum, iconNationality, iconDocType, iconIssuer;
    private LinearLayout scoreContainerPortraitSelfie;

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
        faceImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) faceImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        docPortrait.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) docPortrait.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
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

        tvGivenName = view.findViewById(R.id.tvGivenName);
        tvSurnameMrz = view.findViewById(R.id.tvSurnameMrz);
        tvDobMrz = view.findViewById(R.id.tvDobMrz);
        tvNationalityMrz = view.findViewById(R.id.tvNationalityMrz);
        tvGenderMrz = view.findViewById(R.id.tvGenderMrz);

        tvDocumentNumberMrz = view.findViewById(R.id.tvDocumentNumberMrz);
        tvDateOfExpiryMrz = view.findViewById(R.id.tvDateOfExpiryMrz);
        tvDocumentTypeMrz = view.findViewById(R.id.tvDocumentTypeMrz);
        tvIssuingAuthorityMrz = view.findViewById(R.id.tvIssuingAuthorityMrz);
        // Liaison avec les ImageView
        iconSurname = view.findViewById(R.id.iconSurname);
        iconGivenName =  view.findViewById(R.id.iconGivenName);
        iconGender =  view.findViewById(R.id.iconGender);
        iconDob =  view.findViewById(R.id.iconDob);
        iconExpiryDate =  view.findViewById(R.id.iconExpiryDate);
        iconDocNum =  view.findViewById(R.id.iconDocNum);
        iconNationality =  view.findViewById(R.id.iconNationality);
        iconDocType =  view.findViewById(R.id.iconDocType);
        iconIssuer =  view.findViewById(R.id.iconIssuer);

        scoreContainerPortraitSelfie = view.findViewById(R.id.scoreContainerPortraitSelfie);

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

            nameTextView.setText(getNonEmpty(bundle.getString("name")));
            surnameTextView.setText(getNonEmpty(bundle.getString("surname")));
            genderTextView.setText(getNonEmpty(bundle.getString("gender")));
            birthDateTextView.setText(getNonEmpty(bundle.getString("birthDate")));
            expiryDateTextView.setText(getNonEmpty(bundle.getString("expiryDate")));
            serialNumberTextView.setText(getNonEmpty(bundle.getString("serialNumber")));
            nationalityTextView.setText(getNonEmpty(bundle.getString("nationality")));
            docTypeTextView.setText(getNonEmpty(bundle.getString("docType")));
            issuerAuthorityTextView.setText(getNonEmpty(bundle.getString("issuerAuthority")));


            customerId = bundle.getString("customerId");
            fetchCustomerData();
        }
    }

    private void fetchCustomerData() {
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return; // sécurité si fragment détaché

                requireActivity().runOnUiThread(() -> {
                    customerData = response; // Stocker les données
                    Log.d(TAG, "Customer data fetched: " + response.toString());

                    Toast.makeText(requireContext(), "Customer data loaded.", Toast.LENGTH_SHORT).show();

                    try {
                        displayCustomerData(response);   // ta méthode d’affichage
                        updateFragmentData(response);   // ta méthode de mise à jour
                        // fetchSimilarityScore();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing or displaying customer data", e);
                        Toast.makeText(requireContext(), "Error parsing customer data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return; // sécurité

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Error fetching customer data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to fetch customer data", e);
                });
            }
        });
    }
    private void displayCustomerData(JSONObject response) {
        try {
            JSONObject customer = response.getJSONObject("customer");

            // Champs MRZ
            String surname = customer.getJSONObject("surname").optString("mrz", "");
            String givenName = customer.getJSONObject("givenNames").optString("mrz", "");
            String dob = customer.getJSONObject("dateOfBirth").optString("mrz", "");
            String nationality = customer.getJSONObject("nationality").optString("mrz", "");
            String gender = customer.getJSONObject("gender").optString("mrz", "");

            // Transformer le genre
            if ("M".equalsIgnoreCase(gender)) {
                gender = "Homme";
            } else if ("F".equalsIgnoreCase(gender)) {
                gender = "Femme";
            }

            // Reformater les dates MRZ au format dd.MM.yyyy
            dob = formatMrzDate(dob);

            // Infos document
            JSONObject document = customer.getJSONObject("document");
            String documentNumber = document.getJSONObject("documentNumber").optString("mrz", "");
            String dateOfExpiry = document.getJSONObject("dateOfExpiry").optString("mrz", "");
            String issuingAuthority = document.getJSONObject("issuingAuthority").optString("mrz", "");

            // Reformater la date d'expiration MRZ
            dateOfExpiry = formatMrzDate(dateOfExpiry);

            // Extraire documentCode de td3
            String documentCode = "";
            JSONObject mrz = document.optJSONObject("mrz");
            if (mrz != null) {
                JSONObject td3 = mrz.optJSONObject("td3");
                if (td3 != null) {
                    documentCode = td3.optString("documentCode", "");
                    // Remplacer "P" par "Passeport"
                    if ("P".equalsIgnoreCase(documentCode)) {
                        documentCode = "Passeport";
                    }
                }
            }

            tvGivenName.setText(getNonEmpty(givenName));
            tvSurnameMrz.setText(getNonEmpty(surname));
            tvDobMrz.setText(getNonEmpty(dob));
            tvNationalityMrz.setText(getNonEmpty(nationality));
            tvGenderMrz.setText(getNonEmpty(gender));

            tvDocumentNumberMrz.setText(getNonEmpty(documentNumber));
            tvDateOfExpiryMrz.setText(getNonEmpty(dateOfExpiry));
            tvDocumentTypeMrz.setText(getNonEmpty(documentCode));
            tvIssuingAuthorityMrz.setText(getNonEmpty(issuingAuthority));
            compareFields();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l’affichage des données MRZ", e);
            Toast.makeText(requireContext(), "Erreur affichage données MRZ", Toast.LENGTH_SHORT).show();
        }
    }
    private void compareFields() {
        compareTextViews(surnameTextView, tvSurnameMrz, iconSurname);             // Nom
        compareTextViews(nameTextView, tvGivenName, iconGivenName);               // Prénom
        compareTextViews(genderTextView, tvGenderMrz, iconGender);                // Sexe
        compareTextViews(birthDateTextView, tvDobMrz, iconDob);                   // Date de naissance
        compareTextViews(expiryDateTextView, tvDateOfExpiryMrz, iconExpiryDate);  // Date d’expiration
        compareTextViews(serialNumberTextView, tvDocumentNumberMrz, iconDocNum);  // Numéro document
        compareTextViews(nationalityTextView, tvNationalityMrz, iconNationality); // Nationalité
        compareTextViews(docTypeTextView, tvDocumentTypeMrz, iconDocType);        // Type document
        compareTextViews(issuerAuthorityTextView, tvIssuingAuthorityMrz, iconIssuer); // Autorité
    }

    /**
     * Compare deux TextView : ajoute une icône ✅ ou ❌ selon compatibilité
     */
    private void compareTextViews(TextView tv1, TextView tv2, ImageView icon) {
        String value1 = tv1.getText().toString().trim();
        String value2 = tv2.getText().toString().trim();

        if (!value1.isEmpty() && !value2.isEmpty()) {
            boolean isOk = value1.equalsIgnoreCase(value2);

            // Choisir la couleur
            int color = isOk ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"); // vert / rouge

            icon.setImageResource(R.drawable.ic_check_circle); // icône générique
            icon.setColorFilter(color); // applique la couleur
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * Reformate une date MRZ au format dd.MM.yyyy
     */
    private String formatMrzDate(String mrzDate) {
        try {
            if (mrzDate.contains("-")) {
                // Format yyyy-M-d -> dd.MM.yyyy
                SimpleDateFormat mrzFormat = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
                Date date = mrzFormat.parse(mrzDate);
                if (date != null) {
                    SimpleDateFormat desiredFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    return desiredFormat.format(date);
                }
            } else if (mrzDate.length() == 6) {
                // Format MRZ court : yyMMdd -> dd.MM.yyyy
                SimpleDateFormat mrzShortFormat = new SimpleDateFormat("yyMMdd", Locale.getDefault());
                Date date = mrzShortFormat.parse(mrzDate);
                if (date != null) {
                    SimpleDateFormat desiredFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    return desiredFormat.format(date);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return mrzDate; // fallback si erreur
    }
    private void updateFragmentData(JSONObject response) {
        // Juste stocker la réponse brute dans une variable
        this.customerData = response;
    }
    private void showImageFullscreen(Bitmap bitmap) {
        if (getActivity() == null) return; // sécurité

        Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView imageView = dialog.findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(bitmap);

        // Fermer au clic
        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
        // Mettre à jour le texte
        similarityScoreTextView.setText("Similarité: " + percentage + "%");

        int bgColor;
        int textColor;

        // Appliquer la logique de couleur selon le pourcentage
        if (percentage >= 80) {
            bgColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_bg);
            textColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_text);
            Toast.makeText(getContext(), "Fort taux de similarité (" + percentage + "%)", Toast.LENGTH_SHORT).show();
        } else if (percentage >= 50) {
            bgColor = ContextCompat.getColor(requireContext(), R.color.score_medium_bg);
            textColor = ContextCompat.getColor(requireContext(), R.color.score_medium_text);
            Toast.makeText(getContext(), "Similarité moyenne (" + percentage + "%)", Toast.LENGTH_SHORT).show();
        } else {
            bgColor = ContextCompat.getColor(requireContext(), R.color.score_low_bg);
            textColor = ContextCompat.getColor(requireContext(), R.color.score_low_text);
            Toast.makeText(getContext(), "Faible taux de similarité (" + percentage + "%)", Toast.LENGTH_SHORT).show();
        }

        // Mettre à jour la couleur de fond du conteneur
        if (scoreContainerPortraitSelfie.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) scoreContainerPortraitSelfie.getBackground()).setColor(bgColor);
        } else {
            scoreContainerPortraitSelfie.setBackgroundColor(bgColor);
        }

        // Mettre à jour la couleur du texte
        similarityScoreTextView.setTextColor(textColor);
    }
    @Override
    public void onResume() {
        super.onResume();

        // Réappliquer le score et la couleur si déjà calculé
        if (similarityScore != -1 && scoreContainerPortraitSelfie != null && similarityScoreTextView != null) {
            updateSimilarityUI(similarityScore);
        }

        // Vérifier si la comparaison doit être relancée
        if (!comparisonDone && faceImageBitmap != null && cachedPortraitBitmap != null) {
            checkAndCompareFaces();
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

    private String getNonEmpty(String value) {
        return (value != null && !value.trim().isEmpty()) ? value : "-";
    }

    // Dans votre classe Page4Fragment
    public JSONObject getPdfData() {
        JSONObject data = new JSONObject();
        try {
            // Collecte les données de la puce (TextViews de gauche)
            JSONObject rfidData = new JSONObject();
            rfidData.put("surname", getNonEmpty(surnameTextView.getText().toString()));
            rfidData.put("givenName", getNonEmpty(nameTextView.getText().toString()));
            rfidData.put("gender", getNonEmpty(genderTextView.getText().toString()));
            rfidData.put("dateOfBirth", getNonEmpty(birthDateTextView.getText().toString()));
            rfidData.put("dateOfExpiry", getNonEmpty(expiryDateTextView.getText().toString()));
            rfidData.put("documentNumber", getNonEmpty(serialNumberTextView.getText().toString()));
            rfidData.put("nationality", getNonEmpty(nationalityTextView.getText().toString()));
            rfidData.put("docType", getNonEmpty(docTypeTextView.getText().toString()));
            rfidData.put("issuerAuthority", getNonEmpty(issuerAuthorityTextView.getText().toString()));
            data.put("rfidData", rfidData);

            // Collecte les données MRZ (TextViews de droite)
            JSONObject mrzData = new JSONObject();
            mrzData.put("surname", getNonEmpty(tvSurnameMrz.getText().toString()));
            mrzData.put("givenName", getNonEmpty(tvGivenName.getText().toString()));
            mrzData.put("gender", getNonEmpty(tvGenderMrz.getText().toString()));
            mrzData.put("dateOfBirth", getNonEmpty(tvDobMrz.getText().toString()));
            mrzData.put("dateOfExpiry", getNonEmpty(tvDateOfExpiryMrz.getText().toString()));
            mrzData.put("documentNumber", getNonEmpty(tvDocumentNumberMrz.getText().toString()));
            mrzData.put("nationality", getNonEmpty(tvNationalityMrz.getText().toString()));
            mrzData.put("docType", getNonEmpty(tvDocumentTypeMrz.getText().toString()));
            mrzData.put("issuerAuthority", getNonEmpty(tvIssuingAuthorityMrz.getText().toString()));
            data.put("mrzData", mrzData);

            // Ajoute le score de similarité faciale
            if (similarityScore != -1) {
                data.put("facialSimilarityScore", similarityScore);
            } else {
                data.put("facialSimilarityScore", "N/A");
            }

        } catch (JSONException e) {
            Log.e(TAG, "Erreur lors de la création de l'objet JSON pour le PDF", e);
        }
        return data;
    }
}