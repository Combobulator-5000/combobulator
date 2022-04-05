package com.enph.plab.java.combobulator.ui

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.media.Image
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.OpenCVHelpers
import com.enph.plab.java.combobulator.classifier.Classifier
import com.google.ar.core.Pose
import com.enph.plab.java.combobulator.database.TrackedItem
import com.enph.plab.java.combobulator.databinding.ActivityMainBinding
import com.enph.plab.java.common.helpers.SnackbarHelper
import com.google.ar.core.Track
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.lang.NullPointerException


@SuppressLint("ClickableViewAccessibility")
class UI(protected val activity: CombobulatorMainActivity) {

    companion object {
        const val DEBUG_ON_AT_STARTUP = false

        var miscData : MutableMap<String, String> = HashMap()
    }

    private val ui: ActivityMainBinding = ActivityMainBinding.inflate(activity.layoutInflater)

    lateinit var messageSnackbarHelper: SnackbarHelper

    // Shortcuts to elements that the activity needs access to
    val surfaceView = ui.surfaceview
    val auxButton = ui.auxiliaryButton

    val fragmentManager = activity.supportFragmentManager
    val itemListFragment : ItemListUI
    val itemEditorFragment : ItemEditorUI

    val res : Resources = activity.resources

    private val classifyRequestQueue = ClassifyRequestHandler()

    // Data
    var location : Pose? = null

    private var tracking : Boolean = false
    private var target : TrackedItem? = null
    private var targetName : String = ""
    private var targetLocation : Pose = Pose.makeTranslation(0f,0f,0f)

    var miscDataUpdated = false
    var maxDistance = 1.0
    var minDistance = 0.1


    init {
        activity.setContentView(ui.root)

        ui.mainDebugText.setHorizontallyScrolling(true)

        // Debug panel & switch configuration
        ui.debugSwitch.isChecked = DEBUG_ON_AT_STARTUP
        ui.debugSwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            setDebugPanelVisibility(checked)
        }
        setDebugPanelVisibility(ui.debugSwitch.isChecked)

        ui.classifyButton.setOnClickListener(classifyRequestQueue)

        itemListFragment = ItemListUI(this)
        itemEditorFragment = ItemEditorUI(this)

        setupAuxButton()

        ui.openItemList.setOnClickListener {
            loadFragment(itemListFragment)
            ui.fragmentContainerView.visibility = View.VISIBLE
        }

        ui.reachedTargetDismiss.setOnClickListener {
            ui.reachedTargetHint.visibility = View.INVISIBLE
        }

        updateDebugText()
    }

    private fun setupAuxButton() {
        auxButton.visibility = View.VISIBLE
        auxButton.setOnClickListener {
            loadFragment(itemListFragment)
            ui.fragmentContainerView.visibility = View.VISIBLE
        }
        auxButton.text = "Item List"
    }

    fun showMessage(msg: String){
        messageSnackbarHelper.showMessageWithDismiss(activity, msg)
    }

    fun captureImage(image: Image, currentPose: Pose?){
        val mat = OpenCVHelpers.imageToMat(image)

        activity.runOnUiThread {
            if (itemEditorFragment.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                itemEditorFragment.captureImage(mat, currentPose)
            }
        }
    }

    fun openItemEditor(position: Int) {
        val item = Classifier.allObjects[position]
        loadFragment(itemEditorFragment)
        itemEditorFragment.changeCurrentItem(item, position)
    }

    fun exitMenus() {
        activity.enterMode(CombobulatorMainActivity.Mode.USER)
        setupAuxButton()
        hideFragment()
    }

    fun loadFragment(fragment: Fragment) {
        activity.enterMode(CombobulatorMainActivity.Mode.ADMIN)
        auxButton.visibility = View.INVISIBLE

        val fragmentTransaction = fragmentManager.beginTransaction()

        // replace the FrameLayout with new Fragment
        fragmentTransaction.replace(ui.fragmentContainerView.id, fragment);
        fragmentTransaction.commit(); // save the changes
    }

    fun hideFragment() {
        ui.fragmentContainerView.visibility = View.INVISIBLE
    }

    fun showFragment() {
        ui.fragmentContainerView.visibility = View.VISIBLE
    }

    fun targetReached() {
        if (target != null){
            if (target!!.locationRefImage != null) {
                showMessage("image available")
                activity.runOnUiThread {
                    ui.reachedTargetHint.visibility = View.VISIBLE
                    displayImage(target!!.locationRefImage!!, ui.reachedTargetImage)
                    setTarget(null)
                }
            }
        }
    }

    fun setTarget(target : TrackedItem?) {
        this.target = target

        tracking = (target == null)

        activity.runOnUiThread {
            // reset progress whenever we scan a new thing
            maxDistance = minDistance
            ui.trackingProgress.progress = 0

            if (target == null) {
                ui.trackingText.text = "No current target"
            } else {
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
        activity.runOnUiThread {
            val bm = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(image, bm)
            imageView.setImageBitmap(bm)
        }
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
                val t = target!!.location
                String.format(
                    "Tracking: %s \n\t [%.2f %.2f %.2f]",
                    target!!.name, t.tx(), t.ty(), t.tz()
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


