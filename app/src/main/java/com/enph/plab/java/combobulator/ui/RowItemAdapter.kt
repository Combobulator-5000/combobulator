package com.enph.plab.java.combobulator.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.R
import com.enph.plab.java.combobulator.classifier.Classifier
import com.enph.plab.java.combobulator.database.TrackedItem
import com.google.ar.core.Pose
import kotlinx.android.synthetic.main.row_item.view.*


// This was valuable
//https://guides.codepath.com/android/using-the-recyclerview
// TODO finish going through this tutorial
//https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview

class RowItemAdapter(private val activity: CombobulatorMainActivity, private val items: List<TrackedItem>) : RecyclerView.Adapter<RowItemAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val itemName: TextView = itemView.itemName
        val locateButton: Button = itemView.locateButton
        val imageView : ImageView = itemView.imageView
        val clickTarget : View = itemView.itemClickTarget
//        val itemRefImage: ImageView = itemView.findViewById<ImageView>(R.id.itemRefImage)
    }

    // a new item has been added to the list; add a view layout to the item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val itemView = inflater.inflate(R.layout.row_item, parent, false)
        return ViewHolder(itemView)
    }

    // Populate layout with data from a TrackedItem
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: TrackedItem = items[position]

        holder.itemName.text = item.name

        if (item.images.size > 0) {
            activity.displayImage(holder.imageView, item.images[0])
        } else {
            // Set to default image if no reference image provided
            // Note this must be done explicitly because of how RecyclerView reuses previous entries
            // (otherwise image may already be set for some previous item)
            holder.imageView.setImageResource(R.drawable.ic_launcher)
        }

        holder.clickTarget.setOnClickListener {
            activity.ui.openItemEditor(position)
        }

        // When the "locate" button is pressed, have the activity start tracking this item
        holder.locateButton.setOnClickListener {
            activity.setTarget(item)
            activity.ui.exitMenus()
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }


    fun newItem() {
        val newItem = TrackedItem(
            "New item",
            Pose.makeTranslation(0f,0f,0f),
            ArrayList())

        val lastPosition = Classifier.allObjects.size
        Classifier.addItem(newItem)

        notifyItemInserted(lastPosition)
    }
}