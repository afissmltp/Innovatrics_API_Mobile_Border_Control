package com.dynamsoft.documentscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dynamsoft.core.basic_structures.CapturedResult;
import com.dynamsoft.core.basic_structures.Quadrilateral;
import com.dynamsoft.cvr.CaptureVisionRouter;
import com.dynamsoft.cvr.CaptureVisionRouterException;
import com.dynamsoft.cvr.SimplifiedCaptureVisionSettings;
import com.dynamsoft.ddn.NormalizedImageResultItem;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.jsibbold.zoomage.ZoomageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ViewerActivity extends AppCompatActivity {

    private ZoomageView normalizedImageView;
    private Point[] points;
    private Bitmap rawImage;
    private Bitmap normalized;
    private CaptureVisionRouter cvr;
    private int rotation = 0;

    private static final String[] WRITE_EXTERNAL_STORAGE_PERMISSION = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 10;
    private CustomerService customerService;
    private String customerId;
    private Button readDocBtn;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        customerService = new CustomerService();
        progressBar = findViewById(R.id.progressBar);
        readDocBtn = findViewById(R.id.readDocBtn);
        readDocBtn.setEnabled(false); // Disable until quality checked

        customerId = getIntent().getStringExtra("customerId");

        if (customerId == null || customerId.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            customerId = prefs.getString("customerId", null);
            if (customerId == null || customerId.isEmpty()) {
                Log.e("ViewerActivity", "Customer ID not found via Intent or SharedPreferences.");
                Toast.makeText(this, "Error: Customer ID not available.", Toast.LENGTH_LONG).show();
                finish(); // Close activity if no customer ID
                return;
            } else {
                Log.d("ViewerActivity", "Customer ID retrieved from SharedPreferences: " + customerId);
            }
        } else {
            Log.d("ViewerActivity", "Customer ID retrieved from Intent: " + customerId);
        }

        Button rotateButton = findViewById(R.id.rotateButton);
        Button saveImageButton = findViewById(R.id.saveImageButton);

        rotateButton.setOnClickListener(v -> {
            rotation = rotation + 90;
            if (rotation == 360) {
                rotation = 0;
            }
            normalizedImageView.setRotation(rotation);
        });

        saveImageButton.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                saveImage(normalized);
            } else {
                requestPermission();
            }
        });

        normalizedImageView = findViewById(R.id.normalizedImageView);

        cvr = new CaptureVisionRouter(ViewerActivity.this);
        try {
            cvr.initSettings("{\"CaptureVisionTemplates\": [{\"Name\": \"Default\"},{\"Name\": \"DetectDocumentBoundaries_Default\",\"ImageROIProcessingNameArray\": [\"roi-detect-document-boundaries\"]},{\"Name\": \"DetectAndNormalizeDocument_Default\",\"ImageROIProcessingNameArray\": [\"roi-detect-and-normalize-document\"]},{\"Name\": \"NormalizeDocument_Binary\",\"ImageROIProcessingNameArray\": [\"roi-normalize-document-binary\"]},  {\"Name\": \"NormalizeDocument_Gray\",\"ImageROIProcessingNameArray\": [\"roi-normalize-document-gray\"]},  {\"Name\": \"NormalizeDocument_Color\",\"ImageROIProcessingNameArray\": [\"roi-normalize-document-color\"]}],\"TargetROIDefOptions\": [{\"Name\": \"roi-detect-document-boundaries\",\"TaskSettingNameArray\": [\"task-detect-document-boundaries\"]},{\"Name\": \"roi-detect-and-normalize-document\",\"TaskSettingNameArray\": [\"task-detect-and-normalize-document\"]},{\"Name\": \"roi-normalize-document-binary\",\"TaskSettingNameArray\": [\"task-normalize-document-binary\"]},  {\"Name\": \"roi-normalize-document-gray\",\"TaskSettingNameArray\": [\"task-normalize-document-gray\"]},  {\"Name\": \"roi-normalize-document-color\",\"TaskSettingNameArray\": [\"task-normalize-document-color\"]}],\"DocumentNormalizerTaskSettingOptions\": [{\"Name\": \"task-detect-and-normalize-document\",\"SectionImageParameterArray\": [{\"Section\": \"ST_REGION_PREDETECTION\",\"ImageParameterName\": \"ip-detect-and-normalize\"},{\"Section\": \"ST_DOCUMENT_DETECTION\",\"ImageParameterName\": \"ip-detect-and-normalize\"},{\"Section\": \"ST_DOCUMENT_NORMALIZATION\",\"ImageParameterName\": \"ip-detect-and-normalize\"}]},{\"Name\": \"task-detect-document-boundaries\",\"TerminateSetting\": {\"Section\": \"ST_DOCUMENT_DETECTION\"},\"SectionImageParameterArray\": [{\"Section\": \"ST_REGION_PREDETECTION\",\"ImageParameterName\": \"ip-detect\"},{\"Section\": \"ST_DOCUMENT_DETECTION\",\"ImageParameterName\": \"ip-detect\"},{\"Section\": \"ST_DOCUMENT_NORMALIZATION\",\"ImageParameterName\": \"ip-detect\"}]},{\"Name\": \"task-normalize-document-binary\",\"StartSection\": \"ST_DOCUMENT_NORMALIZATION\",    \"ColourMode\": \"ICM_BINARY\",\"SectionImageParameterArray\": [{\"Section\": \"ST_REGION_PREDETECTION\",\"ImageParameterName\": \"ip-normalize\"},{\"Section\": \"ST_DOCUMENT_DETECTION\",\"ImageParameterName\": \"ip-normalize\"},{\"Section\": \"ST_DOCUMENT_NORMALIZATION\",\"ImageParameterName\": \"ip-normalize\"}]},  {\"Name\": \"task-normalize-document-gray\",    \"ColourMode\": \"ICM_GRAYSCALE\",\"StartSection\": \"ST_DOCUMENT_NORMALIZATION\",\"SectionImageParameterArray\": [{\"Section\": \"ST_REGION_PREDETECTION\",\"ImageParameterName\": \"ip-normalize\"},{\"Section\": \"ST_DOCUMENT_DETECTION\",\"ImageParameterName\": \"ip-normalize\"},{\"Section\": \"ST_DOCUMENT_NORMALIZATION\",\"ImageParameterName\": \"ip-normalize\"}]},  {\"Name\": \"task-normalize-document-color\",    \"ColourMode\": \"ICM_COLOUR\",\"StartSection\": \"ST_DOCUMENT_NORMALIZATION\",\"SectionImageParameterArray\": [{\"Section\": \"ST_REGION_PREDETECTION\",\"ImageParameterName\": \"ip-normalize\"},{\"Section\": \"ST_DOCUMENT_DETECTION\",\"ImageParameterName\": \"ip-normalize\"},{\"Section\": \"ST_DOCUMENT_NORMALIZATION\",\"ImageParameterName\": \"ip-normalize\"}]}],\"ImageParameterOptions\": [{\"Name\": \"ip-detect-and-normalize\",\"BinarizationModes\": [{\"Mode\": \"BM_LOCAL_BLOCK\",\"BlockSizeX\": 0,\"BlockSizeY\": 0,\"EnableFillBinaryVacancy\": 0}],\"TextDetectionMode\": {\"Mode\": \"TTDM_WORD\",\"Direction\": \"HORIZONTAL\",\"Sensitivity\": 7}},{\"Name\": \"ip-detect\",\"BinarizationModes\": [{\"Mode\": \"BM_LOCAL_BLOCK\",\"BlockSizeX\": 0,\"BlockSizeY\": 0,\"EnableFillBinaryVacancy\": 0,\"ThresholdCompensation\" : 7}],\"TextDetectionMode\": {\"Mode\": \"TTDM_WORD\",\"Direction\": \"HORIZONTAL\",\"Sensitivity\": 7},\"ScaleDownThreshold\" : 512},{\"Name\": \"ip-normalize\",\"BinarizationModes\": [{\"Mode\": \"BM_LOCAL_BLOCK\",\"BlockSizeX\": 0,\"BlockSizeY\": 0,\"EnableFillBinaryVacancy\": 0}],\"TextDetectionMode\": {\"Mode\": \"TTDM_WORD\",\"Direction\": \"HORIZONTAL\",\"Sensitivity\": 7}}]}");
        } catch (CaptureVisionRouterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to initialize CaptureVisionRouter: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        loadImageAndPoints();
        normalize();

        // Initiate the sequence: create document -> fetch quality -> enable button
        createDocumentFrontPage();

        readDocBtn.setOnClickListener(v -> {
            if (normalized != null ) {
                Intent intent = new Intent(ViewerActivity.this, CustomerDataActivity.class);
                intent.putExtra("customerId", customerId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Document not processed .", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches the quality analysis of the document front page from the API.
     * After fetching quality, it fetches customer data.
     */
    private void getDocQuality() {
        customerService.getQualityDocumentFrontPage(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject jsonData) {
                runOnUiThread(() -> {
                    try {
                        boolean isFine = jsonData.getBoolean("fine");
                        JSONObject details = jsonData.getJSONObject("details");

                        JSONObject sharpness = details.getJSONObject("sharpness");
                        double sharpnessScore = sharpness.getDouble("score");
                        String sharpnessLevel = sharpness.getString("level");

                        JSONObject brightness = details.getJSONObject("brightness");
                        double brightnessScore = brightness.getDouble("score");
                        String brightnessLevel = brightness.getString("level");

                        JSONObject hotspots = details.getJSONObject("hotspots");
                        double hotspotsScore = hotspots.getDouble("score");
                        String hotspotsLevel = hotspots.getString("level");

                        Log.d("QualityResult", "Document is fine: " + isFine);
                        Log.d("QualityResult", "Sharpness - Score: " + sharpnessScore + ", Level: " + sharpnessLevel);
                        Log.d("QualityResult", "Brightness - Score: " + brightnessScore + ", Level: " + brightnessLevel);
                        Log.d("QualityResult", "Hotspots - Score: " + hotspotsScore + ", Level: " + hotspotsLevel);

                        updateQualityUI(isFine, sharpnessScore, sharpnessLevel,
                                brightnessScore, brightnessLevel,
                                hotspotsScore, hotspotsLevel);

                        readDocBtn.setEnabled(true); // Enable button on success

                    } catch (JSONException e) {
                        Log.e("QualityError", "Error parsing JSON for quality data", e);
                        showError("Error parsing quality data");

                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e("QualityError", "API call failed for quality data", e);
                    showError("Failed to get quality data: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Updates the UI to display the document quality metrics.
     */
    private void updateQualityUI(boolean isFine,
                                 double sharpnessScore, String sharpnessLevel,
                                 double brightnessScore, String brightnessLevel,
                                 double hotspotsScore, String hotspotsLevel) {
        TextView qualityStatus = findViewById(R.id.qualityStatus);
        qualityStatus.setText(isFine ? "✅ Document quality is good" : "⚠️ Document quality issues detected");
        qualityStatus.setTextColor(isFine ?
                ContextCompat.getColor(this, R.color.green) :
                ContextCompat.getColor(this, R.color.orange));

        TextView sharpnessView = findViewById(R.id.sharpnessValue);
        sharpnessView.setText(String.format("%.2f (%s)", sharpnessScore, sharpnessLevel));
        applyQualityLevelStyle(sharpnessView, sharpnessLevel);

        TextView brightnessView = findViewById(R.id.brightnessValue);
        brightnessView.setText(String.format("%.2f (%s)", brightnessScore, brightnessLevel));
        applyQualityLevelStyle(brightnessView, brightnessLevel);

        TextView hotspotsView = findViewById(R.id.hotspotsValue);
        hotspotsView.setText(String.format("%.2f (%s)", hotspotsScore, hotspotsLevel));
        applyQualityLevelStyle(hotspotsView, hotspotsLevel);
    }

    /**
     * Applies a color style to a TextView based on the quality level.
     */
    private void applyQualityLevelStyle(TextView view, String level) {
        int colorRes;
        switch (level) {
            case "HIGH":
                colorRes = R.color.green;
                break;
            case "MEDIUM":
                colorRes = R.color.orange;
                break;
            case "LOW":
                colorRes = R.color.red;
                break;
            default:
                colorRes = R.color.black;
        }
        view.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    /**
     * Displays a Toast message with the given error message.
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Loads the raw image and corner points from the Intent.
     */
    private void loadImageAndPoints() {
        Uri uri = Uri.parse(getIntent().getStringExtra("imageUri"));
        try {
            rawImage = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        int bitmapWidth = getIntent().getIntExtra("bitmapWidth", 720);
        int bitmapHeight = getIntent().getIntExtra("bitmapHeight", 1280);
        Parcelable[] parcelables = getIntent().getParcelableArrayExtra("points");
        if (parcelables != null) {
            points = new Point[parcelables.length];
            for (int i = 0; i < parcelables.length; i++) {
                points[i] = (Point) parcelables[i];
                // Scale points to raw image dimensions
                points[i].x = points[i].x * rawImage.getWidth() / bitmapWidth;
                points[i].y = points[i].y * rawImage.getHeight() / bitmapHeight;
            }
        } else {
            Toast.makeText(this, "No document points found.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Normalizes the loaded raw image using Dynamsoft CaptureVisionRouter.
     */
    private void normalize() {
        if (rawImage == null || points == null) {
            Toast.makeText(this, "Raw image or points not loaded for normalization.", Toast.LENGTH_LONG).show();
            return;
        }
        String templateName = "NormalizeDocument_Color";
        try {
            Quadrilateral quad = new Quadrilateral();
            quad.points = points;
            SimplifiedCaptureVisionSettings settings = cvr.getSimplifiedSettings(templateName);
            settings.roi = quad;
            settings.roiMeasuredInPercentage = false;
            cvr.updateSettings(templateName, settings);
            CapturedResult capturedResult = cvr.capture(rawImage, templateName);
            if (capturedResult != null && capturedResult.getItems() != null && capturedResult.getItems().length > 0) {
                NormalizedImageResultItem result = (NormalizedImageResultItem) capturedResult.getItems()[0];
                normalized = result.getImageData().toBitmap();
                normalizedImageView.setImageBitmap(normalized);
            } else {
                Toast.makeText(this, "No normalized image result.", Toast.LENGTH_LONG).show();
                Log.e("ViewerActivity", "No normalized image result from CaptureVisionRouter.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to normalize image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Saves the given bitmap to external storage.
     */
    public void saveImage(Bitmap bmp) {
        if (bmp == null) {
            Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show();
            return;
        }
        File appDir = new File(this.getApplicationContext().getExternalFilesDir(""), "ddn");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
                rotated.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            }
            fos.flush();
            fos.close();
            Toast.makeText(ViewerActivity.this, "File saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks if the app has WRITE_EXTERNAL_STORAGE permission.
     */
    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests WRITE_EXTERNAL_STORAGE permission from the user.
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                WRITE_EXTERNAL_STORAGE_PERMISSION,
                WRITE_EXTERNAL_STORAGE_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage(normalized);
            } else {
                Toast.makeText(this, "Please grant the permission to write external storage.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Uploads the normalized document image to the backend.
     * After successful upload, it fetches document quality.
     */
    private void createDocumentFrontPage() {
        if (normalized == null) {
            Toast.makeText(this, "Normalized image not available for upload.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Customer ID is missing, cannot upload document.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = bitmapToBase64(normalized);

        try {
            JSONObject requestBody = new JSONObject();
            JSONObject imageObject = new JSONObject();
            imageObject.put("data", imageBase64);
            requestBody.put("image", imageObject);

            customerService.createDocumentFrontPage(customerId, requestBody, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        try {
                            Toast.makeText(ViewerActivity.this,
                                    "Document uploaded successfully: " + response.toString(2),
                                    Toast.LENGTH_LONG).show();
                            Log.d("UPLOAD_SUCCESS", response.toString(2));
                            // After successful document creation, call getDocQuality
                            getDocQuality();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ViewerActivity.this, "Error parsing upload response.", Toast.LENGTH_SHORT).show();
                            // Even if parsing fails, try to get quality and customer data
                            getDocQuality();
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ViewerActivity.this,
                                "Upload error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("UPLOAD_ERROR", "Upload failed", e);

                    });
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Converts a Bitmap to a Base64 encoded String.
     */
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}