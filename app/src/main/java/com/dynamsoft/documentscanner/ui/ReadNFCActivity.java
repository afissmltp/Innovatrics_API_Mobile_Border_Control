package com.dynamsoft.documentscanner.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.CustomerDataActivity;
import com.dynamsoft.documentscanner.R;
import com.dynamsoft.documentscanner.model.AdditionalPersonDetails;
import com.dynamsoft.documentscanner.model.DocType;
import com.dynamsoft.documentscanner.model.EDocument;
import com.dynamsoft.documentscanner.model.PersonDetails;
import com.dynamsoft.documentscanner.util.DateUtil;
import com.dynamsoft.documentscanner.util.Image;
import com.dynamsoft.documentscanner.util.ImageUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardSecurityFile;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG15File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import org.jmrtd.lds.iso19794.FingerImageInfo;
import org.jmrtd.lds.iso19794.FingerInfo;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ReadNFCActivity extends AppCompatActivity {

    private static final String TAG = ReadNFCActivity.class.getSimpleName();
    private NfcAdapter adapter;

    private View mainLayout;
    private View loadingLayout;
    private View imageLayout;
    private TextView tvResult;
    private ImageView ivPhoto;

    private String passportNumber, expirationDate, birthDate;
    private CustomerService customerService;
    private String customerId;
    private String mrzText = "N/A";
    private MaterialButton btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        customerService = new CustomerService();
        customerId = getIntent().getStringExtra("customerId");

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_title);

        mainLayout = findViewById(R.id.main_layout);
        loadingLayout = findViewById(R.id.loading_layout);
        imageLayout = findViewById(R.id.image_layout);
        ivPhoto = findViewById(R.id.view_photo);
        tvResult = findViewById(R.id.text_result);
        btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        ImageView imageView = findViewById(R.id.image_nfc);
        Glide.with(this) // 'this' peut être Activity ou Fragment
                .asGif()    // indique que c'est un GIF animé
                .load(R.drawable.nfcgif) // ton GIF dans res/drawable
                .into(imageView);


        adapter = NfcAdapter.getDefaultAdapter(this);

        if (adapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("NFC non supporté")
                    .setMessage("Votre appareil ne prend pas en charge la technologie NFC. Cette fonctionnalité n'est pas disponible.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false) // Empêche la fermeture de l'alerte
                    .show();
            return; // Arrête l'exécution si le NFC n'est pas supporté
        }

        fetchCustomerData();
    }

    private void setMrzData(MRZInfo mrzInfo) {
        passportNumber = mrzInfo.getDocumentNumber();
        expirationDate = mrzInfo.getDateOfExpiry();
        birthDate = mrzInfo.getDateOfBirth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }

    private void enableNfcForegroundDispatch() {
        if (adapter == null) {
            Toast.makeText(this, "NFC non supporté sur cet appareil", Toast.LENGTH_LONG).show();
            return;
        }

        if (!adapter.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Activer NFC")
                    .setMessage("Veuillez activer le NFC pour lire les documents.")
                    .setPositiveButton("Paramètres", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            return;
        }

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        String[][] techList = new String[][]{new String[]{"android.nfc.tech.IsoDep"}};
        adapter.enableForegroundDispatch(this, pendingIntent, null, techList);
        Log.d(TAG, "NFC foreground dispatch enabled.");
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null && Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {

                clearViews();

                if (passportNumber != null && !passportNumber.isEmpty()
                        && expirationDate != null && !expirationDate.isEmpty()
                        && birthDate != null && !birthDate.isEmpty()) {

                    BACKeySpec bacKey = new BACKey(passportNumber, birthDate, expirationDate);
                    new ReadTask(IsoDep.get(tag), bacKey).execute();

                    mainLayout.setVisibility(View.GONE);
                    imageLayout.setVisibility(View.GONE);
                    loadingLayout.setVisibility(View.VISIBLE);

                } else {
                    Snackbar.make(loadingLayout, "Veuillez attendre le chargement des données avant de scanner.", Snackbar.LENGTH_SHORT).show();
                    imageLayout.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private class ReadTask extends AsyncTask<Void, Void, Exception> {

        private IsoDep isoDep;
        private BACKeySpec bacKey;

        private ReadTask(IsoDep isoDep, BACKeySpec bacKey) {
            this.isoDep = isoDep;
            this.bacKey = bacKey;
        }

        EDocument eDocument = new EDocument();
        DocType docType = DocType.OTHER;
        PersonDetails personDetails = new PersonDetails();
        AdditionalPersonDetails additionalPersonDetails = new AdditionalPersonDetails();

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                PassportService service = new PassportService(cardService, PassportService.NORMAL_MAX_TRANCEIVE_LENGTH, PassportService.DEFAULT_MAX_BLOCKSIZE, true, false);
                service.open();

                boolean paceSucceeded = false;
                try {
                    CardSecurityFile cardSecurityFile = new CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY));
                    Collection<SecurityInfo> securityInfoCollection = cardSecurityFile.getSecurityInfos();
                    for (SecurityInfo securityInfo : securityInfoCollection) {
                        if (securityInfo instanceof PACEInfo) {
                            PACEInfo paceInfo = (PACEInfo) securityInfo;
                            service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
                            paceSucceeded = true;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                service.sendSelectApplet(paceSucceeded);
                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM).read();
                    } catch (Exception e) {
                        service.doBAC(bacKey);
                    }
                }

                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                DG1File dg1File = new DG1File(dg1In);

                MRZInfo mrzInfo = dg1File.getMRZInfo();
                personDetails.setName(mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
                personDetails.setSurname(mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
                personDetails.setPersonalNumber(mrzInfo.getPersonalNumber());
                personDetails.setGender(mrzInfo.getGender().toString());
                personDetails.setBirthDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfBirth()));
                personDetails.setExpiryDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfExpiry()));
                personDetails.setSerialNumber(mrzInfo.getDocumentNumber());
                personDetails.setNationality(mrzInfo.getNationality());
                personDetails.setIssuerAuthority(mrzInfo.getIssuingState());

                if ("I".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.ID_CARD;
                } else if ("P".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.PASSPORT;
                }

                CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
                DG2File dg2File = new DG2File(dg2In);

                List<FaceInfo> faceInfos = dg2File.getFaceInfos();
                List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
                for (FaceInfo faceInfo : faceInfos) {
                    allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
                }

                if (!allFaceImageInfos.isEmpty()) {
                    FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();
                    Image image = ImageUtil.getImage(ReadNFCActivity.this, faceImageInfo);
                    personDetails.setFaceImage(image.getBitmapImage());
                    personDetails.setFaceImageBase64(image.getBase64Image());
                }

                try {
                    CardFileInputStream dg3In = service.getInputStream(PassportService.EF_DG3);
                    DG3File dg3File = new DG3File(dg3In);

                    List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
                    List<FingerImageInfo> allFingerImageInfos = new ArrayList<>();
                    for (FingerInfo fingerInfo : fingerInfos) {
                        allFingerImageInfos.addAll(fingerInfo.getFingerImageInfos());
                    }

                    List<Bitmap> fingerprintsImage = new ArrayList<>();
                    for (FingerImageInfo fingerImageInfo : allFingerImageInfos) {
                        Image image = ImageUtil.getImage(ReadNFCActivity.this, fingerImageInfo);
                        fingerprintsImage.add(image.getBitmapImage());
                    }
                    personDetails.setFingerprints(fingerprintsImage);

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                try {
                    CardFileInputStream dg5In = service.getInputStream(PassportService.EF_DG5);
                    DG5File dg5File = new DG5File(dg5In);

                    List<DisplayedImageInfo> displayedImageInfos = dg5File.getImages();
                    if (!displayedImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = displayedImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(ReadNFCActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                try {
                    CardFileInputStream dg7In = service.getInputStream(PassportService.EF_DG7);
                    DG7File dg7File = new DG7File(dg7In);

                    List<DisplayedImageInfo> signatureImageInfos = dg7File.getImages();
                    if (!signatureImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = signatureImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(ReadNFCActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                try {
                    CardFileInputStream dg11In = service.getInputStream(PassportService.EF_DG11);
                    DG11File dg11File = new DG11File(dg11In);

                    if (dg11File.getLength() > 0) {
                        AdditionalPersonDetails addDetails = additionalPersonDetails;
                        addDetails.setCustodyInformation(dg11File.getCustodyInformation());
                        addDetails.setNameOfHolder(dg11File.getNameOfHolder());
                        addDetails.setFullDateOfBirth(dg11File.getFullDateOfBirth());
                        addDetails.setOtherNames(dg11File.getOtherNames());
                        addDetails.setOtherValidTDNumbers(dg11File.getOtherValidTDNumbers());
                        addDetails.setPermanentAddress(dg11File.getPermanentAddress());
                        addDetails.setPersonalNumber(dg11File.getPersonalNumber());
                        addDetails.setPersonalSummary(dg11File.getPersonalSummary());
                        addDetails.setPlaceOfBirth(dg11File.getPlaceOfBirth());
                        addDetails.setProfession(dg11File.getProfession());
                        addDetails.setProofOfCitizenship(dg11File.getProofOfCitizenship());
                        addDetails.setTag(dg11File.getTag());
                        addDetails.setTagPresenceList(dg11File.getTagPresenceList());
                        addDetails.setTelephone(dg11File.getTelephone());
                        addDetails.setTitle(dg11File.getTitle());
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                try {
                    CardFileInputStream dg15In = service.getInputStream(PassportService.EF_DG15);
                    DG15File dg15File = new DG15File(dg15In);
                    PublicKey publicKey = dg15File.getPublicKey();
                    eDocument.setDocPublicKey(publicKey);
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                eDocument.setDocType(docType);
                eDocument.setPersonDetails(personDetails);
                eDocument.setAdditionalPersonDetails(additionalPersonDetails);

            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            mainLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);

            if (exception == null) {
                setResultToView(eDocument);
            } else {
                Snackbar.make(mainLayout, exception.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setResultToView(EDocument eDocument) {
        Bitmap faceImage = ImageUtil.scaleImage(eDocument.getPersonDetails().getFaceImage());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        faceImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] faceImageBytes = stream.toByteArray();

        openCustomerDataActivity(
                faceImageBytes,
                eDocument.getPersonDetails().getName(),
                eDocument.getPersonDetails().getSurname(),
                eDocument.getPersonDetails().getPersonalNumber(),
                eDocument.getPersonDetails().getGender(),
                eDocument.getPersonDetails().getBirthDate(),
                eDocument.getPersonDetails().getExpiryDate(),
                eDocument.getPersonDetails().getSerialNumber(),
                eDocument.getPersonDetails().getNationality(),
                eDocument.getDocType().name(),
                eDocument.getPersonDetails().getIssuerAuthority()
        );
    }

    private void openCustomerDataActivity(byte[] faceImageBytes,
                                          String name,
                                          String surname,
                                          String personalNumber,
                                          String gender,
                                          String birthDate,
                                          String expiryDate,
                                          String serialNumber,
                                          String nationality,
                                          String docType,
                                          String issuerAuthority) {
        Intent intent = new Intent(this, CustomerDataActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("faceImage", faceImageBytes);

        intent.putExtra("name", name);
        intent.putExtra("surname", surname);
        intent.putExtra("personalNumber", personalNumber);
        intent.putExtra("gender", gender);
        intent.putExtra("birthDate", birthDate);
        intent.putExtra("expiryDate", expiryDate);
        intent.putExtra("serialNumber", serialNumber);
        intent.putExtra("nationality", nationality);
        intent.putExtra("docType", docType);
        intent.putExtra("issuerAuthority", issuerAuthority);

        intent.putExtra("openFragmentIndex", 3);
        startActivity(intent);
    }

    private void clearViews() {
        ivPhoto.setImageBitmap(null);
        tvResult.setText("");
    }

    private void fetchCustomerData() {
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Customer data fetched: " + response.toString());
                    //Toast.makeText(ReadNFCActivity.this, "Customer data loaded.", Toast.LENGTH_SHORT).show();
                    displayMRZ(response);
                    enableNfcForegroundDispatch();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ReadNFCActivity.this,
                            "Error fetching customer data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to fetch customer data", e);
                });
            }
        });
    }

    private void displayMRZ(JSONObject customerData) {
        try {
            JSONObject customer = customerData.getJSONObject("customer");
            JSONObject document = customer.getJSONObject("document");

            JSONObject additionalTexts = document.optJSONObject("additionalTexts");
            if (additionalTexts != null) {
                JSONObject machineReadableZone = additionalTexts.optJSONObject("machineReadableZone");
                if (machineReadableZone != null && machineReadableZone.has("visualZone")) {
                    mrzText = machineReadableZone.getString("visualZone");
                    MRZInfo mrzInfo = new MRZInfo(mrzText);
                    setMrzData(mrzInfo);
                }
            }
            imageLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.GONE);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error extracting data from JSON", Toast.LENGTH_SHORT).show();
        }
    }
}