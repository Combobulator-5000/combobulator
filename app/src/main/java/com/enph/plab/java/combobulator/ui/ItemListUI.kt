package com.enph.plab.java.combobulator.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
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

class ItemListUI(val parent: UI) : Fragment(R.layout.item_list), SwipeRefreshLayout.OnRefreshListener {

    lateinit var activity: CombobulatorMainActivity
    lateinit var binding: ItemListBinding
    lateinit var itemAdapter: RowItemAdapter

    companion object {
        const val layout = R.layout.item_list
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity = getActivity() as CombobulatorMainActivity
        // Inflate the layout for this fragment
        val view = inflater.inflate(layout, container, false)
        binding = ItemListBinding.bind(view)

        // Set up RecyclerView with list of database items
        itemAdapter = RowItemAdapter(activity, Classifier.allObjects)
        binding.rvItemList.adapter = itemAdapter
        binding.rvItemList.layoutManager = LinearLayoutManager(activity)

        binding.newItem.setOnClickListener {
            parent.itemEditorFragment.newItem()
            parent.loadFragment(parent.itemEditorFragment)
        }

        binding.swipeContainer.setOnRefreshListener(this)

        // Clicking "back" returns user to main tracking screen
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            parent.hideFragment()
        }

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onRefresh() {
        binding.swipeContainer.isRefreshing = true

        // TODO: check database for updates

        // TODO: only notify about changes, not whole dataset
        itemAdapter.notifyDataSetChanged()
        binding.swipeContainer.isRefreshing = false
    }
}