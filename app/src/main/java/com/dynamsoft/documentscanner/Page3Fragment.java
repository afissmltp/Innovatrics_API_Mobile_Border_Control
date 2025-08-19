package com.dynamsoft.documentscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class Page3Fragment extends Fragment {
    private static final String TAG = "Page3Fragment";
    private ImageView portraitImageView, selfieImageView, rfidImageView,selfieImageView2;
    private TextView similarityScoreTextView;
    private byte[] faceImageBytes;
    private String customerId;

    public static Page3Fragment newInstance(String customerId, byte[] faceImageBytes) {
        Page3Fragment fragment = new Page3Fragment();
        Bundle args = new Bundle();
        args.putString("customerId", customerId);
        args.putByteArray("faceImage", faceImageBytes);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getString("customerId");
            faceImageBytes = getArguments().getByteArray("faceImage");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page3, container, false);

        portraitImageView = view.findViewById(R.id.portraitImageView);
        selfieImageView = view.findViewById(R.id.imageView2);
        selfieImageView2 = view.findViewById(R.id.selfieIMG);
        similarityScoreTextView = view.findViewById(R.id.tvSimilarityScore);
        rfidImageView = view.findViewById(R.id.rfidImageView); // ajoutez le nouveau

        if (faceImageBytes != null) {
            Bitmap faceBitmap = BitmapFactory.decodeByteArray(faceImageBytes, 0, faceImageBytes.length);
            rfidImageView.setImageBitmap(faceBitmap);
        }
        // Afficher portrait et selfie existants
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
            selfieImageView2.setImageBitmap(CustomerDataActivity.selfieBitmap);

        }
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        }

        return view;
    }

    /**
     * Méthode appelée par CustomerDataActivity pour rafraîchir le selfie et lancer l'upload.
     */
    public void refreshSelfieAndUpload(Bitmap selfie) {
        if (selfie != null) {
            CustomerDataActivity.selfieBitmap = selfie;
            if (selfieImageView != null) {
                selfieImageView.setImageBitmap(selfie);
                selfieImageView2.setImageBitmap(selfie);
            }
            uploadSelfieToServer(selfie);
        }
    }

    private void uploadSelfieToServer(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArray, Base64.NO_WRAP);

        try {
            JSONObject requestBody = new JSONObject();
            JSONObject imageObject = new JSONObject();
            imageObject.put("data", encodedImage);
            requestBody.put("image", imageObject);

            String customerId = ((CustomerDataActivity) requireActivity()).getCustomerId();

            new CustomerService().provideCustomerSelfie(customerId, requestBody, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    requireActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Selfie uploaded successfully");
                        Toast.makeText(requireContext(), "Selfie uploaded successfully", Toast.LENGTH_SHORT).show();
                        checkSimilarityScore(customerId);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                    Log.e(TAG, "Upload failed", e);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSimilarityScore(String customerId) {
        new CustomerService().inspectCustomerDisclose(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        double score = response.getDouble("similarityScore");
                        CustomerDataActivity.similarityScore = score;
                        updateSimilarityScoreText(score);
                    } catch (JSONException e) {
                        similarityScoreTextView.setText("Score unavailable");
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error checking similarity", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void updateSimilarityScoreText(double score) {
        if (similarityScoreTextView != null) {
            String scoreText = String.format("Similarity Score: %.1f%%", score * 100);
            similarityScoreTextView.setText(scoreText);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
        }
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        }
    }
}
