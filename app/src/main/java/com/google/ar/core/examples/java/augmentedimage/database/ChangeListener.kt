package com.google.ar.core.examples.java.augmentedimage.database

import io.realm.Realm
import io.realm.RealmResults
import io.realm.OrderedCollectionChangeSet

import io.realm.OrderedRealmCollectionChangeListener

import android.app.Activity
import android.util.Log
import com.google.ar.core.examples.java.augmentedimage.AugmentedImageActivity
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import com.google.ar.core.examples.java.augmentedimage.classifier.Classifier

// Listens for changes to the realm and populates the Classifier
// keypoint database accordingly.
class ChangeListener(var realm : Realm, var activity : AugmentedImageActivity) : Runnable {
    override fun run() {
        val objects: RealmResults<TrackedItem> =
            realm.where(TrackedItem::class.java).findAllAsync()
        objects.addChangeListener { databaseEntries, changeSet ->
            // New TrackedItems show up here
            val changeIndexes = changeSet.changes
            val changedItems = changeIndexes.map { idx -> databaseEntries[idx] }

            Log.v("Database", "in changelistener")
            if(Classifier.allDescriptors.isEmpty()) {
                // No data has been inserted yet, use databaseEntries to populate
                databaseEntries.forEach { item ->
                    Log.v("Database", "changed" + item!!)
                    if(item != null) {
                        val descriptors = item.images.map { OpenCVHelpers.getDescriptors(it) }
                        Classifier.allDescriptors[item] = descriptors.toMutableList()
                        Log.v("Database", "Added ${item.name} to classifier with ${descriptors.size} descriptors")
                        activity.displayImage(item.images[0])
                    }
                }
            } else {
                // TODO:
                // Only process the changeset
            }
        }
    }
}