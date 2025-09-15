package com.dynamsoft.documentscanner;

import android.app.Activity;
import android.app.AlertDialog;
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

    /**
     * Public static method to clear the selfie and all associated scores.
     */
    public static void clearSelfieAndScores() {
        CustomerDataActivity.selfieBitmap = null;
        CustomerDataActivity.similarityScore = null;
        CustomerDataActivity.rfidSimilarityScore = null;
        CustomerDataActivity.rfidBitmap = null;
        CustomerDataActivity.portraitBitmap=null;
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

        if (CustomerDataActivity.portraitBitmap != null) {
            portraitImageView.setImageBitmap(CustomerDataActivity.portraitBitmap);
        }
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
                    handleRfidComparisonError("Échec création face sonde");
                    return;
                }
                faceMatchingService.createReferenceFace(rfidPortrait, new FaceMatchingService.FaceCreationCallback() {
                    @Override
                    public void onFaceCreated(String referenceFaceId) {
                        if (referenceFaceId == null) {
                            handleRfidComparisonError("Échec création face référence");
                            return;
                        }
                        faceMatchingService.matchFaces(probeFaceId, referenceFaceId, new FaceMatchingService.MatchCallback() {
                            @Override
                            public void onSuccess(double score) {
                                CustomerDataActivity.rfidSimilarityScore = (int) (score * 100);
                                updateRfidSimilarityUI(CustomerDataActivity.rfidSimilarityScore);
                                dismissCurrentProgressDialog();
                            }
                            @Override
                            public void onError(String error) {
                                handleRfidComparisonError("Erreur comparaison RFID: " + error);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        handleRfidComparisonError("Échec création face référence: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                handleRfidComparisonError("Échec création face sonde: " + error);
            }
        });
    }
    private void dismissCurrentProgressDialog() {
        if (currentProgressDialog != null && currentProgressDialog.isShowing()) {
            currentProgressDialog.dismiss();
        }
        currentProgressDialog = null;
    }

    private void handleRfidComparisonError(String message) {
        dismissCurrentProgressDialog();
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
        dismissCurrentProgressDialog(); // S'assurer qu'aucun précédent n'est actif
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
                            dismissCurrentProgressDialog(); // Fermeture du progress
                            Log.d(TAG, "Selfie uploaded successfully");
                            Toast.makeText(requireContext(), "Selfie uploaded successfully", Toast.LENGTH_SHORT).show();
                            checkSimilarityScore(customerId);
                        });
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            dismissCurrentProgressDialog(); // Fermeture même en cas d'erreur
                            Toast.makeText(requireContext(), "Erreur lors de l'upload du selfie: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Upload failed", e);
                        });
                    }
                }
            });
        } catch (JSONException e) {
            dismissCurrentProgressDialog();
            e.printStackTrace();
            Toast.makeText(requireContext(), "Erreur lors de la création de la requête JSON", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            dismissCurrentProgressDialog();
            Log.e(TAG, "Error closing stream", e);
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
                        if (rfidBitmap != null && CustomerDataActivity.selfieBitmap != null) {
                            compareRfidAndSelfie(CustomerDataActivity.selfieBitmap, rfidBitmap);
                        }
                    } catch (JSONException e) {
                        similarityScoreTextView.setText("Score unavailable");
                        Log.e(TAG, "Error parsing similarity score", e);
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

