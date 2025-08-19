package com.dynamsoft.documentscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;
import com.dynamsoft.documentscanner.ui.ReadNFCActivity;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerDataActivity extends AppCompatActivity {
    private static final String TAG = "CustomerDataActivity";

    private TextView tvName,tvGender,tvSurname,tvAge;
    private CustomerService customerService;
    private ImageView imageView;
    private String customerId;
    private JSONObject customerData;
    public static Bitmap portraitBitmap = null;
    public static Bitmap rectoBitmap = null;
    public static Bitmap selfieBitmap = null;
    public static Double similarityScore = null;

    private final String[] PAGE_TITLES = new String[] {
            "RÉSULTAT GLOBAL",
            "Images",
            "VÉRIFICATION",
            "DONNÉES RFID"
    };
    private Fragment[] PAGES;
    // The ViewPager is responsible for sliding pages (fragments) in and out upon user input
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customerdata);
        customerService = new CustomerService();
        initializeViews();

        customerId = getIntent().getStringExtra("customerId");

        if (customerId != null && !customerId.isEmpty()) {
            if (savedInstanceState == null) {
                fetchCustomerData();
                loadDocumentPortrait();
            }
        } else {
            Toast.makeText(this, "No customer ID available. Cannot fetch data.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Customer ID is null or empty.");
            finish(); //
        }


        // 📌 Récupérer les données reçues
        byte[] faceImageBytes = getIntent().getByteArrayExtra("faceImage");
        // Récupérer tous les champs individuels
        String name = getIntent().getStringExtra("name");
        String surname = getIntent().getStringExtra("surname");
        //String personalNumber = getIntent().getStringExtra("personalNumber");
        String gender = getIntent().getStringExtra("gender");
        String birthDate = getIntent().getStringExtra("birthDate");
        String expiryDate = getIntent().getStringExtra("expiryDate");
        String serialNumber = getIntent().getStringExtra("serialNumber");
        String nationality = getIntent().getStringExtra("nationality");
        String docType = getIntent().getStringExtra("docType");
        String issuerAuthority = getIntent().getStringExtra("issuerAuthority");

        // Créer Page4Fragment avec un Bundle contenant les données
        Page4Fragment page4Fragment = new Page4Fragment();
        Bundle bundle = new Bundle();
        bundle.putByteArray("faceImage", faceImageBytes);
        // Ajouter tous les champs individuels
        bundle.putString("name", name);
        bundle.putString("surname", surname);
        //bundle.putString("personalNumber", personalNumber);
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

        // 📌 Initialiser les fragments
        PAGES = new Fragment[] {
                new Page1Fragment(),
                page2Fragment,
                page3Fragment,
                page4Fragment
        };


        setupViewPager();

        // Récupérer l'index du fragment à afficher
        int fragmentIndex = getIntent().getIntExtra("openFragmentIndex", 0);
        mViewPager.setCurrentItem(fragmentIndex); // ouvre directement le fragment 4

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

        // Vérifier si on doit afficher Page3
        boolean showPage3 = getIntent().getBooleanExtra("showPage3Fragment", false);
        if (showPage3) {
            showPage3Fragment();
        }

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Bitmap selfie = data.getParcelableExtra("selfieBitmap");
            if (selfie != null) {
                CustomerDataActivity.selfieBitmap = selfie;
                showPage3Fragment();

                // Rafraîchir le fragment Page3
                Fragment fragment = PAGES[2];
                if (fragment instanceof Page3Fragment) {
                    ((Page3Fragment) fragment).refreshSelfieAndUpload(selfie);
                }
            }
        }
    }
    private void fetchCustomerData() {
        customerService.getCustomer(customerId, new CustomerService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    customerData = response; // Stocker les données
                    Log.d(TAG, "Customer data fetched: " + response.toString());
                    Toast.makeText(CustomerDataActivity.this, "Customer data loaded.", Toast.LENGTH_SHORT).show();
                    try {
                        displayCustomerData(response);
                        updateFragmentData(response);
                        //fetchSimilarityScore();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing or displaying customer data", e);
                        Toast.makeText(CustomerDataActivity.this, "Error parsing customer data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerDataActivity.this,
                            "Error fetching customer data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to fetch customer data", e);
                });
            }
        });
    }
    private void updateFragmentData(JSONObject response) {
        // Trouver le fragment existant
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + R.id.viewpager + ":0");

        // Si le fragment existe, le mettre à jour
        if (fragment instanceof Page1Fragment) {
            ((Page1Fragment) fragment).updateCustomerData(response);
        }

        // Si on est sur la page 1, recréer le fragment pour forcer le rafraîchissement
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
                // Convertir la date de naissance en âge
                int age = calculateAge(dobString);
                tvAge.setText(age + " ans");
            } else {
                tvAge.setText("N/A");
            }

            // Champs de base (nom, prénom, etc.)
            JSONObject givenNames = customer.optJSONObject("givenNames");
            String name = (givenNames != null && givenNames.has("mrz")) ? givenNames.getString("mrz") : "N/A";

            JSONObject surname = customer.optJSONObject("surname");
            String surName = (surname != null && surname.has("mrz")) ? surname.getString("mrz") : "N/A";

            JSONObject gender = customer.optJSONObject("gender");
            String gen = (gender != null && gender.has("mrz")) ? gender.getString("mrz") : "N/A";


            // Affichage dans les TextViews
            tvName.setText(name);
            tvSurname.setText(surName);
            tvGender.setText(gen);

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

    /* PagerAdapter for supplying the ViewPager with the pages (fragments) to display. */
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
            // Force le recyclage des fragments quand notifyDataSetChanged() est appelé
            return POSITION_NONE;
        }
    }

    // Méthode pour calculer l'âge
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
            return -1; // Retourne -1 si le calcul échoue
        }
    }

    @Override
    protected void onDestroy() {
        if (selfieBitmap != null && !selfieBitmap.isRecycled()) {
            selfieBitmap.recycle();
            selfieBitmap = null;
        }
        similarityScore = null;
        super.onDestroy();
    }
}