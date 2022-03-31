package com.google.ar.core.examples.java.augmentedimage.localization

import com.google.ar.core.*

interface Localizer {

    companion object {
        var TAG = "Localizer"
    }

    val workspace : Workspace
    var calibrated : Boolean
    var currentAbsTransform: Pose?

    fun update(frame: Frame, session: Session, attemptCalibration: Boolean)

    fun convertToAbsPose(framePose : Pose) : Pose {
        return currentAbsTransform!!.compose(framePose)
    }

    fun convertToFramePose(absPose : Pose) : Pose {
        return currentAbsTransform!!.inverse().compose(absPose)
    }
}