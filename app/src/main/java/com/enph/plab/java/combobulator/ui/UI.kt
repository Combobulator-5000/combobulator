package com.enph.plab.java.combobulator.ui

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.media.Image
import android.provider.ContactsContract
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
import org.opencv.android.Utils
import org.opencv.core.Mat


@SuppressLint("ClickableViewAccessibility")
class UI(protected val activity: CombobulatorMainActivity) {

    companion object {
        const val ON_AT_STARTUP = true

        var miscData : MutableMap<String, String> = HashMap()
    }

//    var capturer: ImageCapturer = ImageCapturer()

    private val ui: ActivityMainBinding = ActivityMainBinding.inflate(activity.layoutInflater)

    // Shortcuts to elements that the activity needs access to
    val surfaceView = ui.surfaceview

    val fragmentManager = activity.supportFragmentManager
    val itemListFragment : ItemListUI
    val itemEditorFragment : ItemEditorUI

    val res : Resources = activity.resources

    private val classifyRequestQueue = ClassifyRequestHandler()

    // Data
    var location : Pose? = null

    private var tracking : Boolean = false
    private var targetName : String = ""
    private var targetLocation : Pose = Pose.makeTranslation(0f,0f,0f)

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

        itemListFragment = ItemListUI(this)
        itemEditorFragment = ItemEditorUI(this)

        ui.openItemList.setOnClickListener {
            loadFragment(itemListFragment)
            ui.fragmentContainerView.visibility = View.VISIBLE
        }

        updateDebugText()
    }

    fun captureImage(image: Image){
        val mat = OpenCVHelpers.imageToMat(image)

        activity.runOnUiThread {
            if (itemEditorFragment.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                itemEditorFragment.captureImage(mat)
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
        hideFragment()
    }

    fun loadFragment(fragment: Fragment) {
        activity.enterMode(CombobulatorMainActivity.Mode.ADMIN)

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

    fun isFragmentVisible() : Boolean {
        return ui.fragmentContainerView.visibility == View.VISIBLE
    }

    fun setTarget(target : TrackedItem?) {
        // Must be unpacked here,
        if (target != null) {
            tracking = true
            targetName = target.name
            targetLocation = target.location
        } else {
            tracking = false
        }

        activity.runOnUiThread {
            // reset progress whenever we scan a new thing
            maxDistance = minDistance
            ui.trackingProgress.progress = 0

            if (target == null) {
                ui.trackingText.text = "No current target"
            } else {
                ui.trackingText.text = "Tracking item: ${targetName}"
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

    fun displayImage(bm: Bitmap) {
        activity.runOnUiThread { ui.debugImageView.setImageBitmap(bm) }
    }

    fun displayImage(image : Mat, imageView : ImageView) {
        activity.runOnUiThread {
            try {
                val bm = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
                this["bm dims"] = bm.height.toString() + bm.width.toString()
                Utils.matToBitmap(image, bm)
                imageView.setImageBitmap(bm)
            } catch (e: Exception) {
                this["error"] = e.toString()
            }

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

            val trackingText: String = if (tracking) {
                val t = targetLocation
                String.format(
                    "Tracking: %s \n\t [%.2f %.2f %.2f]",
                    targetName, t.tx(), t.ty(), t.tz()
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


