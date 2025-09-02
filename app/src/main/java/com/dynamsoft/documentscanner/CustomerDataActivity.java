package com.dynamsoft.documentscanner;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.model.SessionData;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.google.android.material.tabs.TabLayout;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerDataActivity extends AppCompatActivity {
    private static final String TAG = "CustomerDataActivity";
    private TextView tvName, tvGender, tvSurname, tvAge;
    private CustomerService customerService;
    private ImageView imageView;
    private String customerId;
    private JSONObject customerData;
    public static Bitmap portraitBitmap = null;
    public static Bitmap rectoBitmap = null;
    public static Bitmap selfieBitmap = null;
    public static Double similarityScore = null;
    public static Integer rfidSimilarityScore = null;
    public static Bitmap rfidBitmap;
    private final String[] PAGE_TITLES = new String[]{
            "INFO",
            "Images",
            "Check",
            "DONNÉES RFID"
    };
    private Fragment[] PAGES;
    private ViewPager mViewPager;
    private String expirationStatusText, mrzStatusText, printCopyStatusText, textConsistencyStatusText, ocrConfidenceStatusText, screenshotStatusText, genderComparisonStatus, ageComparisonStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customerdata);

        customerService = new CustomerService();

        initializeViews();

        customerId = getIntent().getStringExtra("customerId");
       /* expirationStatusText = getIntent().getStringExtra("expirationStatus");
        mrzStatusText = getIntent().getStringExtra("mrzStatus");
        printCopyStatusText = getIntent().getStringExtra("printCopyStatus");
        textConsistencyStatusText = getIntent().getStringExtra("textConsistencyStatus");
        ocrConfidenceStatusText = getIntent().getStringExtra("ocrConfidenceStatus");
        screenshotStatusText = getIntent().getStringExtra("screenshotStatus");
        ageComparisonStatus = getIntent().getStringExtra("ageComparison");
        genderComparisonStatus = getIntent().getStringExtra("genderComparison");*/

        SessionData data = SessionData.getInstance();
        expirationStatusText = data.expirationStatus;
        mrzStatusText = data.mrzStatus;
        printCopyStatusText = data.printCopyStatus;
        textConsistencyStatusText = data.textConsistencyStatus;
        ocrConfidenceStatusText = data.ocrConfidenceStatus;
        screenshotStatusText = data.screenshotStatus;
        ageComparisonStatus = data.ageComparison;
        genderComparisonStatus = data.genderComparison;

        if (customerId != null && !customerId.isEmpty()) {
            if (savedInstanceState == null) {
                fetchCustomerData();
                loadDocumentPortrait();
            }
        } else {
            Toast.makeText(this, "No customer ID available. Cannot fetch data.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Customer ID is null or empty.");
            finish();
        }

        Toolbar toolbar = findViewById(R.id.simpleToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Contrôle identité");

        ImageButton homebtn = findViewById(R.id.homeButton);
        homebtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            Page3Fragment.clearSelfieAndScores();
            startActivity(intent);
        });

        ImageButton docScanButton = findViewById(R.id.docScanButton);
        docScanButton.setOnClickListener(v -> onScanButtonClicked());

        // Initialisation des fragments
        byte[] faceImageBytes = getIntent().getByteArrayExtra("faceImage");
        String name = getIntent().getStringExtra("name");
        String surname = getIntent().getStringExtra("surname");
        String gender = getIntent().getStringExtra("gender");
        if ("Male".equalsIgnoreCase(gender)) {
            gender = "Homme";
        } else if ("Female".equalsIgnoreCase(gender)) {
            gender = "Femme";
        }
        String birthDate = getIntent().getStringExtra("birthDate");
        String expiryDate = getIntent().getStringExtra("expiryDate");
        String serialNumber = getIntent().getStringExtra("serialNumber");
        String nationality = getIntent().getStringExtra("nationality");
        String docType = getIntent().getStringExtra("docType");
        if (docType != null && !docType.isEmpty() && "PASSPORT".equalsIgnoreCase(docType)) {
            docType = "Passeport";
        }
        String issuerAuthority = getIntent().getStringExtra("issuerAuthority");

        Page4Fragment page4Fragment = new Page4Fragment();
        Bundle bundle = new Bundle();
        bundle.putByteArray("faceImage", faceImageBytes);
        bundle.putString("name", name);
        bundle.putString("surname", surname);
        bundle.putString("gender", gender);
        bundle.putString("birthDate", birthDate);
        bundle.putString("expiryDate", expiryDate);
        bundle.putString("serialNumber", serialNumber);
        bundle.putString("nationality", nationality);
        bundle.putString("docType", docType);
        bundle.putString("issuerAuthority", issuerAuthority);
        bundle.putString("customerId", customerId);
        page4Fragment.setArguments(bundle);

        Page2Fragment page2Fragment = Page2Fragment.newInstance(customerId, faceImageBytes);
        Page3Fragment page3Fragment = Page3Fragment.newInstance(customerId, faceImageBytes);

        PAGES = new Fragment[]{
                new Page1Fragment(),
                page2Fragment,
                page3Fragment,
                page4Fragment
        };

        setupViewPager();

        int fragmentIndex = getIntent().getIntExtra("openFragmentIndex", 0);
        mViewPager.setCurrentItem(fragmentIndex);

        findViewById(R.id.readNfcBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReadNFCActivity.class);
            intent.putExtra("customerId", customerId);
            startActivityForResult(intent, 1001);
        });

        findViewById(R.id.selfieBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelfieCameraActivity.class);
            intent.putExtra("customerId", customerId);
            intent.putExtra("parentActivity", "CustomerDataActivity");
            startActivityForResult(intent, 1001);
        });

        boolean showPage3 = getIntent().getBooleanExtra("showPage3Fragment", false);
        if (showPage3) {
            showPage3Fragment();
        }

        imageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            showImageFullscreen(bitmap);
        });

        ImageButton btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnGeneratePdf.setOnClickListener(v -> {
            Log.d("PDF_FLOW", "Bouton PDF cliqué");

            if (areAllDataReady()) {
                Log.d("PDF_FLOW", "Toutes les données sont prêtes");

                // Afficher un indicateur de chargement
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Génération du PDF en cours...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                Log.d("PDF_FLOW", "ProgressDialog affiché");

                new Thread(() -> {
                    Log.d("PDF_FLOW", "Début génération PDF en background thread");
                    File pdfFile = generateAndExportPdf(customerData);

                    runOnUiThread(() -> {
                        Log.d("PDF_FLOW", "Retour sur UI thread, fermeture ProgressDialog");
                        progressDialog.dismiss();

                        if (pdfFile != null && pdfFile.exists()) {
                            Log.d("PDF_FLOW", "PDF généré avec succès: " + pdfFile.getAbsolutePath());
                            Log.d("PDF_FLOW", "Taille du fichier: " + pdfFile.length() + " bytes");
                            Toast.makeText(this, "PDF généré avec succès", Toast.LENGTH_SHORT).show();

                            Log.d("PDF_FLOW", "Tentative d'ouverture de WhatsApp");
                            openWhatsAppWithPdf(pdfFile);
                        } else {
                            Log.e("PDF_FLOW", "Échec de la génération du PDF - fichier null ou inexistant");
                            Toast.makeText(this, "Échec de la génération du PDF", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();

            } else {
                Log.w("PDF_FLOW", "Données client pas toutes prêtes");
                Toast.makeText(this, "Les données client ne sont pas encore toutes prêtes.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void openWhatsAppWithPdf(File pdfFile) {
        try {
            Log.d("WHATSAPP_FLOW", "Début d'ouverture de WhatsApp");
            Log.d("WHATSAPP_FLOW", "Fichier à partager: " + pdfFile.getAbsolutePath());

            if (pdfFile == null || !pdfFile.exists()) {
                Log.e("WHATSAPP_FLOW", "Fichier PDF non trouvé ou null");
                Toast.makeText(this, "Fichier PDF non trouvé", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );
            Log.d("WHATSAPP_FLOW", "URI générée: " + pdfUri.toString());

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Partager le PDF via"));

            Log.d("WHATSAPP_FLOW", "Intent créé, vérification de WhatsApp...");

            // Vérifier si WhatsApp est installé
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                Log.d("WHATSAPP_FLOW", "WhatsApp trouvé, lancement de l'activité");
                Log.d("WHATSAPP_FLOW", "L'application va quitter pour ouvrir WhatsApp");
                startActivity(shareIntent);
                Log.d("WHATSAPP_FLOW", "Activity lancée - WhatsApp devrait s'ouvrir");
            } else {
                Log.w("WHATSAPP_FLOW", "WhatsApp n'est pas installé");
                Toast.makeText(this, "WhatsApp n'est pas installé", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("WHATSAPP_FLOW", "Erreur lors de l'ouverture de WhatsApp", e);
            Toast.makeText(this, "Erreur lors du partage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void onScanButtonClicked() {
        // Clear the cache BEFORE launching the new scan activity.
        // This ensures Page1Fragment will perform a new API call for the updated image.
        Page1Fragment.clearDocumentImageCache();
        Page2Fragment.clearDocumentImageCache();
        Page3Fragment.clearSelfieAndScores();

        Intent intent = new Intent(this, CameraActivity2.class);
        intent.putExtra("customerId", customerId);
        startActivity(intent);
    }

    public void showPage3Fragment() {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(2, true);
        }
    }

    public String getCustomerId() {
        return customerId;
    }

    private void setupViewPager() {
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(), PAGES, PAGE_TITLES));

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0 && customerData != null) {
                    updateFragmentData(customerData);
                }
            }
        });
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tvName);
        tvSurname = findViewById(R.id.tvSurname);
        tvGender = findViewById(R.id.tvGender);
        imageView = findViewById(R.id.imageView);
        tvAge = findViewById(R.id.tvAge);
    }

    private void showImageFullscreen(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        ImageView imageView = dialog.findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(bitmap);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String imagePath = data.getStringExtra("imagePath");
            if (imagePath != null) {
                Bitmap selfie = handleRotation(imagePath);
                if (selfie != null) {
                    CustomerDataActivity.selfieBitmap = selfie;
                    showPage3Fragment();
                    Fragment fragment = PAGES[2];
                    if (fragment instanceof Page3Fragment) {
                        ((Page3Fragment) fragment).refreshSelfieAndUpload(selfie);
                    }
                }
            }
        }
    }

    private Bitmap handleRotation(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
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
                    return bitmap;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void fetchCustomerData() {
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    customerData = response;
                    Log.d(TAG, "Customer data fetched: " + response.toString());
                    Toast.makeText(CustomerDataActivity.this, "Customer data loaded.", Toast.LENGTH_SHORT).show();
                    try {
                        displayCustomerData(response);
                        updateFragmentData(response);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing or displaying customer data", e);
                        Toast.makeText(CustomerDataActivity.this, "Error parsing customer data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerDataActivity.this, "Error fetching customer data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to fetch customer data", e);
                });
            }
        });
    }

    private void updateFragmentData(JSONObject response) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + R.id.viewpager + ":0");
        if (fragment instanceof Page1Fragment) {
            ((Page1Fragment) fragment).updateCustomerData(response);
        }
        if (mViewPager.getCurrentItem() == 0) {
            mViewPager.getAdapter().notifyDataSetChanged();
        }
    }

    private void displayCustomerData(JSONObject customerData) {
        try {
            JSONObject customer = customerData.getJSONObject("customer");
            JSONObject dateOfBirth = customer.optJSONObject("dateOfBirth");
            if (dateOfBirth != null && dateOfBirth.has("mrz")) {
                String dobString = dateOfBirth.getString("mrz");
                int age = calculateAge(dobString);
                tvAge.setText(age + " ans");
            } else {
                tvAge.setText("N/A");
            }
            JSONObject givenNames = customer.optJSONObject("givenNames");
            String name = (givenNames != null && givenNames.has("mrz")) ? givenNames.getString("mrz") : "N/A";
            JSONObject surname = customer.optJSONObject("surname");
            String surName = (surname != null && surname.has("mrz")) ? surname.getString("mrz") : "N/A";
            JSONObject gender = customer.optJSONObject("gender");
            String gen = (gender != null && gender.has("mrz")) ? gender.getString("mrz") : "N/A";
            tvName.setText(name);
            tvSurname.setText(surName);
            tvGender.setText("M".equals(gen) ? "Homme" : "F".equals(gen) ? "Femme" : "N/A");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error extracting data from JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDocumentPortrait() {
        try {
            Log.d(TAG, "Attempting to load document portrait for customerId: " + customerId);
            customerService.getDocumentPortrait(customerId, new CustomerService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    String base64Image = response.optString("data");
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        try {
                            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            handler.post(() -> {
                                if (bitmap != null) {
                                    portraitBitmap = bitmap;
                                    imageView.setImageBitmap(bitmap);
                                    Toast.makeText(CustomerDataActivity.this, "Image portrait du document chargée avec succès.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(CustomerDataActivity.this, "Erreur de décodage du portrait du document.", Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Erreur: Le bitmap du portrait du document est null après décodage.");
                                }
                            });
                        } catch (Exception e) {
                            handler.post(() -> {
                                Toast.makeText(CustomerDataActivity.this, "Erreur lors du traitement de l'image du portrait du document.", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Erreur traitement portrait: " + e.getMessage(), e);
                            });
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Erreur API portrait : " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(CustomerDataActivity.this, "Erreur réseau (portrait) : " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating document portrait loading: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading document portrait: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {
        private final Fragment[] fragments;
        private final String[] titles;

        public MyPagerAdapter(FragmentManager fm, Fragment[] fragments, String[] titles) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = fragments;
            this.titles = titles;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0 && customerData != null) {
                return Page1Fragment.newInstance(customerData, customerId);
            }
            if (position == 1) {
                byte[] faceImageBytes = getIntent().getByteArrayExtra("faceImage");
                return Page2Fragment.newInstance(customerId, faceImageBytes);
            }
            return fragments[position];
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    private int calculateAge(String dobString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dob = sdf.parse(dobString);
            Calendar today = Calendar.getInstance();
            Calendar birthDate = Calendar.getInstance();
            birthDate.setTime(dob);
            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    protected void onDestroy() {
        if (selfieBitmap != null && !selfieBitmap.isRecycled()) {
            selfieBitmap.recycle();
            selfieBitmap = null;
        }

        similarityScore = null;
        rfidSimilarityScore = null;

        SessionData.clear(); // Les données sont supprimées quand l'activité est détruite

        super.onDestroy();
    }

    private File generateAndExportPdf(JSONObject apiResponse) {
        File file = null;

        try {
            JSONObject customer = apiResponse.getJSONObject("customer");
            JSONObject document = customer.getJSONObject("document");

            // Extraction des données du JSON pour la première page
            String name = customer.optJSONObject("givenNames").optString("mrz", "N/A");
            String surname = customer.optJSONObject("surname").optString("mrz", "N/A");
            String gender = customer.optJSONObject("gender").optString("mrz", "N/A").equals("M") ? "Homme" : "Femme";
            String dateOfBirth = customer.optJSONObject("dateOfBirth").optString("mrz", "N/A");
            String nationality = customer.optJSONObject("nationality").optString("mrz", "N/A");
            String documentNumber = document.optJSONObject("documentNumber").optString("mrz", "N/A");
            String dateOfExpiry = document.optJSONObject("dateOfExpiry").optString("mrz", "N/A");
            String issuingAuthority = document.optJSONObject("issuingAuthority").optString("mrz", "N/A");
            String docType = document.optJSONObject("type").optString("machineReadableTravelDocument", "N/A");

            if ("td3".equalsIgnoreCase(docType)) {
                docType = "Passeport";
            } else if ("td1".equalsIgnoreCase(docType)) {
                docType = "Carte d'identité";
            } else if ("td2".equalsIgnoreCase(docType)) {
                docType = "Document TD2";
            }

            PdfDocument pdfDocument = new PdfDocument();

            // Couleurs modernes
            int primaryColor = Color.rgb(59, 89, 152);
            int secondaryColor = Color.rgb(120, 140, 180);
            int textColor = Color.rgb(50, 50, 50);
            int lightGray = Color.rgb(240, 240, 240);
            int successColor = Color.rgb(46, 125, 50);
            int warningColor = Color.rgb(237, 108, 0);
            int errorColor = Color.rgb(198, 40, 40);

            // Création des styles
            Paint headerPaint = new Paint();
            headerPaint.setTextSize(28);
            headerPaint.setFakeBoldText(true);
            headerPaint.setColor(primaryColor);

            Paint sectionPaint = new Paint();
            sectionPaint.setTextSize(20);
            sectionPaint.setFakeBoldText(true);
            sectionPaint.setColor(secondaryColor);

            Paint textPaint = new Paint();
            textPaint.setTextSize(16);
            textPaint.setColor(textColor);

            Paint labelPaint = new Paint();
            labelPaint.setTextSize(16);
            labelPaint.setFakeBoldText(true);
            labelPaint.setColor(textColor);

            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(lightGray);
            backgroundPaint.setStyle(Paint.Style.FILL);

            Paint scorePaint = new Paint();
            scorePaint.setTextSize(18);
            scorePaint.setFakeBoldText(true);

            Paint linePaint = new Paint();
            linePaint.setColor(Color.LTGRAY);
            linePaint.setStrokeWidth(1);

            int contentWidth = 515;
            int pageMargin = 40;
            int currentPage = 1;

            // --- PAGE 1 : Infos de base ---
            PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(595, 842, currentPage).create();
            PdfDocument.Page page1 = pdfDocument.startPage(pageInfo1);
            Canvas canvas = page1.getCanvas();
            int x = pageMargin;
            int y = 60;

            canvas.drawText("Résultat d'inspection", x, y, headerPaint);
            y += 40;

            if (portraitBitmap != null) {
                canvas.drawText("Portrait du document", x, y, sectionPaint);
                y += 20;
                Bitmap scaledPortrait = Bitmap.createScaledBitmap(portraitBitmap, 155, 155, true);
                canvas.drawBitmap(scaledPortrait, x, y, null);
                y += 160;
            }

            canvas.drawRect(x, y, x + contentWidth, y + 30, backgroundPaint);
            canvas.drawText("Informations Personnelles", x + 10, y + 22, sectionPaint);
            y += 40;
            canvas.drawText("Nom complet: ", x, y, labelPaint);
            canvas.drawText(name + " " + surname, x + 150, y, textPaint); y += 25;
            canvas.drawText("Sexe: ", x, y, labelPaint);
            canvas.drawText(gender, x + 150, y, textPaint); y += 25;
            canvas.drawText("Date de naissance: ", x, y, labelPaint);
            canvas.drawText(dateOfBirth, x + 150, y, textPaint); y += 25;
            canvas.drawText("Nationalité: ", x, y, labelPaint);
            canvas.drawText(nationality, x + 150, y, textPaint); y += 40;

            canvas.drawRect(x, y, x + contentWidth, y + 30, backgroundPaint);
            canvas.drawText("Informations du Document", x + 10, y + 22, sectionPaint);
            y += 40;
            canvas.drawText("Type de document: " + docType, x, y, textPaint); y += 25;
            canvas.drawText("Numéro: " + documentNumber, x, y, textPaint); y += 25;
            canvas.drawText("Expiration: " + dateOfExpiry, x, y, textPaint); y += 25;
            canvas.drawText("Autorité: " + issuingAuthority, x, y, textPaint); y += 40;

            if (rectoBitmap != null) {
                canvas.drawText("Document", x, y, sectionPaint);
                y += 20;
                float aspectRatio = (float) rectoBitmap.getWidth() / rectoBitmap.getHeight();
                int docWidth = 300;
                int docHeight = (int) (docWidth / aspectRatio);
                if (docHeight > 200) {
                    docHeight = 200;
                    docWidth = (int) (docHeight * aspectRatio);
                }
                Bitmap scaledRecto = Bitmap.createScaledBitmap(rectoBitmap, docWidth, docHeight, true);
                canvas.drawBitmap(scaledRecto, x, y, null);
            }

            pdfDocument.finishPage(page1);

            // --- PAGE 2 : Cohérence + inspection ---
            currentPage++;
            PdfDocument.PageInfo pageInfo2 = new PdfDocument.PageInfo.Builder(595, 842, currentPage).create();
            PdfDocument.Page page2 = pdfDocument.startPage(pageInfo2);
            canvas = page2.getCanvas();
            x = pageMargin; y = 60;

            // Section : Vérification de cohérence (MRZ - Portrait)

            canvas.drawRect(x, y, x + contentWidth, y + 30, backgroundPaint);
            canvas.drawText("Vérification de cohérence (MRZ - Portrait)", x + 10, y + 22, sectionPaint);
            y += 40;

            canvas.drawText("Comparaison âge : " + (ageComparisonStatus != null ? ageComparisonStatus : "N/A"), x, y, textPaint);
            y += 25;

            canvas.drawText("Comparaison genre : " + (genderComparisonStatus != null ? genderComparisonStatus : "N/A"), x, y, textPaint);
            y += 40;

// Section : Statuts d’inspection du document

            canvas.drawRect(x, y, x + contentWidth, y + 30, backgroundPaint);
            canvas.drawText("Statuts d'inspection du documen", x + 10, y + 22, sectionPaint);
            y += 40;

            canvas.drawText("Expiration : " + (expirationStatusText != null ? expirationStatusText : "N/A"), x, y, textPaint);
            y += 25;

            canvas.drawText("MRZ : " + (mrzStatusText != null ? mrzStatusText : "N/A"), x, y, textPaint);
            y += 25;

            canvas.drawText("Copie imprimée : " + (printCopyStatusText != null ? printCopyStatusText : "N/A"), x, y, textPaint);
            y += 25;

            canvas.drawText("Cohérence du texte : " + (textConsistencyStatusText != null ? textConsistencyStatusText : "N/A"), x, y, textPaint);
            y += 25;

            canvas.drawText("Confiance OCR : " + (ocrConfidenceStatusText != null ? ocrConfidenceStatusText : "N/A"), x, y, textPaint);
            y += 25;

            canvas.drawText("Capture d'écran : " + (screenshotStatusText != null ? screenshotStatusText : "N/A"), x, y, textPaint);
            y += 25;

            pdfDocument.finishPage(page2);

            // --- PAGE 3 : Comparaison Portrait/Selfie et RFID/Selfie ---
            currentPage++;
            PdfDocument.PageInfo pageInfo3 = new PdfDocument.PageInfo.Builder(595, 842, currentPage).create();
            PdfDocument.Page page3 = pdfDocument.startPage(pageInfo3);
            canvas = page3.getCanvas();
            x = pageMargin; y = 60;

            Page3Fragment page3Fragment = (Page3Fragment) mViewPager.getAdapter().instantiateItem(mViewPager, 2);
            String portraitSelfieScore = "N/A";
            String rfidSelfieScore = "N/A";
            if (page3Fragment != null) {
                JSONObject page3Data = page3Fragment.getPdfData();
                portraitSelfieScore = page3Data.optString("portraitSelfieScore", "N/A");
                rfidSelfieScore = page3Data.optString("rfidSelfieScore", "N/A");
            }

            canvas.drawText("Comparaison Portrait Document vs Selfie", x, y, headerPaint); y += 40;
            canvas.drawText("Score: " + portraitSelfieScore, x, y, scorePaint); y += 30;

            if (CustomerDataActivity.portraitBitmap != null) {
                Bitmap scaledPortrait = Bitmap.createScaledBitmap(CustomerDataActivity.portraitBitmap,150,200,true);
                canvas.drawBitmap(scaledPortrait, x, y, null);
                canvas.drawText("Portrait", x, y+220, textPaint);
            }
            if (CustomerDataActivity.selfieBitmap != null) {
                Bitmap scaledSelfie = Bitmap.createScaledBitmap(CustomerDataActivity.selfieBitmap,150,200,true);
                canvas.drawBitmap(scaledSelfie, x+200, y, null);
                canvas.drawText("Selfie", x+200, y+220, textPaint);
            }
            y += 260;

            canvas.drawLine(x, y, x+contentWidth, y, linePaint); y += 20;

            canvas.drawText("Comparaison Photo RFID vs Selfie", x, y, headerPaint); y += 40;
            canvas.drawText("Score: " + rfidSelfieScore, x, y, scorePaint); y += 30;

            if (CustomerDataActivity.rfidBitmap != null) {
                Bitmap scaledRfid = Bitmap.createScaledBitmap(CustomerDataActivity.rfidBitmap,150,200,true);
                canvas.drawBitmap(scaledRfid, x, y, null);
                canvas.drawText("Photo RFID", x, y+220, textPaint);
            }
            if (CustomerDataActivity.selfieBitmap != null) {
                Bitmap scaledSelfie = Bitmap.createScaledBitmap(CustomerDataActivity.selfieBitmap,150,200,true);
                canvas.drawBitmap(scaledSelfie, x+200, y, null);
                canvas.drawText("Selfie", x+200, y+220, textPaint);
            }

            pdfDocument.finishPage(page3);

            // --- PAGE 4 : Comparaison RFID vs Portrait Document ---
            currentPage++;
            PdfDocument.PageInfo pageInfo4 = new PdfDocument.PageInfo.Builder(595, 842, currentPage).create();
            PdfDocument.Page page4 = pdfDocument.startPage(pageInfo4);
            canvas = page4.getCanvas();
            x = pageMargin;
            y = 60;

            Page4Fragment page4Fragment = (Page4Fragment) mViewPager.getAdapter().instantiateItem(mViewPager, 3);
            String facialSimilarityScore = "N/A";
            JSONObject rfidData = null;
            JSONObject mrzData = null;

            if (page4Fragment != null) {
                JSONObject page4Data = page4Fragment.getPdfData();
                rfidData = page4Data.optJSONObject("rfidData");
                mrzData = page4Data.optJSONObject("mrzData");
                facialSimilarityScore = page4Data.optString("facialSimilarityScore", "N/A");
            }

// Titre
            canvas.drawText("Comparaison Photo RFID vs Portrait Document", x, y, headerPaint);
            y += 40;

// Affichage des images
            int imgWidth = 200;
            int imgHeight = 250;
            if (CustomerDataActivity.rfidBitmap != null) {
                Bitmap scaledRfid = Bitmap.createScaledBitmap(CustomerDataActivity.rfidBitmap, imgWidth, imgHeight, true);
                canvas.drawBitmap(scaledRfid, x, y, null);
                canvas.drawText("Photo RFID", x, y + imgHeight + 20, textPaint);
            }

            if (CustomerDataActivity.portraitBitmap != null) {
                Bitmap scaledPortrait = Bitmap.createScaledBitmap(CustomerDataActivity.portraitBitmap, imgWidth, imgHeight, true);
                canvas.drawBitmap(scaledPortrait, x + 250, y, null);
                canvas.drawText("Portrait Document", x + 250, y + imgHeight + 20, textPaint);
            }

            y += imgHeight + 60; // Décalage après les images

// Score de similarité
            canvas.drawText("Score de similarité faciale : " + facialSimilarityScore, x, y, scorePaint);
            y += 40;

// --- Tableau comparatif RFID vs MRZ ---
            if (rfidData != null && mrzData != null) {
                int col1X = x;
                int col2X = x + 200;
                int col3X = x + 400;
                int tableY = y;

                canvas.drawRect(col1X, tableY, col3X + 100, tableY + 30, backgroundPaint);
                canvas.drawText("Champ", col1X + 10, tableY + 20, labelPaint);
                canvas.drawText("RFID", col2X + 10, tableY + 20, labelPaint);
                canvas.drawText("MRZ", col3X + 10, tableY + 20, labelPaint);
                tableY += 35;

                String[] fields = {"Nom","Prénom","Sexe","Nationalité","Naissance","Numéro","Expiration","Autorité","Type"};
                String[] keys = {"surname","givenName","gender","nationality","dateOfBirth","documentNumber","dateOfExpiry","issuerAuthority","docType"};

                for (int i = 0; i < fields.length; i++) {
                    String rfidVal = rfidData.optString(keys[i], "N/A");
                    String mrzVal = mrzData.optString(keys[i], "N/A");
                    drawTableRow(canvas, textPaint, col1X + 10, col2X + 10, col3X + 10, fields[i], rfidVal, mrzVal, tableY + 18);
                    tableY += 25;
                }
            }

            pdfDocument.finishPage(page4);

            // --- Sauvegarde ---
            file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "fiche_client_" + surname + "_" + System.currentTimeMillis() + ".pdf"
            );
            try {
                pdfDocument.writeTo(new FileOutputStream(file));

            } catch (IOException e) {
                Log.e("PDF_GENERATION","Error writing PDF",e);
            }
            pdfDocument.close();

        } catch (JSONException e) {
            Log.e("JSON_PARSING","Error parsing JSON",e);
        }
        return file;
    }

    private void drawTableRow(Canvas canvas, Paint paint, int col1X, int col2X, int col3X,
                              String label, String dataRfid, String dataMrz, int y) {
        canvas.drawText(label, col1X, y, paint);
        Paint dataPaint = new Paint(paint);
        if (dataRfid.equals(dataMrz)) {
            dataPaint.setColor(Color.rgb(46, 125, 50));
        } else if (!"N/A".equals(dataRfid) && !"N/A".equals(dataMrz)) {
            dataPaint.setColor(Color.rgb(198, 40, 40));
        }
        canvas.drawText(dataRfid, col2X, y, dataPaint);
        canvas.drawText(dataMrz, col3X, y, dataPaint);
    }
    private boolean areAllDataReady() {
        return portraitBitmap != null &&
                rectoBitmap != null &&
                CustomerDataActivity.selfieBitmap != null &&
                CustomerDataActivity.rfidBitmap != null &&
                CustomerDataActivity.portraitBitmap != null &&

                expirationStatusText != null &&
                mrzStatusText != null &&
                printCopyStatusText != null &&
                textConsistencyStatusText != null &&
                ocrConfidenceStatusText != null &&
                screenshotStatusText != null &&
                ageComparisonStatus != null &&
                genderComparisonStatus != null;
    }
}
// You also need to add the getPdfData() method to your Page3Fragment.
// I recommend adding the method as shown in the previous response.}