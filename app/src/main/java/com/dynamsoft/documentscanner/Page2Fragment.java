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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import org.json.JSONObject;

public class Page2Fragment extends Fragment {
    private CustomerService customerService;
    private String customerId;

    // Rendre les variables de cache STATIC pour qu'elles persistent
    private static Bitmap cachedDocumentBitmap = null;
    private static String cachedDocumentBitmapCustomerId = null; // Ajouter un ID pour le recto
    private static Bitmap cachedDocumentPortrait = null;
    private static String cachedDocumentPortraitCustomerId = null; // Ajouter un ID pour le portrait

    private ImageView rectoImageView;
    private ImageView portraitImageView;
    private ImageView faceImageView;

    /**
     * Méthode statique pour vider le cache des images.
     * Appelée depuis l'activité principale.
     */
    public static void clearDocumentImageCache() {
        cachedDocumentBitmap = null;
        cachedDocumentBitmapCustomerId = null;
        cachedDocumentPortrait = null;
        cachedDocumentPortraitCustomerId = null;
    }

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

        boolean hasFaceImage = loadFaceImage();
        rfidSection.setVisibility(hasFaceImage ? View.VISIBLE : View.GONE);

        // Charger les images en vérifiant le cache
        if (customerId != null) {
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
        if (getActivity() == null) return;

        Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView imageView = dialog.findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(bitmap);

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

        // Vérifier si l'image est en cache pour l'ID client actuel
        if (cachedDocumentBitmap != null && customerId.equals(cachedDocumentBitmapCustomerId)) {
            rectoImageView.setImageBitmap(cachedDocumentBitmap);
            return;
        }

        // Appeler l'API si l'image n'est pas en cache
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
                            cachedDocumentBitmapCustomerId = customerId;
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

        // Vérifier si l'image est en cache pour l'ID client actuel
        if (cachedDocumentPortrait != null && customerId.equals(cachedDocumentPortraitCustomerId)) {
            portraitImageView.setImageBitmap(cachedDocumentPortrait);
            return;
        }

        // Appeler l'API si l'image n'est pas en cache
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
                            cachedDocumentPortraitCustomerId = customerId;
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