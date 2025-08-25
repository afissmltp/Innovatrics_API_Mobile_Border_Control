package com.dynamsoft.documentscanner;

import android.app.Dialog;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONObject;

public class Page2Fragment extends Fragment {
    private CustomerService customerService;
    private String customerId;
    private static Bitmap cachedDocumentBitmap = null;
    private static Bitmap cachedDocumentPortrait = null;
    private ImageView rectoImageView;
    private ImageView portraitImageView;
    private ImageView faceImageView;

    public static Page2Fragment newInstance(String customerId, byte[] faceImageBytes) {
        Page2Fragment fragment = new Page2Fragment();
        Bundle args = new Bundle();
        args.putString("customerId", customerId);
        args.putByteArray("faceImage", faceImageBytes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerService = new CustomerService();
        if (getArguments() != null) {
            try {
                customerId = getArguments().getString("customerId");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page2, container, false);

        portraitImageView = view.findViewById(R.id.portraitImageView);
        rectoImageView = view.findViewById(R.id.rectoImageView);
        faceImageView = view.findViewById(R.id.faceImageView);
        LinearLayout rfidSection = view.findViewById(R.id.rfidSection);

        // Charger l'image de face et gérer la visibilité
        boolean hasFaceImage = loadFaceImage();
        rfidSection.setVisibility(hasFaceImage ? View.VISIBLE : View.GONE);

        // Afficher les images si elles sont disponibles
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
        if(customerId != null){
            loadDocumentFrontImage();
            loadDocumentPortrait();
        }
        portraitImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) portraitImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });

        faceImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) faceImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        rectoImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) rectoImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });

        return view;
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
    private boolean loadFaceImage() {
        Bundle args = getArguments();
        if (args != null) {
            byte[] faceImageBytes = args.getByteArray("faceImage");
            if (faceImageBytes != null && faceImageBytes.length > 0) {
                Bitmap faceBitmap = BitmapFactory.decodeByteArray(faceImageBytes, 0, faceImageBytes.length);
                if (faceBitmap != null) {
                    faceImageView.setImageBitmap(faceBitmap);
                    return true;
                }
            }
        }
        return false;
    }
    private void loadDocumentFrontImage() {
        if (customerId == null || customerId.isEmpty()) {
            return;
        }
        // Vérifier si l'image est déjà en cache
        if (cachedDocumentBitmap != null) {
            rectoImageView.setImageBitmap(cachedDocumentBitmap);
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
                            rectoImageView.setImageBitmap(bitmap);
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

    private void loadDocumentPortrait() {
        if (customerId == null || customerId.isEmpty()) {
            return;
        }
        // Vérifier si l'image est déjà en cache
        if (cachedDocumentPortrait != null) {
            portraitImageView.setImageBitmap(cachedDocumentPortrait);
            return;
        }
        customerService.getDocumentPortrait(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    String base64Image = response.optString("data");
                    if (!base64Image.isEmpty()) {
                        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        if (bitmap != null) {
                            cachedDocumentPortrait = bitmap;
                            portraitImageView.setImageBitmap(bitmap);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to load document portrait image", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}