package com.google.ar.core.examples.java.augmentedimage

import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.augmentedimage.classifier.DatabaseObject

class DebugPanel(val activity: AugmentedImageActivity) : CompoundButton.OnCheckedChangeListener {

    companion object {
        const val ON_AT_STARTUP = true
    }

    // UI elements
    val panel : ConstraintLayout
    val debugSwitch : Switch
    val mainText : TextView
    val text : TextView
    val image : ImageView

    // Data
    var location : Pose? = null
    var target : DatabaseObject? = null
    var miscData : MutableMap<String, String> = HashMap()
    var miscDataUpdated = false

    init {
//        activity.setContentView(R.layout.activity_main)

        debugSwitch = activity.findViewById<Switch>(R.id.debugSwitch)
        panel = activity.findViewById<ConstraintLayout>(R.id.debugPanel)
        mainText = activity.findViewById<TextView>(R.id.mainDebugText)
        text = activity.findViewById<TextView>(R.id.miscDebugText)
        image = activity.findViewById(R.id.debugImageView)

        // Opens app with debug screen on
        debugSwitch.isChecked = ON_AT_STARTUP


        mainText.setHorizontallyScrolling(true)
        debugSwitch.setOnCheckedChangeListener(this)
        setPanelVisibility(debugSwitch.isChecked)


//        miscData["hi"] = "123"
//        miscData["this works"] = "yes"
//
//        target = "scissors a la carte"
//        targetLocation = Pose.makeTranslation(1f,3f,4f,)
//        location = Pose.makeTranslation(2f,2f,2f)

        updateText()
    }

    operator fun set(key : String, value : Any) {
        miscData[key] = value.toString()
        miscDataUpdated = true
    }


    @Synchronized
    fun updateText() {
        if (debugSwitch.isChecked) {

            val locationText : String = if (location != null) {
                String.format(
                    "X: %.2f \nY: %.2f \nZ: %.2f \n",
                    location!!.tx(), location!!.ty(), location!!.tz())
            } else {
                "X: - \nY: - \nZ: - \n"
            }

            val trackingText : String = if (target != null) {
                val t = target!!
                String.format(
                    "Tracking: %s \n\t [%.2f %.2f %.2f]",
                    t.name, t.location.tx(), t.location.ty(), t.location.tz())
            } else {
                "Tracking: - \n\t [--  --  --]"
            }

            mainText.text = locationText  + trackingText

            if (miscDataUpdated){
                miscDataUpdated = false
                text.text = miscData.map { (k, v) -> "$k: $v" }.joinToString("\n")
            }
        }
    }


    fun setPanelVisibility(visible : Boolean) {
        if(visible) {
            updateText()
            panel.visibility = View.VISIBLE
        } else {
            panel.visibility = View.INVISIBLE
        }
    }

    override fun onCheckedChanged(p0: CompoundButton?, checked: Boolean) {
        setPanelVisibility(checked)
    }
}