package com.google.ar.core.examples.java.augmentedimage.localization

import com.google.ar.core.Pose
import kotlin.math.sqrt

// Abstraction layer between position lookup tables for augmentedImages/fiducials/tools and our code
class Workspace {

    fun getCalibrationPointPose(index : Int) : Pose
    {
        val translation = Pose.makeTranslation(0f, 0f, 0f)
        // normal vector parallel to x axis
        val rotation = Pose.makeRotation(-0.5f, -0.5f, -0.5f, -0.5f)

        // An addtional rotation that seems to be necessary given how ARCore interprets poses of AugmentedImages
        // (equal to 90 deg rot about -y)
        val halfSqrt2 = sqrt(2.0f)/2
        val rotation2 = Pose.makeRotation(0f, halfSqrt2, 0f, halfSqrt2)

        return translation.compose(rotation2.compose(rotation))
    }
}