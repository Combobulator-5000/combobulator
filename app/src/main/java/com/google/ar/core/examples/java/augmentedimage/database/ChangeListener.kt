package com.google.ar.core.examples.java.augmentedimage.database

import io.realm.Realm
import io.realm.RealmResults
import io.realm.OrderedCollectionChangeSet

import io.realm.OrderedRealmCollectionChangeListener

import android.app.Activity
import android.util.Log

// Listens for changes to the realm and populates the Classifier
// keypoint database accordingly.
class ChangeListener(var realm : Realm) : Runnable {
    override fun run() {
        val objects: RealmResults<TrackedItem> =
            realm.where(TrackedItem::class.java).findAllAsync()
        objects.addChangeListener { databaseEntries, changeSet ->
            // New TrackedItems show up here
            val changeIndexes = changeSet.changes
            val changedItems = changeIndexes.map { idx -> databaseEntries[idx] }
            changedItems.forEach { item ->
                Log.v("Database", "changed" + item!!)
            }
        }
    }
}