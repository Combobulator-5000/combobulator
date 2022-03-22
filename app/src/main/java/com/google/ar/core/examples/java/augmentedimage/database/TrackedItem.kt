package com.google.ar.core.examples.java.augmentedimage.database

import android.content.Context
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import com.google.ar.core.examples.java.augmentedimage.classifier.Classifier
import io.realm.RealmObject
import io.realm.annotations.Ignore
import org.opencv.core.Mat

class TrackedItem(val name : String, private val rawLocation : FloatArray) : RealmObject() {

    // is this absolute pose? if so pog champion
    @Ignore
    var location : Pose = Pose.makeTranslation(0f,0f,0f)
    val images : MutableList<Mat> = ArrayList();
    // val allDescriptors : MutableList<Mat> = ArrayList()

    public TrackedItem(name : String, location : Pose, images : MutableList<Mat>) {

    }

    protected TrackedItem() {

    }

    // TODO:
    // Make database background listener that populates Classifier.allDescriptors as it goes
    // Gut this class to get rid of the mutation to the imageset
    // compute descriptors

    // Below: methods to add reference image data (in multiple formats) to the database object
    // fun addAssetImage(fileName : String, context : Context) {
    //     val mat = OpenCVHelpers.readImageMatFromAsset(fileName, context)
    //     addMatImage(mat)
    // }

    // fun addMatImage(image : Mat) {
    //     val descriptors = OpenCVHelpers.getDescriptors(image) addDescriptorMat(descriptors)
    // }

    // fun addDescriptorMat(descriptors : Mat) {
    //     Classifier.allDescriptors[this] = descriptors
    // }

    // override fun toString() : String {
    //     return String.format(
    //         "DatabaseObject: %s [Location: %.2f %.2f %.2f; Reference images: not implemented rn, ask seth]",
    //         name, location.tx(), location.ty(), location.tz()
    //     )
    // }
}