package com.enph.plab.java.combobulator.ui

import android.graphics.drawable.Drawable
import android.media.Image
import com.enph.plab.java.combobulator.databinding.ItemEditorBinding
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.OpenCVHelpers
import com.enph.plab.java.combobulator.R
import com.enph.plab.java.combobulator.classifier.Classifier
import com.enph.plab.java.combobulator.database.TrackedItem
import com.google.android.material.textfield.TextInputLayout
import com.google.ar.core.Pose
import com.google.ar.core.Track
import org.opencv.core.Mat

//RecyclerView.ViewHolder(itemView)

class ItemEditorUI(val parent: UI) : Fragment(R.layout.item_editor) {

    companion object {
        const val layout = R.layout.item_editor
        const val NEW = -1
    }

    lateinit var activity: CombobulatorMainActivity
    lateinit var binding: ItemEditorBinding

    enum class ImageListener {NONE, REF_IMAGE, SCANS}
    var imageListenerMode = ImageListener.NONE
    var refImage: Mat? = null
    var scans: MutableList<Mat> = ArrayList()

    var activeItem: TrackedItem? = null
    private var activeItemPosition = NEW

    // The Editor screen needs to be able to
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity = getActivity() as CombobulatorMainActivity
        // Inflate the layout for this fragment
        val view = inflater.inflate(layout, container, false)
        binding = ItemEditorBinding.bind(view)

        update(activeItem)

        binding.helperImage.setOnClickListener {
            parent.hideFragment()
            imageListenerMode = ImageListener.REF_IMAGE
        }

        // TODO: add onClickListener for scan mode


//         Clicking "back" returns user to item list screen
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (imageListenerMode != ImageListener.NONE) {
                exitCaptureMode()
            } else {
                returnToListScreen()
            }
        }

        binding.cancelButton.setOnClickListener { returnToListScreen() }
        binding.saveButton.setOnClickListener { save(binding) }

        return view
    }

    fun update(item: TrackedItem?) {
        binding.helperImage.setImageResource(android.R.drawable.ic_input_add)

        if (item != null) {

            binding.itemNameEditor.editText?.setText(item.name)

            binding.xInput.editText?.setText(item.location.tx().toString())
            binding.yInput.editText?.setText(item.location.ty().toString())
            binding.zInput.editText?.setText(item.location.tz().toString())
        }
    }

    private fun returnToListScreen() {
        parent.loadFragment(parent.itemListFragment)
    }

    fun changeCurrentItem(item: TrackedItem, position: Int) {
        activeItem = item
        activeItemPosition = position
    }

    fun newItem() {
        activeItem = null
        activeItemPosition = NEW
    }

    fun save(binding: ItemEditorBinding){

        try {
            val name = binding.itemNameEditor.editText?.text.toString()

            val location = Pose.makeTranslation(
                binding.xInput.editText?.text.toString().toFloat(),
                binding.yInput.editText?.text.toString().toFloat(),
                binding.zInput.editText?.text.toString().toFloat(),
            )

            if (activeItem == null) {
                val newItem = TrackedItem(name, location, ArrayList())
                Classifier.addItem(newItem)

                // TODO: in a smarter implementation we may be able to cache the item list RecyclerView
                // and do minimal updating
                // itemAdapter.notifyItemInserted( current_last_position + 1 )

            } else {


                activeItem!!.name = name
                activeItem!!.location = location

                // TODO: in a smarter implementation we may be able to cache the item list RecyclerView
                // and do minimal updating
                // itemAdapter.notifyItemChanged(activeItemPosition)
            }

        } catch (e : Exception) {
            Log.v("Editor", "Not all fields populated")
            return
        }

        returnToListScreen()
    }

    fun exitCaptureMode() {
        parent.showFragment()
        imageListenerMode = ImageListener.NONE
    }

    fun captureImage(image: Mat) {

        if(imageListenerMode == ImageListener.REF_IMAGE) {


            refImage = image
            UI.miscData["capturedmat"] = refImage.toString()


            parent.displayImage(refImage!!, binding.helperImage)

            UI.miscData["listener mode"] = imageListenerMode.toString() + "c"
//            exitCaptureMode()
        }

//        when (imageListenerMode) {
//            ImageListener.NONE -> {
//                UI.miscData["listener mode"] = imageListenerMode.toString()
//            }
//            ImageListener.REF_IMAGE -> {
//                refImage = OpenCVHelpers.imageToMat(image)
//
//                UI.displayImage(refImage!!, binding.helperImage)
//                UI.miscData["listener mode"] = imageListenerMode.toString()
//                UI.miscData["hi"] = "yyes"
//                exitCaptureMode()
//            }
//            ImageListener.SCANS -> {
//                UI.miscData["listener mode"] = imageListenerMode.toString()
//            }
//        }
    }


//    fun openEditor(item: TrackedItem) {
//        // TODO
//    }
}