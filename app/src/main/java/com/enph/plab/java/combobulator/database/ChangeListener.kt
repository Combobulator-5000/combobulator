package com.enph.plab.java.combobulator.database

import io.realm.Realm
import io.realm.RealmResults

import android.util.Log
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.classifier.Classifier

// Listens for changes to the realm and populates the Classifier
// keypoint database accordingly.
class ChangeListener(var realm : Realm, var activity : CombobulatorMainActivity) : Runnable {
    override fun run() {
        val objects: RealmResults<RealmTrackedItem> =
            realm.where(RealmTrackedItem::class.java).findAllAsync()
        objects.addChangeListener { databaseEntries, changeSet ->
            // New TrackedItems show up here
            val changeIndexes = changeSet.changes
            val changedItems = changeIndexes.map { idx -> databaseEntries[idx] }

            Log.v("Database", "in changelistener")
            if(Classifier.allDescriptors.isEmpty()) {
                // No data has been inserted yet, use databaseEntries to populate
                databaseEntries.forEach { item ->
                    Log.v("Database", "changed" + item!!)
                    val localItem = item.toLocalTrackedItem()
                    Classifier.addItem(localItem)
                    activity.displayImage(localItem.images[0])
                }
            } else {
                // TODO:
                // Only process the changeset
            }
        }
    }
}