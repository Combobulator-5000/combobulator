package com.google.ar.core.examples.java.augmentedimage.classifier

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.SIFT
import java.io.IOException
import java.io.InputStream

class DatabaseObject(val name : String){

    val location : Pose = Pose.makeTranslation(0f,0f,0f)
    val allDescriptors : MutableList<Mat> = ArrayList()

    // Below: methods to add reference image data (in multiple formats) to the database object
    fun addAssetImage(fileName : String, context : Context) {
        val mat = OpenCVHelpers.readImageMatFromAsset(fileName, context)
        addMatImage(mat)
    }

    fun addMatImage(image : Mat) {
        val descriptors = OpenCVHelpers.getDescriptors(image)
        addDescriptorMat(descriptors)
    }

    fun addDescriptorMat(descriptors : Mat) {
        allDescriptors.add(descriptors)
    }

    override fun toString() : String {
        return String.format(
            "DatabaseObject: %s [Location: %.2f %.2f %.2f; Reference images: %d]",
            name, location.tx(), location.ty(), location.tz(), allDescriptors.size
        )
    }
}