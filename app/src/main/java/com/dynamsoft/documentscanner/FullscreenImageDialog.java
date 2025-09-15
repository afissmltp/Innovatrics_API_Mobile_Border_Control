package com.dynamsoft.documentscanner;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;

import com.jsibbold.zoomage.ZoomageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class FullscreenImageDialog {

    public interface OnImageSavedListener {
        void onImageSaved();
    }

    public static void show(Context context, Bitmap bitmap, OnImageSavedListener listener) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ZoomageView imageView = dialog.findViewById(R.id.dialogImageView);
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);
        ImageButton saveButton = dialog.findViewById(R.id.saveButton);

        imageView.setImageBitmap(bitmap);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            saveImageToGallery(context, bitmap);
            Toast.makeText(context, "Image sauvegardée avec succès", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onImageSaved();
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private static void saveImageToGallery(Context context, Bitmap bitmap) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "Image_" + System.currentTimeMillis() + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MonAppImages");
                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    fos = context.getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    if (fos != null) fos.close();
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/MonAppImages";
                File file = new File(imagesDir);
                if (!file.exists()) file.mkdirs();
                String fileName = "Image_" + System.currentTimeMillis() + ".png";
                File image = new File(file, fileName);
                fos = new FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(image));
                context.sendBroadcast(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Erreur lors de la sauvegarde de l'image", Toast.LENGTH_SHORT).show();
        }
    }
}
