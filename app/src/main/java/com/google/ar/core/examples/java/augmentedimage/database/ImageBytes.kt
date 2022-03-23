package com.google.ar.core.examples.java.augmentedimage.database

import io.realm.RealmModel
import io.realm.RealmObject

open class ImageBytes(var bytes : ByteArray? = null, var width : Int? = null, var height : Int? = null) : RealmObject() {
}
