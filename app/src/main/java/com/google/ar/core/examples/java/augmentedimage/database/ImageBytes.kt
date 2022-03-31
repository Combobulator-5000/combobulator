package com.google.ar.core.examples.java.augmentedimage.database

import android.util.Log
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmField
import io.realm.annotations.Required
import org.bson.types.ObjectId
import java.util.*

open class ImageBytes(var bytes : ByteArray = ByteArray(0), var width : Int = 0, var height : Int = 0, @Required var deployment : String = "plab") : RealmObject() {
    @PrimaryKey
    @RealmField("_id") private var _id: ObjectId = ObjectId()
}
