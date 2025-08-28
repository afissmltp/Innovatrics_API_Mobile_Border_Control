package com.dynamsoft.documentscanner;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.FaceMatchingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Page3Fragment extends Fragment {
    private static final String TAG = "Page3Fragment";
    private static final int SELFIE_CAPTURE_REQUEST_CODE = 2002; // Un code de requête unique pour ce fragment

    // Vues
    private ImageView portraitImageView, selfieImageView, rfidImageView, selfieImageView2;
    private TextView similarityScoreTextView;
    private TextView rfidSimilarityScoreTextView;
    private CardView rfidComparisonCard;
    private LinearLayout scoreContainerPortraitSelfie;
    private LinearLayout scoreContainerRfidSelfie;
    private Button btnCaptureSelfie; // Référence au bouton de capture/recapture

    // Données
    private byte[] faceImageBytes;
    private String customerId;
    private Bitmap rfidBitmap;

    /**
     * Public static method to clear the selfie and all associated scores.
     * This ensures the UI is reset when a new document is scanned.
     */
    public static void clearSelfieAndScores() {
        CustomerDataActivity.selfieBitmap = null;
        CustomerDataActivity.similarityScore = null;
        CustomerDataActivity.rfidSimilarityScore = null;
    }

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
            if (faceImageBytes != null) {
                rfidBitmap = BitmapFactory.decodeByteArray(faceImageBytes, 0, faceImageBytes.length);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page3, container, false);

        // Initialiser les vues
        portraitImageView = view.findViewById(R.id.portraitImageView);
        selfieImageView = view.findViewById(R.id.imageView2);
        selfieImageView2 = view.findViewById(R.id.selfieIMG);
        rfidImageView = view.findViewById(R.id.rfidImageView);
        similarityScoreTextView = view.findViewById(R.id.tvSimilarityScore);
        rfidSimilarityScoreTextView = view.findViewById(R.id.tvRfidSimilarityScore);
        rfidComparisonCard = view.findViewById(R.id.rfidComparisonCard);
        scoreContainerPortraitSelfie = view.findViewById(R.id.scoreContainerPortraitSelfie);
        scoreContainerRfidSelfie = view.findViewById(R.id.scoreContainerRfidSelfie);
        btnCaptureSelfie = view.findViewById(R.id.btnCaptureSelfie); // Initialisation du bouton

        // Charger les images existantes
        if (rfidBitmap != null) {
            rfidComparisonCard.setVisibility(View.VISIBLE);
            rfidImageView.setImageBitmap(rfidBitmap);
        } else {
            rfidComparisonCard.setVisibility(View.GONE);
        }
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }

        updateSelfieUI(); // Appelle la méthode pour mettre à jour l'UI du selfie et du bouton

        // Mettre à jour les scores si les variables statiques sont disponibles
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        } else {
            // Si le selfie est présent, essayer de relancer la comparaison Portrait vs Selfie
            if (CustomerDataActivity.selfieBitmap != null && CustomerDataActivity.portraitBitmap != null) {
                // Appelez checkSimilarityScore pour initier le calcul si nécessaire
                // Cela devrait idéalement être géré après l'upload du selfie si ce n'est pas déjà fait.
                // Cependant, pour s'assurer que l'UI est à jour même sans nouvel upload, nous vérifions ici.
                checkSimilarityScore(customerId);
            }
        }


        if (CustomerDataActivity.rfidSimilarityScore != null) {
            updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
        } else {
            // Relancer la comparaison si les conditions sont remplies
            if (CustomerDataActivity.selfieBitmap != null && rfidBitmap != null) {
                compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
            }
        }

        // Setup image click listeners
        portraitImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) portraitImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        selfieImageView.setOnClickListener(v -> {
            // Seulement si une image est affichée, sinon le clic n'a pas de sens
            if (selfieImageView.getDrawable() != null) {
                Bitmap bitmap = ((BitmapDrawable) selfieImageView.getDrawable()).getBitmap();
                showImageFullscreen(bitmap);
            }
        });
        rfidImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) rfidImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        selfieImageView2.setOnClickListener(v -> {
            // Seulement si une image est affichée, sinon le clic n'a pas de sens
            if (selfieImageView2.getDrawable() != null) {
                Bitmap bitmap = ((BitmapDrawable) selfieImageView2.getDrawable()).getBitmap();
                showImageFullscreen(bitmap);
            }
        });

        // C'est ici que vous définissez ce qui se passe lorsque l'utilisateur clique sur le bouton
        btnCaptureSelfie.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SelfieCameraActivity.class);
            intent.putExtra("customerId", customerId);
            // Utilisez startActivityForResult pour que ce fragment reçoive le résultat
            startActivityForResult(intent, SELFIE_CAPTURE_REQUEST_CODE);
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

    private void compareRfidAndSelfie(Bitmap selfie, Bitmap rfidPortrait) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Comparaison RFID vs Selfie en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        FaceMatchingService faceMatchingService = new FaceMatchingService(requireContext());

        faceMatchingService.createProbeFace(selfie, probeFaceId -> {
            if (probeFaceId == null) {
                handleRfidComparisonError("Échec création face sonde", progressDialog);
                return;
            }
            faceMatchingService.createReferenceFace(rfidPortrait, referenceFaceId -> {
                if (referenceFaceId == null) {
                    handleRfidComparisonError("Échec création face référence", progressDialog);
                    return;
                }
                faceMatchingService.matchFaces(probeFaceId, referenceFaceId, new FaceMatchingService.MatchCallback() {
                    @Override
                    public void onSuccess(double score) {
                        CustomerDataActivity.rfidSimilarityScore = (int) (score * 100);
                        updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onError(String error) {
                        handleRfidComparisonError("Erreur comparaison RFID: " + error, progressDialog);
                    }
                });
            });
        });
    }

    private void handleRfidComparisonError(String message, ProgressDialog dialog) {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    private void updateRfidSimilarityUI(int percentage) {
        if (rfidSimilarityScoreTextView != null && scoreContainerRfidSelfie != null) {
            rfidSimilarityScoreTextView.setText("Similarité: " + percentage + "%");
            int bgColor;
            int textColor;
            if (percentage == 100) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_text);
            } else if (percentage >= 80) { // Condition ajustée
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_high_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_high_text);
            } else if (percentage >= 50) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_medium_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_medium_text);
            } else {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_low_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_low_text);
            }
            if (scoreContainerRfidSelfie.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) scoreContainerRfidSelfie.getBackground()).setColor(bgColor);
            } else {
                scoreContainerRfidSelfie.setBackgroundColor(bgColor);
            }
            rfidSimilarityScoreTextView.setTextColor(textColor);
        }
    }

    /**
     * Cette méthode est appelée par l'activité principale (CustomerDataActivity)
     * après qu'un nouveau selfie ait été pris et passé à ce fragment.
     */
    public void refreshSelfieAndUpload(Bitmap selfie) {
        if (selfie != null) {
            CustomerDataActivity.similarityScore = null;
            CustomerDataActivity.rfidSimilarityScore = null;
            CustomerDataActivity.selfieBitmap = selfie; // Mettre à jour le bitmap statique
            updateSelfieUI(); // Met à jour l'UI du selfie et du bouton
            uploadSelfieToServer(selfie); // Relance l'upload et le calcul des scores
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
                        if (rfidBitmap != null && CustomerDataActivity.selfieBitmap != null) { // Vérifier si selfieBitmap est non-null ici
                            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
                        }
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
        int percentage = (int) (score * 100);
        if (similarityScoreTextView != null && scoreContainerPortraitSelfie != null) {
            String scoreText = String.format("Similarité: %.1f%%", score * 100);
            similarityScoreTextView.setText(scoreText);
            int bgColor;
            int textColor;
            if (percentage >= 70 || percentage == 100) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_text);
            } else if (percentage >= 50) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_medium_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_medium_text);
            } else {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_low_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_low_text);
            }
            if (scoreContainerPortraitSelfie.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) scoreContainerPortraitSelfie.getBackground()).setColor(bgColor);
            } else {
                scoreContainerPortraitSelfie.setBackgroundColor(bgColor);
            }
            similarityScoreTextView.setTextColor(textColor);
        }
    }

    // Méthode centralisée pour gérer la mise à jour de l'UI du selfie et du bouton
    private void updateSelfieUI() {
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
            selfieImageView2.setImageBitmap(CustomerDataActivity.selfieBitmap);
            if (btnCaptureSelfie != null) {
                btnCaptureSelfie.setText("Recapturer le selfie");
            }
        } else {
            selfieImageView.setImageDrawable(null);
            selfieImageView2.setImageDrawable(null);
            if (btnCaptureSelfie != null) {
                btnCaptureSelfie.setText("Capturer un selfie");
            }
        }
    }

    /**
     * Gère le résultat de l'activité de capture de selfie lancée par ce fragment.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELFIE_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) { // Utilisation de Activity.RESULT_OK
            String imagePath = data.getStringExtra("imagePath");
            if (imagePath != null) {
                // Décode l'image et l'applique
                Bitmap selfie = handleRotation(imagePath);
                if (selfie != null) {
                    // Mettre à jour le bitmap statique dans l'activité pour qu'il soit disponible globalement
                    CustomerDataActivity.selfieBitmap = selfie;
                    // Rafraîchir l'UI du fragment et relancer l'upload/comparaison
                    refreshSelfieAndUpload(selfie);
                } else {
                    Toast.makeText(getContext(), "Échec du décodage de l'image du selfie.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Le chemin de l'image du selfie est introuvable.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Cette méthode pour gérer la rotation d'image.
     * Idéalement, elle devrait être déplacée dans une classe utilitaire ou CustomerDataActivity
     * si elle est partagée, mais pour la simplicité, elle est ici.
     */
    private Bitmap handleRotation(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;

        try {
            // Utiliser ExifInterface de androidx.exifinterface.media.ExifInterface si disponible
            // Sinon, l'importer manuellement ou l'ajouter aux dépendances
            // Pour l'exemple, nous allons supposer qu'il est disponible ou que vous le gérerez.
            // Si vous n'avez pas cette dépendance, vous devrez l'ajouter:
            // implementation 'androidx.exifinterface:exifinterface:1.3.3' (dernière version)
            ExifInterface exif = new ExifInterface(imagePath); // Utilisation de l'import direct
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap; // pas de rotation nécessaire
            }

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Erreur lors de la gestion de la rotation de l'image: " + e.getMessage());
        }
        return bitmap;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSelfieUI(); // Appel de la méthode centralisée pour la mise à jour de l'UI du selfie et du bouton

        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        }
        if (CustomerDataActivity.rfidSimilarityScore != null) {
            updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
        }
        // Relancer la comparaison RFID si un selfie est présent, RFID bitmap est présent, et le score RFID est null
        if (CustomerDataActivity.selfieBitmap != null && rfidBitmap != null && CustomerDataActivity.rfidSimilarityScore == null) {
            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
        }

        // Vérifier et potentiellement relancer la comparaison Portrait vs Selfie si nécessaire
        if (CustomerDataActivity.selfieBitmap != null && CustomerDataActivity.portraitBitmap != null && CustomerDataActivity.similarityScore == null) {
            checkSimilarityScore(customerId);
        }

        if (rfidComparisonCard != null) {
            rfidComparisonCard.setVisibility(rfidBitmap != null ? View.VISIBLE : View.GONE);
        }
    }
}

