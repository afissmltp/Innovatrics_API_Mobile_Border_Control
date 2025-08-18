package com.dynamsoft.documentscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.dynamsoft.documentscanner.API.services.CustomerOnboarding.CustomerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CroppingActivity extends AppCompatActivity {
    private Button okayButton;
    private Button reTakeButton;
    private Bitmap background;
    private ImageView imageView;
    private OverlayView overlayView;
    private ImageView corner1;
    private ImageView corner2;
    private ImageView corner3;
    private ImageView corner4;
    private ImageView[] corners = new ImageView[4];
    private int mLastX;
    private int mLastY;
    private Point[] points;
    private int screenWidth;
    private int screenHeight;
    private int bitmapWidth;
    private int bitmapHeight;
    private int cornerWidth = (int) dp2px(15);
    private String source;
    private CustomerService customerService;
    private String imageUriString;
    private String customerId;

    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cropping);

        source = getIntent().getStringExtra("SOURCE");
        customerId = getIntent().getStringExtra("customerId");
        imageUriString = getIntent().getStringExtra("imageUri");
        customerService = new CustomerService();


        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        imageView = findViewById(R.id.imageView);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        overlayView = findViewById(R.id.cropOverlayView);
        reTakeButton = findViewById(R.id.reTakeButton);
        reTakeButton.setOnClickListener(v -> {
            onBackPressed();
        });
        okayButton = findViewById(R.id.okayButton);

        okayButton.setOnClickListener(v -> {
                Intent intent = new Intent(CroppingActivity.this, ViewerActivity.class);
                intent.putExtra("imageUri", imageUriString);
                intent.putExtra("points", points);
                intent.putExtra("bitmapWidth", bitmapWidth);
                intent.putExtra("bitmapHeight", bitmapHeight);
                intent.putExtra("customerId", customerId);
                startActivity(intent);
        });

        corner1 = findViewById(R.id.corner1);
        corner2 = findViewById(R.id.corner2);
        corner3 = findViewById(R.id.corner3);
        corner4 = findViewById(R.id.corner4);
        corners[0] = corner1;
        corners[1] = corner2;
        corners[2] = corner3;
        corners[3] = corner4;
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //updateOverlayViewLayout();
            }
        });
        DisplayMetrics metrics=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth=metrics.widthPixels;
        screenHeight=metrics.heightPixels;
        bitmapWidth = getIntent().getIntExtra("bitmapWidth",720);
        bitmapHeight = getIntent().getIntExtra("bitmapHeight",1280);
        loadPoints();
        loadImage();
        setEvents();
    }

    private void loadPoints(){
        Parcelable[] parcelables = getIntent().getParcelableArrayExtra("points");
        points = new Point[parcelables.length];
        for (int i = 0; i < parcelables.length; i++) {
            points[i] = (Point) parcelables[i];
        }
    }

    private void loadImage(){
        try {
            Uri uri = Uri.parse(getIntent().getStringExtra("imageUri"));
            InputStream inp = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inp);
            bitmap = rotatedImageBasedOnExif(bitmap,uri.getPath());
            imageView.setImageBitmap(bitmap);
            background = bitmap;
            drawOverlay();
            updateCornersPosition();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap rotatedImageBasedOnExif(Bitmap bitmap, String path) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(path);
            } catch (IOException e) {
                return bitmap;
            }
            int rotate = 0;
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            Log.d("DDN","orientation: "+orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            try (FileOutputStream out = new FileOutputStream(path)) {
                rotated.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return rotated;
        }
        return bitmap;
    }

    private void updateCornersPosition(){
        for (int i = 0; i < 4; i++) {
            int offsetX = getOffsetX(i);
            int offsetY = getOffsetY(i);
            corners[i].setX(points[i].x*screenWidth/bitmapWidth+offsetX);
            corners[i].setY(points[i].y*screenHeight/bitmapHeight+offsetY);
        }
    }

    private int getOffsetX(int index) {
        if (index == 0) {
            return -cornerWidth;
        }else if (index == 1){
            return 0;
        }else if (index == 2){
            return 0;
        }else{
            return -cornerWidth;
        }
    }

    private int getOffsetY(int index) {
        if (index == 0) {
            return -cornerWidth;
        }else if (index == 1){
            return -cornerWidth;
        }else if (index == 2){
            return 0;
        }else{
            return 0;
        }
    }

    private void setEvents(){
        for (int i = 0; i < 4; i++) {
            corners[i].setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    Log.d("DDN",event.toString());
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            mLastX = x;
                            mLastY = y;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            view.setX(view.getX()+x);
                            view.setY(view.getY()+y);
                            updatePointsAndRedraw();
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            });
        }
    }

    private void updatePointsAndRedraw(){
        for (int i = 0; i < 4; i++) {
            int offsetX = getOffsetX(i);
            int offsetY = getOffsetY(i);
            points[i].x = (int) ((corners[i].getX()-offsetX)/screenWidth*bitmapWidth);
            points[i].y = (int) ((corners[i].getY()-offsetY)/screenHeight*bitmapHeight);
        }
        drawOverlay();
    }

    private void drawOverlay(){
        overlayView.setPointsAndImageGeometry(points,bitmapWidth,bitmapHeight);
    }

    private void updateOverlayViewLayout(){
        Bitmap bm = background;
        double ratioView = ((double) imageView.getWidth())/imageView.getHeight();
        double ratioImage = ((double) bm.getWidth())/bm.getHeight();
        double offsetX = (ratioImage*bm.getWidth()-bm.getHeight())/2;
        overlayView.setX((float) offsetX);
    }

    public float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    private void createDocumentFrontPage(Bitmap image) {
        String imageBase64 = bitmapToBase64(image);
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
                            Toast.makeText(CroppingActivity.this,
                                    "Document uploaded successfully",
                                    Toast.LENGTH_LONG).show();
                            Log.d("UPLOAD_SUCCESS", response.toString(2));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(CroppingActivity.this, "Erreur parsing JSON", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CroppingActivity.this,
                                "Upload error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("UPLOAD_ERROR", "Upload failed", e);
                    });
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}