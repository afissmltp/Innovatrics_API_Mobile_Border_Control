package com.dynamsoft.documentscanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.model.SessionData;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.jsibbold.zoomage.ZoomageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.jsibbold.zoomage.ZoomageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class Page5Fragment extends Fragment {
    private static final String TAG = "Page5Fragment";

    private ZoomageView documentImageView;
    private ProgressBar progressBar;
    private TextView expirationStatus, mrzStatus, textConsistencyStatus, ocrConfidenceStatus, screenshotStatus, printCopyStatus;
    private ImageView expirationIcon, mrzIcon, textConsistencyIcon, screenshotIcon, printCopyIcon, ocrConfidenceIcon;
    private CustomerService customerService;
    private String customerId;
    private Bitmap displayedBitmap;
    private boolean isDocumentInspected = false;
    private ImageView genderIcon;
    private TextView genderStatus;

    // Nouvelle variable pour suivre si les données ont été chargées
    private boolean isDataLoaded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page5, container, false);
        initializeViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCustomerId();
        setupButtonListeners();

        // Vérifier si les données ont déjà été chargées
        if (!isDataLoaded) {
            loadDocumentImage();
            fetchCustomerData();
            isDataLoaded = true; // Marquer les données comme chargées
        } else {
            // Si les données sont déjà chargées, mettre à jour l'UI
            updateUIFromCachedData();
        }
    }

    public static Page5Fragment newInstance(String customerId) {
        Page5Fragment fragment = new Page5Fragment();
        Bundle args = new Bundle();
        args.putString("customerId", customerId);
        fragment.setArguments(args);
        return fragment;
    }

    private void initializeViews(View view) {
        documentImageView = view.findViewById(R.id.documentImageView);
        progressBar = view.findViewById(R.id.progressBar);

        expirationStatus = view.findViewById(R.id.expirationStatus);
        mrzStatus = view.findViewById(R.id.mrzStatus);
        textConsistencyStatus = view.findViewById(R.id.textConsistencyStatus);
        ocrConfidenceStatus = view.findViewById(R.id.ocrConfidenceStatus);
        screenshotStatus = view.findViewById(R.id.screenshotStatus);
        printCopyStatus = view.findViewById(R.id.printCopyStatus);

        expirationIcon = view.findViewById(R.id.expirationIcon);
        mrzIcon = view.findViewById(R.id.mrzIcon);
        textConsistencyIcon = view.findViewById(R.id.textConsistencyIcon);
        screenshotIcon = view.findViewById(R.id.screenshotIcon);
        printCopyIcon = view.findViewById(R.id.printCopyIcon);
        ocrConfidenceIcon = view.findViewById(R.id.ocrConfidenceIcon);

        documentImageView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        genderIcon = view.findViewById(R.id.genderIcon);
        genderStatus = view.findViewById(R.id.genderStatus);
    }

    private void setupCustomerId() {
        if (getArguments() != null) {
            customerId = getArguments().getString("customerId");
        }
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(getContext(), "Customer ID manquant", Toast.LENGTH_LONG).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
        customerService = new CustomerService();
    }

    private void updateUIFromCachedData() {
        // Mettre à jour l'image si elle a été chargée
        if (displayedBitmap != null) {
            documentImageView.setImageBitmap(displayedBitmap);
            documentImageView.setVisibility(View.VISIBLE);
        }
        fetchCustomerData();
        inspectDocument();
    }

    private void fetchCustomerData() {
        showProgress();
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            updateUIWithCustomer(response);
                            hideProgress();
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lecture données", e);
                            hideProgress();
                            Toast.makeText(getContext(), "Erreur lecture données", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(getContext(), "Échec de récupération du client", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateUIWithCustomer(JSONObject response) throws JSONException {
        JSONObject customer = response.getJSONObject("customer");
        String genderMRZ = customer.getJSONObject("gender").getString("mrz");
        String genderPortrait = customer.getJSONObject("gender").getString("documentPortrait");

        SessionData session = SessionData.getInstance(); // ✅ stocker les données

        if (genderMRZ.equalsIgnoreCase(genderPortrait)) {
            genderIcon.setImageResource(R.drawable.ic_check_circle);
            genderIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
            String text = "Genre : (" + genderMRZ + ") ✔";
            genderStatus.setText(text);
            session.genderComparison = text; // ✅ Sauvegarde
        } else {
            genderIcon.setImageResource(R.drawable.ic_error_circle);
            genderIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
            String text = "Genre : (MRZ: " + genderMRZ + " vs Portrait: " + genderPortrait + ") ❌";
            genderStatus.setText(text);
            session.genderComparison = text; // ✅ Sauvegarde
        }
    }


    private void inspectDocument() {
        showProgress();
        customerService.inspectDocument(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            isDocumentInspected = true;
                            boolean expired = response.optBoolean("expired", true);
                            updateStatus(expirationStatus, expirationIcon,
                                    "Expiration : " + (expired ? "EXPIRÉ" : "VALIDE"), !expired);

                            JSONObject mrzInspection = response.optJSONObject("mrzInspection");
                            if (mrzInspection != null) {
                                boolean mrzValid = mrzInspection.optBoolean("valid", false);
                                updateStatus(mrzStatus, mrzIcon,
                                        "MRZ : " + (mrzValid ? "VALIDE" : "INVALIDE"), mrzValid);
                            }

                            JSONObject visualZone = response.optJSONObject("visualZoneInspection");
                            if (visualZone != null) {
                                boolean textConsistent = visualZone.optJSONObject("textConsistency") != null
                                        && visualZone.optJSONObject("textConsistency").optBoolean("consistent", false);
                                updateStatus(textConsistencyStatus, textConsistencyIcon,
                                        "Cohérence du texte : " + (textConsistent ? "COHÉRENT" : "INCOHÉRENT"), textConsistent);

                                JSONObject ocrConfidence = visualZone.optJSONObject("ocrConfidence");
                                if (ocrConfidence != null) {
                                    double confidence = ocrConfidence.optDouble("confidence", 0) * 100;
                                    String text = String.format(Locale.getDefault(), "Confiance OCR : %.2f%%", confidence);
                                    boolean isHighConfidence = confidence >= 90.0;
                                    updateStatus(ocrConfidenceStatus, ocrConfidenceIcon, text, isHighConfidence);
                                }
                            }

                            JSONObject pageTampering = response.optJSONObject("pageTampering");
                            if (pageTampering != null) {
                                JSONObject front = pageTampering.optJSONObject("front");
                                if (front != null) {
                                    boolean isScreenshot = front.optBoolean("looksLikeScreenshot", false);
                                    updateStatus(screenshotStatus, screenshotIcon,
                                            "Capture d'écran : " + (isScreenshot ? "OUI" : "NON"), !isScreenshot);
                                    boolean isPrintCopy = front.optBoolean("looksLikePrintCopy", false);
                                    updateStatus(printCopyStatus, printCopyIcon,
                                            "Copie imprimée : " + (isPrintCopy ? "OUI" : "NON"), !isPrintCopy);
                                }
                            }
                            hideProgress();
                        } catch (Exception e) {
                            handleInspectionError("Erreur de traitement", e);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                handleInspectionError("Échec de l'inspection", e);
            }
        });
    }

    private void setupButtonListeners() {
        documentImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) documentImageView.getDrawable()).getBitmap();
            FullscreenImageDialog.show(getContext(), bitmap, () -> {
                Log.d(TAG, "Image saved callback");
            });
        });
    }

    private void handleInspectionError(String message, Exception e) {
        Log.e(TAG, message, e);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                hideProgress();
                updateStatus(expirationStatus, expirationIcon, "Expiration : Inconnu", false);
                updateStatus(mrzStatus, mrzIcon, "MRZ : Inconnu", false);
                updateStatus(textConsistencyStatus, textConsistencyIcon, "Texte : Inconnu", false);
                ocrConfidenceStatus.setText("Confiance OCR : N/A");
                if (screenshotStatus != null && screenshotIcon != null) {
                    updateStatus(screenshotStatus, screenshotIcon, "Capture d'écran : Inconnu", false);
                }
                if (printCopyStatus != null && printCopyIcon != null) {
                    updateStatus(printCopyStatus, printCopyIcon, "Copie imprimée : Inconnu", false);
                }
                Toast.makeText(getContext(), message + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
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
                    inspectDocument(); // Appeler l'inspection après le chargement de l'image
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
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                documentImageView.setImageBitmap(displayedBitmap);
                documentImageView.setVisibility(View.VISIBLE);
                hideProgress();
            });
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
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                documentImageView.setAlpha(0.5f);
            });
        }
    }

    private void hideProgress() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                documentImageView.setAlpha(1f);
            });
        }
    }

    private void handleImageError(String message, Exception e) {
        Log.e(TAG, message, e);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                hideProgress();
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            });
        }
    }

    private void updateStatus(TextView statusText, ImageView statusIcon, String text, boolean isOk) {
        statusText.setText(text);
        int color = ContextCompat.getColor(requireContext(), isOk ? R.color.green : R.color.red);
        int iconResId = isOk ? R.drawable.ic_check_circle : R.drawable.ic_error_circle;
        statusIcon.setColorFilter(color);
        statusIcon.setImageResource(iconResId);
        // ✅ Sauvegarde dans SessionData pour CustomerDataActivity
        SessionData session = SessionData.getInstance();
        if (statusText == expirationStatus) session.expirationStatus = text;
        else if (statusText == mrzStatus) session.mrzStatus = text;
        else if (statusText == textConsistencyStatus) session.textConsistencyStatus = text;
        else if (statusText == ocrConfidenceStatus) session.ocrConfidenceStatus = text;
        else if (statusText == screenshotStatus) session.screenshotStatus = text;
        else if (statusText == printCopyStatus) session.printCopyStatus = text;
    }
}