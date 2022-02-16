package com.google.ar.core.examples.java.augmentedimage.localization

import com.google.ar.core.*
import java.util.*

class AugmentedImagesLocalizer {

    companion object {
        var TAG = "Localizer"

        class CalibrationPoint(val augmentedImage: AugmentedImage, workspace: Workspace) {
            val anchor : Anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
            val absPose : Pose = workspace.getCalibrationPointPose(augmentedImage.index)
        }
    }

    val workspace : Workspace = Workspace()

    var calibrated : Boolean = false
    val calibrationMap : MutableMap<Int, CalibrationPoint> = HashMap()
    var currentAbsTransform : Pose? = null

    fun convertToAbsPose(framePose : Pose) : Pose {
        return currentAbsTransform!!.compose(framePose)
    }

    fun update(frame : Frame, attemptCalibration : Boolean = true) {
        if (attemptCalibration) updateCalibrationMap(frame)

        val absTransformGuesses = calibrationMap.values.map {
            it.absPose.compose(it.anchor.pose.inverse())
        }

        // If we are tracking multiple images, for now just pick one
        // TODO: possibly also assign confidence scores, e.g. based on current distance from anchor or time since anchor was placed/last seen?
        if (absTransformGuesses.isNotEmpty()) {
            currentAbsTransform = absTransformGuesses.first()
        }
        else {
            calibrated = false
            currentAbsTransform = null
        }

    }

    private fun updateCalibrationMap(frame : Frame) {

        val updatedAugmentedImages: Collection<AugmentedImage> =
            frame.getUpdatedTrackables<AugmentedImage>(AugmentedImage::class.java)

        // Iterate to update augmentedImageMap, remove elements that are no longer tracked.
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.TRACKING -> {
                    calibrated = true

                    // Create a new anchor for newly found images.
                    if (!calibrationMap.containsKey(augmentedImage.index))
                    {
                        calibrationMap[augmentedImage.index] = CalibrationPoint(augmentedImage, workspace)
                    }
                }
                TrackingState.STOPPED -> calibrationMap.remove(augmentedImage.index)
                else -> {
                }
            }
        }
    }


}