package com.dynamsoft.documentscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.FaceMatchingService;
import com.dynamsoft.documentscanner.util.ImageUtils;
import com.jsibbold.zoomage.ZoomageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Page3Fragment extends Fragment {
    private static final String TAG = "Page3Fragment";
    private static final int SELFIE_CAPTURE_REQUEST_CODE = 1001;

    // Vues
    private ZoomageView portraitImageView, selfieImageView, rfidImageView, selfieImageView2;
    private TextView similarityScoreTextView;
    private TextView rfidSimilarityScoreTextView;
    private CardView rfidComparisonCard;
    private LinearLayout scoreContainerPortraitSelfie;
    private LinearLayout scoreContainerRfidSelfie;
    private CardView selfieCameraButtonContainer;
    private CardView selfieCameraButtonContainer2;

    // Données
    private byte[] faceImageBytes;
    private String customerId;
    private Bitmap rfidBitmap;
    private ProgressDialog currentProgressDialog;

    // Variables pour stocker les images en cas de réessaye
    private Bitmap lastSelfieForComparison;
    private Bitmap lastRfidForComparison;

    // Nouvelle classe pour gérer l'état de l'erreur
    private static class ErrorState {
        String title;
        String message;
    }

    // Variable statique pour stocker l'erreur en attente
    private static ErrorState pendingError;

    private ProgressBar portraitProgressBar;
    private boolean portraitLoaded = false;
    public static void clearSelfieAndScores() {
        CustomerDataActivity.selfieBitmap = null;
        CustomerDataActivity.similarityScore = null;
        CustomerDataActivity.rfidSimilarityScore = null;
        CustomerDataActivity.rfidBitmap = null;
        CustomerDataActivity.portraitBitmap = null;
        pendingError = null;
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
                CustomerDataActivity.rfidBitmap = rfidBitmap;
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page3, container, false);
        initializeViews(view);
        setupImageClickListeners();
        portraitProgressBar = view.findViewById(R.id.portraitProgressBar);
        loadExistingImages();
        updateSelfieUI();
        checkExistingScores();

        return view;
    }

    private void initializeViews(View view) {
        portraitImageView = view.findViewById(R.id.portraitImageView);
        selfieImageView = view.findViewById(R.id.imageView2);
        selfieImageView2 = view.findViewById(R.id.selfieIMG);
        rfidImageView = view.findViewById(R.id.rfidImageView);
        similarityScoreTextView = view.findViewById(R.id.tvSimilarityScore);
        rfidSimilarityScoreTextView = view.findViewById(R.id.tvRfidSimilarityScore);
        rfidComparisonCard = view.findViewById(R.id.rfidComparisonCard);
        scoreContainerPortraitSelfie = view.findViewById(R.id.scoreContainerPortraitSelfie);
        scoreContainerRfidSelfie = view.findViewById(R.id.scoreContainerRfidSelfie);
        selfieCameraButtonContainer = view.findViewById(R.id.selfieCameraButtonContainer);
        selfieCameraButtonContainer2 = view.findViewById(R.id.selfieCameraButtonContainer2);
    }

    private void setupImageClickListeners() {
        portraitImageView.setOnClickListener(v -> openFullscreenImage(portraitImageView, "portrait"));
        selfieImageView.setOnClickListener(v -> openFullscreenImage(selfieImageView, "selfie"));
        rfidImageView.setOnClickListener(v -> openFullscreenImage(rfidImageView, "rfid"));
        selfieImageView2.setOnClickListener(v -> openFullscreenImage(selfieImageView2, "selfie2"));

        selfieCameraButtonContainer.setOnClickListener(v -> launchSelfieCamera());
        selfieCameraButtonContainer2.setOnClickListener(v -> launchSelfieCamera());
    }

    private void openFullscreenImage(ImageView imageView, String type) {
        if (imageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            FullscreenImageDialog.show(requireContext(), bitmap, () -> {
                Log.d(TAG, type + " Image saved callback");
            });
        }
    }

    private void launchSelfieCamera() {
        Intent intent = new Intent(getActivity(), SelfieCameraActivity.class);
        intent.putExtra("customerId", customerId);
        startActivityForResult(intent, SELFIE_CAPTURE_REQUEST_CODE);
    }

    private void loadExistingImages() {
        if (rfidBitmap != null) {
            rfidComparisonCard.setVisibility(View.VISIBLE);
            rfidImageView.setImageBitmap(rfidBitmap);
        } else {
            rfidComparisonCard.setVisibility(View.GONE);
        }

        // Vérifier si l'image portrait est déjà chargée
        if (CustomerDataActivity.portraitBitmap != null) {
            displayPortraitImage(CustomerDataActivity.portraitBitmap);
        } else if (customerId != null && !portraitLoaded) {
            // Charger l'image si elle n'est pas disponible
            loadPortraitImage();
        }
    }
    private void displayPortraitImage(Bitmap bitmap) {
        if (portraitProgressBar != null) {
            portraitProgressBar.setVisibility(View.GONE);
        }
        portraitImageView.setImageBitmap(bitmap);
        portraitImageView.setVisibility(View.VISIBLE);
        portraitLoaded = true;
    }    private void loadPortraitImage() {
        if (portraitProgressBar != null) {
            portraitProgressBar.setVisibility(View.VISIBLE);
        }
        portraitImageView.setVisibility(View.INVISIBLE);

        CustomerService customerService = new CustomerService();
        customerService.getDocumentPortrait(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    String base64Image = response.optString("data");
                    new Thread(() -> {
                        try {
                            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && bitmap != null) {
                                    CustomerDataActivity.portraitBitmap = bitmap;
                                    displayPortraitImage(bitmap);
                                }
                            });
                        } catch (Exception e) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded()) {
                                    handlePortraitLoadError();
                                }
                            });
                        }
                    }).start();
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        handlePortraitLoadError();
                    }
                });
            }
        });
    }

    private void handlePortraitLoadError() {
        if (portraitProgressBar != null) {
            portraitProgressBar.setVisibility(View.GONE);
        }
        portraitImageView.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Erreur de chargement du portrait", Toast.LENGTH_SHORT).show();
    }

    private void checkExistingScores() {
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        } else if (CustomerDataActivity.selfieBitmap != null && CustomerDataActivity.portraitBitmap != null) {
            checkSimilarityScore(customerId);
        }

        if (CustomerDataActivity.rfidSimilarityScore != null) {
            updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
        } else if (CustomerDataActivity.selfieBitmap != null && rfidBitmap != null) {
            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
        }
    }

    private void compareRfidAndSelfie(Bitmap selfie, Bitmap rfidPortrait) {
        lastSelfieForComparison = selfie;
        lastRfidForComparison = rfidPortrait;

        dismissCurrentProgressDialog();
        currentProgressDialog = new ProgressDialog(getContext());
        currentProgressDialog.setMessage("Comparaison des photos en cours...");
        currentProgressDialog.setCancelable(false);
        currentProgressDialog.show();

        FaceMatchingService faceMatchingService = new FaceMatchingService(requireContext());

        faceMatchingService.createProbeFace(selfie, new FaceMatchingService.FaceCreationCallback() {
            @Override
            public void onFaceCreated(String probeFaceId) {
                if (probeFaceId == null) {
                    handleError("Analyse impossible", "Le système n'a pas pu détecter de visage dans votre selfie.\n\n" +
                            "Conseils :\n• Assurez-vous que votre visage est bien visible\n• Évitez les ombres sur votre visage\n• Regardez droit vers la caméra", true);
                    dismissCurrentProgressDialog();
                    return;
                }
                faceMatchingService.createReferenceFace(rfidPortrait, new FaceMatchingService.FaceCreationCallback() {
                    @Override
                    public void onFaceCreated(String referenceFaceId) {
                        if (referenceFaceId == null) {
                            handleError("Analyse impossible", "Le système n'a pas pu détecter de visage dans la photo du document.\n\n" +
                                    "Conseils :\n• Vérifiez que le document est bien visible\n• Évitez les reflets sur la photo\n• Cadrez bien le visage sur le document", false);
                            dismissCurrentProgressDialog();
                            return;
                        }
                        faceMatchingService.matchFaces(probeFaceId, referenceFaceId, new FaceMatchingService.MatchCallback() {
                            @Override
                            public void onSuccess(double score) {
                                if (score < 0 || score > 1) {
                                    handleError("Comparaison impossible", "Le système n'a pas pu calculer la similarité. Veuillez réessayer avec des images contenant des visages clairs.",false);
                                    Log.e(TAG, "Score de similarité invalide du serveur: " + score);
                                    rfidSimilarityScoreTextView.setText("Score indisponible");
                                    dismissCurrentProgressDialog();
                                    return;
                                }
                                CustomerDataActivity.rfidSimilarityScore = (int) (score * 100);
                                updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
                                dismissCurrentProgressDialog();
                            }
                            @Override
                            public void onError(String error) {
                                handleError("Comparaison impossible", "Erreur technique lors de la comparaison des visages.", false);
                                dismissCurrentProgressDialog();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        handleError("Analyse impossible", "Erreur technique lors de l'analyse du document.\n\nVeuillez réessayer.", false);
                        dismissCurrentProgressDialog();
                    }
                });
            }

            @Override
            public void onError(String error) {
                handleError("Analyse impossible", "Erreur technique lors de l'analyse de votre selfie.\n\nVeuillez réessayer.", true);
                CustomerDataActivity.similarityScore = null;
                CustomerDataActivity.rfidSimilarityScore = null;
                rfidSimilarityScoreTextView.setText("Score indisponible");
                rfidSimilarityScoreTextView.setTextColor(Color.RED);
                similarityScoreTextView.setText("Score indisponible");
                similarityScoreTextView.setTextColor(Color.RED);
                dismissCurrentProgressDialog();
            }
        });
    }

    private void handleError(String title, String message, boolean showRetryOption) {
        if (isAdded() && isResumed()) {
            // Le fragment est visible, on affiche immédiatement le dialogue.
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(title)
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false);

            if (showRetryOption) {
                builder.setPositiveButton("Prendre un nouveau selfie", (dialog, which) -> {
                    launchSelfieCamera();
                    dialog.dismiss();
                });
                builder.setNegativeButton("Fermer", (dialog, which) -> dialog.dismiss());
            } else {
                builder.setNegativeButton("Fermer", (dialog, which) -> dialog.dismiss());
            }

            builder.show();
        } else {
            // Le fragment n'est pas visible, on stocke l'erreur avec une indication de réessaye.
            pendingError = new ErrorState();
            pendingError.title = title;
            pendingError.message = message;
            // On pourrait ajouter un champ `hasRetryOption` dans `ErrorState` pour le gérer dans onResume()
        }
        Log.e(TAG, "Erreur gérée : " + title + " - " + message);
    }

    private void dismissCurrentProgressDialog() {
        if (currentProgressDialog != null && currentProgressDialog.isShowing()) {
            currentProgressDialog.dismiss();
        }
        currentProgressDialog = null;
    }

    private void updateRfidSimilarityUI(int percentage) {
        if (rfidSimilarityScoreTextView != null && scoreContainerRfidSelfie != null) {
            if (percentage < 0 || percentage > 100) {
                rfidSimilarityScoreTextView.setText("Non calculable");
                int bgColor = ContextCompat.getColor(requireContext(), R.color.score_low_bg);
                int textColor = ContextCompat.getColor(requireContext(), R.color.score_low_text);
                setScoreContainerStyle(scoreContainerRfidSelfie, bgColor, rfidSimilarityScoreTextView, textColor);
                return;
            }
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
            setScoreContainerStyle(scoreContainerRfidSelfie, bgColor, rfidSimilarityScoreTextView, textColor);
        }
    }

    private void setScoreContainerStyle(View container, int bgColor, TextView textView, int textColor) {
        if (container.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) container.getBackground()).setColor(bgColor);
        } else {
            container.setBackgroundColor(bgColor);
        }
        textView.setTextColor(textColor);
    }

    public void refreshSelfieAndUpload(Bitmap selfie) {
        if (selfie != null) {
            CustomerDataActivity.similarityScore = null;
            CustomerDataActivity.rfidSimilarityScore = null;
            CustomerDataActivity.selfieBitmap = selfie;
            updateSelfieUI();
            uploadSelfieToServer(selfie);
        }
    }

    private void uploadSelfieToServer(Bitmap bitmap) {
        dismissCurrentProgressDialog();
        currentProgressDialog = new ProgressDialog(getContext());
        currentProgressDialog.setMessage("Upload du selfie en cours...");
        currentProgressDialog.setCancelable(false);
        currentProgressDialog.show();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encodedImage = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            JSONObject requestBody = new JSONObject();
            JSONObject imageObject = new JSONObject();
            imageObject.put("data", encodedImage);
            requestBody.put("image", imageObject);
            new CustomerService().provideCustomerSelfie(customerId, requestBody, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            dismissCurrentProgressDialog();
                            Log.d(TAG, "Selfie uploaded successfully");
                            Log.e("Fragmen3 upload", response.toString());
                            if (response.has("errorCode")) {
                                String errorCode = response.optString("errorCode");
                                if ("NO_FACE_DETECTED".equals(errorCode)) {
                                    handleError("Selfie non valide", "Aucun visage détecté dans votre selfie.\n\n" +
                                            "Conseils :\n• Assurez-vous que votre visage est bien visible\n• Évitez les ombres sur votre visage\n• Regardez droit vers la caméra\n• Prenez la photo dans un endroit bien éclairé", true);
                                    similarityScoreTextView.setText("Score indisponible");
                                    similarityScoreTextView.setTextColor(Color.RED);
                                    CustomerDataActivity.similarityScore = null;

                                    rfidSimilarityScoreTextView.setText("Score indisponible");
                                    rfidSimilarityScoreTextView.setTextColor(Color.RED);
                                    CustomerDataActivity.rfidSimilarityScore = null;

                                    return;
                                }
                            }
                            Toast.makeText(requireContext(), "Selfie uploaded successfully", Toast.LENGTH_SHORT).show();
                            checkSimilarityScore(customerId);
                        });
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            dismissCurrentProgressDialog();
                            Toast.makeText(requireContext(), "Erreur lors de l'upload du selfie: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Upload failed", e);
                        });
                    }
                }
            });
        } catch (JSONException | IOException e) {
            dismissCurrentProgressDialog();
            e.printStackTrace();
            Toast.makeText(requireContext(), "Erreur lors de la création de la requête JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSimilarityScore(String customerId) {
        new CustomerService().inspectCustomerDisclose(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        double score = response.getDouble("similarityScore");
                        if (score < 0 || score > 1) {
                            Log.e(TAG, "Score de similarité invalide du serveur: " + score);
                            handleError("Erreur de similarité", "Le score de similarité reçu est invalide. Veuillez réessayer.", true);
                            similarityScoreTextView.setText("Score indisponible");
                            similarityScoreTextView.setTextColor(Color.RED);
                            CustomerDataActivity.similarityScore = null;
                        } else {
                            CustomerDataActivity.similarityScore = score;
                            updateSimilarityScoreText(score);
                        }
                        if (rfidBitmap != null && CustomerDataActivity.selfieBitmap != null) {
                            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
                        }
                    } catch (JSONException e) {
                        handleError("Erreur de similarité", "Erreur lors du traitement de la réponse du serveur.",true);
                        similarityScoreTextView.setText("Score unavailable");
                        Log.e(TAG, "Error parsing similarity score", e);
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                handleError("Erreur de similarité", "Erreur lors de la vérification de similarité. Veuillez réessayer.",true);
            }
        });
    }

    private void updateSimilarityScoreText(double score) {
        int percentage = (int) (score * 100);
        if (similarityScoreTextView != null && scoreContainerPortraitSelfie != null) {
            String scoreText = String.format("Similarité: %.1f%%", score * 100);
            similarityScoreTextView.setText(scoreText);
            int bgColor, textColor;
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
            setScoreContainerStyle(scoreContainerPortraitSelfie, bgColor, similarityScoreTextView, textColor);
        }
    }

    private void updateSelfieUI() {
        if (CustomerDataActivity.selfieBitmap != null) {
            selfieImageView.setImageBitmap(CustomerDataActivity.selfieBitmap);
            selfieImageView2.setImageBitmap(CustomerDataActivity.selfieBitmap);
            selfieImageView2.setVisibility(View.VISIBLE);
            selfieImageView.setVisibility(View.VISIBLE);
            selfieCameraButtonContainer.setVisibility(View.GONE);
            selfieCameraButtonContainer2.setVisibility(View.GONE);
        } else {
            selfieImageView.setImageDrawable(null);
            selfieImageView2.setImageDrawable(null);
            selfieImageView2.setVisibility(View.GONE);
            selfieCameraButtonContainer.setVisibility(View.VISIBLE);
            selfieCameraButtonContainer2.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELFIE_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            String imagePath = data.getStringExtra("imagePath");
            if (imagePath != null) {
                Bitmap selfie = ImageUtils.handleRotation(imagePath);
                if (selfie != null) {
                    CustomerDataActivity.selfieBitmap = selfie;
                    refreshSelfieAndUpload(selfie);
                } else {
                    Toast.makeText(getContext(), "Échec du décodage de l'image du selfie.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Le chemin de l'image du selfie est introuvable.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSelfieUI();
        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
        if (CustomerDataActivity.similarityScore != null) {
            updateSimilarityScoreText(CustomerDataActivity.similarityScore);
        }
        if (CustomerDataActivity.rfidSimilarityScore != null) {
            updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
        }
        if (rfidComparisonCard != null) {
            rfidComparisonCard.setVisibility(rfidBitmap != null ? View.VISIBLE : View.GONE);
        }

        // Vérifier et afficher l'alerte en attente
        if (pendingError != null && getContext() != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle(pendingError.title)
                    .setMessage(pendingError.message)
                    .setNegativeButton("Fermer", (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();
            pendingError = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissCurrentProgressDialog();
    }
    public JSONObject getPdfData() {
        JSONObject data = new JSONObject();
        try {
            if (CustomerDataActivity.similarityScore != null) {
                data.put("portraitSelfieScore", String.format("%.1f%%", CustomerDataActivity.similarityScore * 100));
            } else {
                data.put("portraitSelfieScore", "N/A");
            }

            if (CustomerDataActivity.rfidSimilarityScore != null) {
                data.put("rfidSelfieScore", CustomerDataActivity.rfidSimilarityScore + "%");
            } else {
                data.put("rfidSelfieScore", "N/A");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erreur lors de la création de l'objet JSON pour le PDF", e);
        }
        return data;
    }
}

