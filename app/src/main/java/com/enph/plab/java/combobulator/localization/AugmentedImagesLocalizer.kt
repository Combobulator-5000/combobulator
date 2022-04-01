package com.enph.plab.java.combobulator.localization

import com.google.ar.core.*
import java.util.*

class AugmentedImagesLocalizer(private val workspace: Workspace) {

    companion object {
        var TAG = "Localizer"

        class CalibrationPoint(val augmentedImage: AugmentedImage, workspace: Workspace, session: Session) {
//            val anchor : Anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
            val anchor : Anchor = session.createAnchor(augmentedImage.centerPose)
            val absPose : Pose = workspace.getCalibrationPointPose(augmentedImage.index)
        }
    }
    var calibrated : Boolean = false
    val calibrationMap : MutableMap<Int, CalibrationPoint> = HashMap()
    var currentAbsTransform : Pose? = null

    fun convertToAbsPose(framePose : Pose) : Pose {
        return currentAbsTransform!!.compose(framePose)
    }

    fun convertToFramePose(absPose : Pose) : Pose {
        return currentAbsTransform!!.inverse().compose(absPose)
    }

    fun update(frame : Frame, session : Session, attemptCalibration : Boolean = true) {
        if (attemptCalibration) updateCalibrationMap(frame, session)

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

    private fun updateCalibrationMap(frame : Frame, session : Session) {

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
                        calibrationMap[augmentedImage.index] = CalibrationPoint(augmentedImage, workspace, session)
                    }
                }
                TrackingState.STOPPED -> {
                    val out = calibrationMap.remove(augmentedImage.index)
                    out?.anchor?.detach()
                }
                else -> {
                }
            }
        }
    }


}