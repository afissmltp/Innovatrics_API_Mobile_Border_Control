package com.dynamsoft.documentscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class Page3Fragment extends Fragment {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;

    private ImageView portraitImageView, selfieImageView;
    private TextView similarityScoreTextView;
    private Button openCamButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page3, container, false);

        // Initialize views
        portraitImageView = view.findViewById(R.id.portraitImageView);
        selfieImageView = view.findViewById(R.id.imageView2);
        similarityScoreTextView = view.findViewById(R.id.tvSimilarityScore);
        openCamButton = view.findViewById(R.id.openCambtn);

        // Set portrait image if available
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
        }
        if (CustomerDataActivity.similarityScore != null) {
            String scoreText = String.format("Similarity Score: %.1f%%",
                    CustomerDataActivity.similarityScore * 100);
            similarityScoreTextView.setText(scoreText);
        }
        // Set click listener for camera button
        openCamButton.setOnClickListener(v -> checkCameraPermission());

        return view;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            launchCamera();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            CustomerDataActivity.selfieBitmap = imageBitmap;
            if (imageBitmap != null) {
                selfieImageView.setImageBitmap(imageBitmap);
                uploadSelfieToServer(imageBitmap);
            }
        }
    }
    private void uploadSelfieToServer(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArray, Base64.NO_WRAP); // NO_WRAP évite les \n

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
                        Toast.makeText(requireContext(), "Selfie uploaded successfully", Toast.LENGTH_SHORT).show();
                        checkSimilarityScore(customerId);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
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
                        String scoreText = String.format("Similarity Score: %.1f%%", score * 100);
                        similarityScoreTextView.setText(scoreText);
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

    @Override
    public void onResume() {
        super.onResume();

        // Rafraîchir l'image si elle est disponible
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
        }

        // Rafraîchir aussi le portrait au cas où
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }

        if (CustomerDataActivity.similarityScore != null) {
            String scoreText = String.format("Similarity Score: %.1f%%",
                    CustomerDataActivity.similarityScore * 100);
            similarityScoreTextView.setText(scoreText);
        }
    }
}