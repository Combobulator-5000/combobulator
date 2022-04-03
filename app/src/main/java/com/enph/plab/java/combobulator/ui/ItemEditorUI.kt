package com.enph.plab.java.combobulator.ui

import com.enph.plab.java.combobulator.databinding.ItemEditorBinding
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.R
import com.enph.plab.java.combobulator.classifier.Classifier
import com.enph.plab.java.combobulator.database.TrackedItem
import com.enph.plab.java.combobulator.databinding.ItemListBinding
import com.google.ar.core.Pose

//RecyclerView.ViewHolder(itemView)

class ItemEditorUI(parent: UI) : Fragment(R.layout.item_editor) {

    lateinit var activity: CombobulatorMainActivity
    lateinit var binding: ItemEditorBinding
    lateinit var itemAdapter: RowItemAdapter

    companion object {
        const val layout = R.layout.item_editor
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

//        itemAdapter = RowItemAdapter(activity, Classifier.allObjects)
//        binding.rvItemList.adapter = itemAdapter
//        binding.rvItemList.layoutManager = LinearLayoutManager(activity)
//
//        binding.swipeContainer.setOnRefreshListener(this)

//        addItem()

        return view
    }


//    fun openEditor(item: TrackedItem) {
//        // TODO
//    }
}