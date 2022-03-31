package com.google.ar.core.examples.java.augmentedimage

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.ar.core.examples.java.augmentedimage.database.TrackedItem
import kotlinx.android.synthetic.main.row_item.view.*


// This was valuable
//https://guides.codepath.com/android/using-the-recyclerview
// TODO finish going through this tutorial
//https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview

class ItemListAdapter(private val activity: AugmentedImageActivity, private val items: List<TrackedItem>) : RecyclerView.Adapter<ItemListAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val itemName: TextView = itemView.itemName
        val locateButton: Button = itemView.locateButton
//        val itemRefImage: ImageView = itemView.findViewById<ImageView>(R.id.itemRefImage)
    }

    // a new item has been added to the list; add a view layout to the item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListAdapter.ViewHolder {

        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val itemView = inflater.inflate(R.layout.row_item, parent, false)
        return ViewHolder(itemView)
    }

    // Populate layout with data from a TrackedItem
    override fun onBindViewHolder(holder: ItemListAdapter.ViewHolder, position: Int) {
        val item: TrackedItem = items[position]

        holder.itemName.text = item.name

        // When the "locate" button is pressed, have the activity start tracking this item
        holder.locateButton.setOnClickListener {
            activity.setTarget(item)
        }

        // TODO: implement reference images
//        if (item.refImage != null) {
//            val bytes = item.refImage!!.bytes!!
//            val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//            holder.itemRefImage.setImageBitmap(bm)
//        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}