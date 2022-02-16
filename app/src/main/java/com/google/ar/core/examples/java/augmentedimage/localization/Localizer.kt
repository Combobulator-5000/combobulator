package com.google.ar.core.examples.java.augmentedimage.localization

import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Pose

interface Localizer {

    companion object {
        var TAG = "Localizer"
    }

    val workspace : Workspace

    var calibrated : Boolean
    var lastAnchor : Anchor?
    var lastAnchorAbsPose : Pose?

    var currentAbsPose : Pose?

    fun updatePose(camera : Camera)
}