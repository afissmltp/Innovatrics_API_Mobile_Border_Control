package com.dynamsoft.documentscanner;

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
import androidx.lifecycle.ViewModelProvider;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Page1Fragment extends Fragment {
    private TextView tvDob, tvNationality,tvDocumentType,tvDocumentNumber,tvDateOfExpiry,tvIssuingAuthority;
    private JSONObject customerData;
    private ImageView documentImageView;
    private CustomerService customerService;
    private String customerId;
    private static Bitmap cachedDocumentBitmap = null;


    public static Page1Fragment newInstance(JSONObject customerData,String customerId) {
        Page1Fragment fragment = new Page1Fragment();
        Bundle args = new Bundle();
        args.putString("customerData", customerData.toString());
        args.putString("customerId", customerId);

        fragment.setArguments(args);
        return fragment;
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
                System.out.println("*************************** "+customerId);
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
        tvIssuingAuthority= view.findViewById(R.id.tvIssuingAuthorityFragment);
        documentImageView = view.findViewById(R.id.documentImageViewFragment);
        if (customerData != null) {
            displayCustomerData(customerData);
        }
        if(customerId != null){
            loadDocumentFrontImage();
        }

        return view;
    }

    public void updateCustomerData(JSONObject customerData) {
        this.customerData = customerData;
        if (getView() != null) {
            displayCustomerData(customerData);
        }
    }
    private void displayCustomerData(JSONObject customerData) {
        try {
            JSONObject customer = customerData.getJSONObject("customer");

            JSONObject dateOfBirth = customer.optJSONObject("dateOfBirth");
            String dob = (dateOfBirth != null && dateOfBirth.has("mrz")) ? dateOfBirth.getString("mrz") : "N/A";

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


            tvDob.setText(dob);
            tvNationality.setText(nation);
            tvDocumentType.setText(docType);
            tvDocumentNumber.setText(docNum);
            tvDateOfExpiry.setText(expiry);
            tvIssuingAuthority.setText(issuingAuth);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void loadDocumentFrontImage() {
        if (customerId == null || customerId.isEmpty()) {
            return;
        }
        // Vérifier si l'image est déjà en cache
        if (cachedDocumentBitmap != null) {
            documentImageView.setImageBitmap(cachedDocumentBitmap);
            return;
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
}
