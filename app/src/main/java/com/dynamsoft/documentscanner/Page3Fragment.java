package com.dynamsoft.documentscanner;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.FaceMatchingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;


public class Page3Fragment extends Fragment {
    private static final String TAG = "Page3Fragment";

    // Vues
    private ImageView portraitImageView, selfieImageView, rfidImageView, selfieImageView2;
    private TextView similarityScoreTextView;       // Portrait vs Selfie
    private TextView rfidSimilarityScoreTextView;   // RFID vs Selfie
    private CardView rfidComparisonCard;

    // Ajout des références aux conteneurs de score
    private LinearLayout scoreContainerPortraitSelfie;
    private LinearLayout scoreContainerRfidSelfie;

    // Données
    private byte[] faceImageBytes; // image issue du RFID
    private String customerId;

    // Bitmap
    private Bitmap rfidBitmap;

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

        // Initialiser les nouveaux conteneurs de score
        scoreContainerPortraitSelfie = view.findViewById(R.id.scoreContainerPortraitSelfie);
        scoreContainerRfidSelfie = view.findViewById(R.id.scoreContainerRfidSelfie);

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
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
            selfieImageView2.setImageBitmap(CustomerDataActivity.selfieBitmap);
        }

        // Mettre à jour les scores si les variables statiques sont disponibles
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        }
        if (CustomerDataActivity.rfidSimilarityScore != null) {
            updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
        }

        // Lancer la comparaison RFID vs Selfie UNIQUEMENT si le score n'existe pas encore
        if (CustomerDataActivity.selfieBitmap != null && rfidBitmap != null && CustomerDataActivity.rfidSimilarityScore == null) {
            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
        }
        portraitImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) portraitImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        selfieImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) selfieImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });

        rfidImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) rfidImageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });
        selfieImageView2.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) selfieImageView2.getDrawable()).getBitmap();
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

    /**
     * Comparaison RFID vs Selfie
     */
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
                        // Stocker le score dans la variable statique de l'activité
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

    /**
     * Met à jour le TextView du score RFID avec les couleurs
     * en fonction du pourcentage.
     */
    private void updateRfidSimilarityUI(int percentage) {
        if (rfidSimilarityScoreTextView != null && scoreContainerRfidSelfie != null) {
            rfidSimilarityScoreTextView.setText("Similarité: " + percentage + "%");

            int bgColor;
            int textColor;

            if (percentage == 100) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_perfect_text);
            } else if (percentage >= 80) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_high_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_high_text);
            } else if (percentage >= 50) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_medium_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_medium_text);
            } else {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_low_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_low_text);
            }

            // Mettre à jour la couleur de fond du conteneur
            if (scoreContainerRfidSelfie.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) scoreContainerRfidSelfie.getBackground()).setColor(bgColor);
            } else {
                scoreContainerRfidSelfie.setBackgroundColor(bgColor);
            }
            rfidSimilarityScoreTextView.setTextColor(textColor);
        }
    }

    /**
     * Upload selfie et check du score (portrait vs selfie)
     * Cette méthode a été mise à jour pour réinitialiser les scores.
     */
    public void refreshSelfieAndUpload(Bitmap selfie) {
        if (selfie != null) {
            // Réinitialiser les scores pour forcer un nouveau calcul
            CustomerDataActivity.similarityScore = null;
            CustomerDataActivity.rfidSimilarityScore = null;

            // Mettre à jour les images
            CustomerDataActivity.selfieBitmap = selfie;
            if (selfieImageView != null) {
                selfieImageView.setImageBitmap(selfie);
                selfieImageView2.setImageBitmap(selfie);
            }

            // Lancer le processus de mise à jour des scores
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
                        // Après l'upload, on relance le processus de vérification
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
                        // Une fois le score du document mis à jour, on relance la comparaison RFID
                        if (rfidBitmap != null) {
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

    /**
     * Met à jour le TextView du score Portrait vs Selfie avec les couleurs
     * en fonction du pourcentage.
     */
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
             /*}else if (percentage >= 80) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_high_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_high_text);*/
            } else if (percentage >= 50) {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_medium_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_medium_text);
            } else {
                bgColor = ContextCompat.getColor(requireContext(), R.color.score_low_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.score_low_text);
            }

            // Mettre à jour la couleur de fond du conteneur
            if (scoreContainerPortraitSelfie.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) scoreContainerPortraitSelfie.getBackground()).setColor(bgColor);
            } else {
                scoreContainerPortraitSelfie.setBackgroundColor(bgColor);
            }
            similarityScoreTextView.setTextColor(textColor);
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
        // Mise à jour de l'UI si les scores existent
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        }
        if (CustomerDataActivity.rfidSimilarityScore != null) {
            updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
        }

        // Relancer la comparaison UNIQUEMENT si le score RFID n'existe pas encore
        if (CustomerDataActivity.selfieBitmap != null && rfidBitmap != null && CustomerDataActivity.rfidSimilarityScore == null) {
            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
        }

        // Mettre à jour la visibilité du CardView
        if (rfidComparisonCard != null) {
            rfidComparisonCard.setVisibility(rfidBitmap != null ? View.VISIBLE : View.GONE);
        }
    }
}