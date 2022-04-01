package com.enph.plab.java.combobulator.database

import com.google.ar.core.Pose
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmField
import io.realm.annotations.Required
import org.bson.types.ObjectId
import org.opencv.core.Mat

// I don't like that these fields are nilable, but Realm requires there to be an empty
// constructor in order to populate this object with (i'm guessing) reflection
open class TrackedItem(@Required var name : String = "Uninitialized object!",
                       @Required private var rawLocation : RealmList<Float> = RealmList<Float>(),
                       private var rawImages : RealmList<ImageBytes>? = null,
                        @Required var deployment : String = "plab") : RealmObject() {

    // is this absolute pose? if so pog champion
    @Ignore var location : Pose = Pose.makeTranslation(0f,0f,0f)
    @Ignore var images : MutableList<Mat> = ArrayList();
    @PrimaryKey @RealmField("_id") private var _id: ObjectId? = ObjectId()
    // val allDescriptors : MutableList<Mat> = ArrayList()

    init {
        location = DBUtil.deserialize_pose(rawLocation)
        if (rawImages == null) {
            rawImages = RealmList<ImageBytes>()
        }

        images = rawImages!!.map { img -> DBUtil.deserialize_mat(img) }.toMutableList()
    }

    constructor(name : String, location : Pose, images : MutableList<Mat>)
        : this(name,
        DBUtil.serialize(location),
        DBUtil.imageListToRawData(images)) {}

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