package com.enph.plab.java.combobulator.database

import com.google.ar.core.Pose
import org.opencv.core.Mat

class TrackedItem(
    var name: String,
    var location: Pose,
    var images: MutableList<Mat> ) {

    fun toRealmTrackedItem() : RealmTrackedItem {
        return RealmTrackedItem(name, location, images)
    }

}