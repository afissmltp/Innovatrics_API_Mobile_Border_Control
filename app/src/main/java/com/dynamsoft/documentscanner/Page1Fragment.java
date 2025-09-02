package com.dynamsoft.documentscanner;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
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

import org.json.JSONException;
import org.json.JSONObject;

public class Page1Fragment extends Fragment {

    private TextView tvDob, tvNationality, tvDocumentType, tvDocumentNumber, tvDateOfExpiry, tvIssuingAuthority;
    private ImageView documentImageView;
    private CustomerService customerService;
    private String customerId;
    private JSONObject customerData;

    // Use a static cache to persist the image and its ID across fragment instances
    private static Bitmap cachedDocumentBitmap = null;
    private static String cachedCustomerId = null;

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param customerData The customer data to display.
     * @param customerId The ID of the customer.
     * @return A new instance of Page1Fragment.
     */
    public static Page1Fragment newInstance(JSONObject customerData, String customerId) {
        Page1Fragment fragment = new Page1Fragment();
        Bundle args = new Bundle();
        args.putString("customerData", customerData.toString());
        args.putString("customerId", customerId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Public static method to clear the document image cache.
     * This should be called from the Activity whenever a new document is scanned.
     */
    public static void clearDocumentImageCache() {
        cachedDocumentBitmap = null;
        cachedCustomerId = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerService = new CustomerService();

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

        if (customerData != null) {
            displayCustomerData(customerData);
        }

        if (customerId != null) {
            loadDocumentFrontImage();
        }

        documentImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) documentImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });

        return view;
    }

    private void showImageFullscreen(Bitmap bitmap) {
        if (getActivity() == null) return;

        Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView imageView = dialog.findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(bitmap);

        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void displayCustomerData(JSONObject customerData) {
        try {
            JSONObject customer = customerData.getJSONObject("customer");

            String dob = customer.optJSONObject("dateOfBirth") != null ? customer.getJSONObject("dateOfBirth").optString("mrz", "N/A") : "N/A";
            String nation = customer.optJSONObject("nationality") != null ? customer.getJSONObject("nationality").optString("mrz", "N/A") : "N/A";

            JSONObject document = customer.getJSONObject("document");
            JSONObject mrz = document.optJSONObject("mrz");
            String docType = "N/A";
            if (mrz != null && mrz.optJSONObject("td3") != null) {
                docType = mrz.getJSONObject("td3").optString("documentCode", "N/A");
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

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadDocumentFrontImage() {
        if (customerId == null || customerId.isEmpty()) return;

        // Check if we already have the image for THIS customerId
        if (cachedDocumentBitmap != null && customerId.equals(cachedCustomerId)) {
            documentImageView.setImageBitmap(cachedDocumentBitmap);
            return; // Exit to prevent API call
        }

        customerService.getDocumentFrontImage(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    String base64Image = response.optString("data");
                    if (!base64Image.isEmpty()) {
                        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        if (bitmap != null) {
                            cachedDocumentBitmap = bitmap;
                            cachedCustomerId = customerId; // Store the corresponding ID
                            CustomerDataActivity.rectoBitmap = bitmap;
                            documentImageView.setImageBitmap(bitmap);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to load document image", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    public void updateCustomerData(JSONObject customerData) {
        this.customerData = customerData;
        if (getView() != null) {
            displayCustomerData(customerData);
        }
    }
}