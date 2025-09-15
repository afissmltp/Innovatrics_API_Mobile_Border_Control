package com.dynamsoft.documentscanner.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static Bitmap handleRotation(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;

        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

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

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (IOException e) {
            Log.e("ImageUtils", "Erreur lors de la gestion de la rotation de l'image: " + e.getMessage());
        }
        return bitmap;
    }

    public static Bitmap compressBitmap(Bitmap original, int quality) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            original.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            byte[] compressedData = outputStream.toByteArray();
            return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length);
        } catch (IOException e) {
            Log.e("ImageUtils", "Erreur compression bitmap", e);
            return original;
        }
    }
}