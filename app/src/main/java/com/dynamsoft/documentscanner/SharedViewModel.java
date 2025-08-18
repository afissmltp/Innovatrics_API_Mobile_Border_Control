package com.dynamsoft.documentscanner;

import android.graphics.Bitmap;

import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private Bitmap portraitBitmap;
    private Bitmap frontImageBitmap;

    public void setPortraitBitmap(Bitmap bitmap) {
        portraitBitmap = bitmap;
    }

    public void setFrontImageBitmap(Bitmap bitmap) {
        frontImageBitmap = bitmap;
    }

    public Bitmap getPortraitBitmap() {
        return portraitBitmap;
    }

    public Bitmap getFrontImageBitmap() {
        return frontImageBitmap;
    }
}