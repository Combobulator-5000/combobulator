package com.google.ar.core.examples.java.augmentedimage

import android.R
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.augmentedimage.database.TrackedItem
import com.google.ar.core.examples.java.augmentedimage.databinding.ActivityMainBinding
import org.opencv.android.Utils
import org.opencv.core.Mat


class UI(private val activity: AugmentedImageActivity) {

    companion object {
        const val ON_AT_STARTUP = true
    }

    private val ui: ActivityMainBinding = ActivityMainBinding.inflate(activity.layoutInflater)

    // Shortcuts to elements that the activity needs access to
    val surfaceView = ui.surfaceview

    val res : Resources = activity.resources

    private val classifyRequestQueue = ClassifyRequestHandler()

    // Data
    var location : Pose? = null
    private var target : TrackedItem? = null
    var miscData : MutableMap<String, String> = HashMap()
    var miscDataUpdated = false
    var maxDistance = 1.0
    var minDistance = 0.1


    init {
        activity.setContentView(ui.root)

        ui.mainDebugText.setHorizontallyScrolling(true)

        // Debug panel & switch configuration
        ui.debugSwitch.isChecked = ON_AT_STARTUP
        ui.debugSwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            setDebugPanelVisibility(checked)
        }
        setDebugPanelVisibility(ui.debugSwitch.isChecked)

        ui.classifyButton.setOnClickListener(classifyRequestQueue)

        updateDebugText()
    }

    fun setTarget(target : TrackedItem?) {
        this.target = target

        activity.runOnUiThread {
            // reset progress whenever we scan a new thing
            maxDistance = minDistance
            ui.trackingProgress.progress = 0

            if (target == null) {
                ui.scanCheckbox.isChecked = false
                ui.trackingText.text = "No target tracking"
            } else {
                ui.scanCheckbox.isChecked = true
                ui.trackingText.text = "Tracking item: ${target.name}"
            }
        }
    }

    fun classifyRequestPending() : Boolean {
        return classifyRequestQueue.poll()
    }

    fun setPositionCalibrated(isCalibrated : Boolean) {
        activity.runOnUiThread {
            ui.calibratedCheckbox.isChecked = isCalibrated
        }
    }

    fun displayImage(image : Mat, imageView : ImageView) {
        val bm = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(image, bm)
        imageView.setImageBitmap(bm)
    }

    fun displayImage(image : Mat) {
        displayImage(image, ui.debugImageView)
    }

    fun updateTrackingProgress(distance : Double) {
        activity.runOnUiThread {
            if (distance > maxDistance) {
                maxDistance = distance
                val adjustedMax = maxDistance - minDistance
                ui.trackingProgress.max = (adjustedMax * 100).toInt()
                ui.trackingProgress.progress = 0
            } else {
                ui.trackingProgress.progress = ((maxDistance - distance)*100).toInt()
            }
        }
    }

    operator fun set(key : String, value : Any) {
        miscData[key] = value.toString()
        miscDataUpdated = true
    }


    @SuppressLint("SetTextI18n")
    @Synchronized
    fun updateDebugText() {
        if (ui.debugSwitch.isChecked) {

            val locationText: String = if (location != null) {
                String.format(
                    "X: %.2f \nY: %.2f \nZ: %.2f \n",
                    location!!.tx(), location!!.ty(), location!!.tz()
                )
            } else {
                "X: - \nY: - \nZ: - \n"
            }

            val trackingText: String = if (target != null) {
                val t = target!!
                String.format(
                    "Tracking: %s \n\t [%.2f %.2f %.2f]",
                    t.name, t.location.tx(), t.location.ty(), t.location.tz()
                )
            } else {
                "Tracking: - \n\t [--  --  --]"
            }

            activity.runOnUiThread {
                ui.mainDebugText.text = locationText + trackingText
                if (miscDataUpdated) {
                    miscDataUpdated = false
                    ui.miscDebugText.text = miscData.map { (k, v) -> "$k: $v" }.joinToString("\n")
                }
            }
        }
    }

    fun setFitToScanVisibility(visibility : Int) {
        activity.runOnUiThread {
            ui.imageViewFitToScan.visibility = visibility
        }
    }


    private fun setDebugPanelVisibility(visible : Boolean) {
        activity.runOnUiThread {
            if (visible) {
                updateDebugText()
                ui.debugPanel.visibility = View.VISIBLE
            } else {
                ui.debugPanel.visibility = View.INVISIBLE
            }
        }
    }

    // Handles capturing button clicks and passing the info between UI thread and the main
    // activity's onDrawFrame method
    class ClassifyRequestHandler : View.OnClickListener {

        var tapQueued = false

        @Synchronized
        override fun onClick(p0: View?) {
            tapQueued = true
        }

        @Synchronized
        fun poll() : Boolean {
            val temp = tapQueued
            tapQueued = false
            return temp
        }
    }
}