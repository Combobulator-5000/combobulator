package com.enph.plab.java.combobulator.ui

import com.enph.plab.java.combobulator.databinding.ItemEditorBinding
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.R
import com.enph.plab.java.combobulator.classifier.Classifier
import com.enph.plab.java.combobulator.database.TrackedItem
import com.google.ar.core.Pose
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

        val numScans = scans.size

        binding.addScans.setOnClickListener {
            parent.hideFragment()
            parent.auxButton.visibility = View.VISIBLE
            parent.auxButton.text = "Done (scans: $numScans)"
            parent.auxButton.setOnClickListener { exitCaptureMode() }

            imageListenerMode = ImageListener.SCANS
        }

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

    fun setPose(pose: Pose) {
        binding.xInput.editText?.setText(pose.tx().toString())
        binding.yInput.editText?.setText(pose.ty().toString())
        binding.zInput.editText?.setText(pose.tz().toString())
    }

    fun update(item: TrackedItem?) {
        binding.helperImage.setImageResource(android.R.drawable.ic_input_add)

        if (item != null) {

            binding.itemNameEditor.editText?.setText(item.name)
            setPose(item.location)

            // Prepare reference image (image demonstrating how/where to put item away)
            if (item.locationRefImage != null) {
                refImage = item.locationRefImage
                parent.displayImage(refImage!!, binding.helperImage)
            }

            // Prepare scrollable list of scanned images
            scans.addAll(item.images)
            binding.rvScans.adapter = ScanViewAdapter(parent, scans)
            binding.rvScans.layoutManager = LinearLayoutManager(
                activity, LinearLayoutManager.HORIZONTAL,false
            )
        }
    }

    private fun returnToListScreen() {
        // clear fields that are stored here
        refImage = null
        scans = ArrayList()
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
                activeItem = TrackedItem(name, location, scans)
                activeItem!!.locationRefImage = refImage
                Classifier.addItem(activeItem!!)

                // TODO: in a smarter implementation we may be able to cache the item list RecyclerView
                // and do minimal updating
                // itemAdapter.notifyItemInserted( current_last_position + 1 )

            } else {
                activeItem!!.name = name
                activeItem!!.location = location
                activeItem!!.images = scans
                activeItem!!.locationRefImage = refImage

                // TODO: in a smarter implementation we may be able to cache the item list RecyclerView
                // and do minimal updating
                // itemAdapter.notifyItemChanged(activeItemPosition)
            }

        } catch (e : Exception) {
            parent.showMessage("Not all fields populated")
            return
        }

        returnToListScreen()
    }

    fun exitCaptureMode() {
        parent.showFragment()
        parent.auxButton.visibility = View.INVISIBLE
        imageListenerMode = ImageListener.NONE
    }


    fun captureImage(image: Mat, currentPose: Pose?) {
        when (imageListenerMode) {
            ImageListener.NONE -> return
            ImageListener.REF_IMAGE -> {
                if (currentPose == null) {
                    parent.showMessage("Position not calibrated; look for marker")
                    return
                }
                refImage = image
                parent.displayImage(refImage!!, binding.helperImage)
                setPose(currentPose)
                exitCaptureMode()
            }
            ImageListener.SCANS -> {
                scans.add(image)
                val numScans = scans.size
                parent.auxButton.text = "Done (scans: $numScans)"
                binding.rvScans.adapter?.notifyItemInserted(-1)
            }
        }
    }

    class ScanViewAdapter(private val mainUI: UI, private val images: List<Mat>)
        : RecyclerView.Adapter<ScanViewAdapter.ImageViewHolder>() {


        class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView : ImageView = itemView as ImageView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = ImageView(parent.context)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val image = images[position]
            mainUI.displayImage(image, holder.imageView)
        }

        override fun getItemCount(): Int {
            return images.size
        }
    }
}