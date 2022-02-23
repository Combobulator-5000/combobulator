package com.google.ar.core.examples.java.augmentedimage.classifier

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.core.Mat
import java.io.IOException
import java.io.InputStream

class Classifier(val context: Context) {


    fun load_image_to_Mat(fileName : String) : Bitmap? {
        val assetManager = context.assets
        var istr : InputStream? = null
        try {
            istr = assetManager.open(fileName);
        } catch (e : IOException) {
            e.printStackTrace();
        }
        val bitmap = BitmapFactory.decodeStream(istr);
        return bitmap;
    }
}