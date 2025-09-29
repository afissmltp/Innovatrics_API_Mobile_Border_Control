package com.dynamsoft.documentscanner;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.PassportService;
import com.jsibbold.zoomage.ZoomageView;

import org.json.JSONException;
import org.json.JSONObject;

public class Page1Fragment extends Fragment {

    private TextView tvDob, tvNationality, tvDocumentType, tvDocumentNumber, tvDateOfExpiry, tvIssuingAuthority;
    private ZoomageView documentImageView;
    private ZoomageView modelDocImgView;
    private CustomerService customerService;
    private String customerId;
    private JSONObject customerData;

    private static Bitmap cachedDocumentBitmap = null;
    private static String cachedCustomerId = null;
    private static Bitmap cachedModelPassportBitmap = null;
    private static String cachedNation = null;
    private static String cachedDocType = null;
    private boolean isDataDisplayed = false; // 🔹 Nouveau flag
    private PassportService passportService;


    public static Page1Fragment newInstance(JSONObject customerData, String customerId) {
        Page1Fragment fragment = new Page1Fragment();
        Bundle args = new Bundle();
        args.putString("customerData", customerData != null ? customerData.toString() : new JSONObject().toString());
        args.putString("customerId", customerId);
        fragment.setArguments(args);
        return fragment;
    }

    public static void clearDocumentImageCache() {
        cachedDocumentBitmap = null;
        cachedCustomerId = null;
        cachedModelPassportBitmap = null;
        cachedNation = null;
        cachedDocType = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerService = new CustomerService();
        passportService = new PassportService();


        if (getArguments() != null) {
            try {
                String jsonString = getArguments().getString("customerData");
                customerData = new JSONObject(jsonString);
                customerId = getArguments().getString("customerId");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page1, container, false);

        tvDob = view.findViewById(R.id.tvDobFragment);
        tvNationality = view.findViewById(R.id.tvNationalityFragment);
        tvDocumentType = view.findViewById(R.id.tvDocumentTypeFragment);
        tvDocumentNumber = view.findViewById(R.id.tvDocumentNumberFragment);
        tvDateOfExpiry = view.findViewById(R.id.tvDateOfExpiryFragment);
        tvIssuingAuthority = view.findViewById(R.id.tvIssuingAuthorityFragment);
        documentImageView = view.findViewById(R.id.documentImageViewFragment);
        modelDocImgView =  view.findViewById(R.id.modelDocImgView);
        if (customerData != null && !isDataDisplayed) {
            displayCustomerData(customerData);
        }

        if (customerId == null) {
            Log.e("Page1Fragment", "customùer id est null");
        }

        if (customerId != null) {
            loadDocumentFrontImage();
        }

        documentImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) documentImageView.getDrawable()).getBitmap();
            FullscreenImageDialog.show(requireContext(), bitmap, () -> Log.d("FullscreenDialog", "Image saved callback"));
        });

        return view;
    }

    private void displayCustomerData(JSONObject customerData) {

        try {
            JSONObject customer = customerData.getJSONObject("customer");

            String dob = customer.optJSONObject("dateOfBirth") != null ? customer.getJSONObject("dateOfBirth").optString("mrz", "N/A") : "N/A";
            String nation = customer.optJSONObject("nationality") != null ? customer.getJSONObject("nationality").optString("mrz", "N/A") : "N/A";

            JSONObject document = customer.getJSONObject("document");
            JSONObject mrz = document.optJSONObject("mrz");
            String docType;
            if (mrz != null && mrz.optJSONObject("td3") != null) {
                docType = mrz.getJSONObject("td3").optString("documentCode", "N/A");
            } else {
                docType = "N/A";
            }

            String docNum = document.optJSONObject("documentNumber") != null ? document.getJSONObject("documentNumber").optString("mrz", "N/A") : "N/A";
            String expiry = document.optJSONObject("dateOfExpiry") != null ? document.getJSONObject("dateOfExpiry").optString("mrz", "N/A") : "N/A";
            String issuingAuth = document.optJSONObject("issuingAuthority") != null ? document.getJSONObject("issuingAuthority").optString("mrz", "N/A") : "N/A";

            tvDob.setText(dob);
            tvNationality.setText(nation);
            tvDocumentType.setText("P".equals(docType) ? "Passeport" : docType);
            tvDocumentNumber.setText(docNum);
            tvDateOfExpiry.setText(expiry);
            tvIssuingAuthority.setText(issuingAuth);

            Log.d("Page1Fragment", "Appel à fetchPassportImage avec nation=" + nation + " docType=" + docType);

            if (cachedModelPassportBitmap != null
                    && nation.equals(cachedNation)
                    && docType.equals(cachedDocType)) {
                modelDocImgView.setImageBitmap(cachedModelPassportBitmap);
                modelDocImgView.setVisibility(View.VISIBLE);
                Log.d("Page1Fragment", "Image modèle chargée depuis le cache");
                return;
            }

            passportService.fetchPassportImage(nation, docType, new PassportService.PassportImageCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    requireActivity().runOnUiThread(() -> {
                        if (bitmap != null) {
                            cachedModelPassportBitmap = bitmap;
                            cachedNation = nation;
                            cachedDocType = docType;
                            modelDocImgView.setImageBitmap(bitmap);
                            modelDocImgView.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("Page1Fragment", "Erreur fetchPassportImage", e);
                }
            });

            isDataDisplayed = true; // 🔹 Flag activé après affichage
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadDocumentFrontImage() {
        Log.d("Page1Fragment", "loadDocumentFrontImage() called");

        if (customerId == null || customerId.isEmpty()) {
            Log.w("Page1Fragment", "customerId is null or empty -> cannot load image");
            return;
        }

        // Vérifie si on a déjà l'image en cache
        if (cachedDocumentBitmap != null && customerId.equals(cachedCustomerId)) {
            Log.d("Page1Fragment", "Using cached image for customerId: " + customerId);
            documentImageView.setImageBitmap(cachedDocumentBitmap);
            return;
        }

        Log.d("Page1Fragment", "Loading image from API for customerId: " + customerId);

        customerService.getDocumentFrontImage(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("Page1Fragment", "API call success for document image");
                if (getActivity() == null || !isAdded()) {
                    Log.w("Page1Fragment", "Fragment not attached, skipping image display");
                    return;
                }

                getActivity().runOnUiThread(() -> {
                    String base64Image = response.optString("data", "");
                    Log.d("Page1Fragment", "Base64 image length: " + base64Image.length());

                    if (base64Image.isEmpty()) {
                        Log.w("Page1Fragment", "No image data returned from API");
                        return;
                    }

                    try {
                        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                        if (bitmap != null) {
                            Log.d("Page1Fragment", "Bitmap decoded OK: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                            cachedDocumentBitmap = bitmap;
                            cachedCustomerId = customerId;
                            CustomerDataActivity.rectoBitmap = bitmap;
                            documentImageView.setImageBitmap(bitmap);
                        } else {
                            Log.e("Page1Fragment", "Bitmap decoding failed (null result)");
                        }
                    } catch (Exception e) {
                        Log.e("Page1Fragment", "Exception while decoding Base64 image: " + e.getMessage(), e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Page1Fragment", "API call failed: " + e.getMessage(), e);
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to load document image", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    public void updateCustomerData(JSONObject customerData) {
        if (this.customerData == null || !this.customerData.toString().equals(customerData.toString())) {
            this.customerData = customerData;
            if (getView() != null) {
                displayCustomerData(customerData);
            }
        }
    }

}
