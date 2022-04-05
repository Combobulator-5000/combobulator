package com.enph.plab.java.combobulator.database

import com.google.ar.core.Pose
import org.opencv.core.Mat

class TrackedItem(
    var name: String,
    var location: Pose,
    var images: MutableList<Mat>) {

    // An image demonstrating how/where the item should be put away; not for feature matching.
    // May be shown to the user when they arrive near the target location.
    var locationRefImage: Mat? = null

    fun toRealmTrackedItem() : RealmTrackedItem {
        return RealmTrackedItem(name, location, images)
    }

    fun getRefImage() : Mat? {
        return locationRefImage ?: if(images.size > 0) images[0] else null
    }
}