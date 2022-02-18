package com.google.ar.core.examples.java.augmentedimage.localization

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.ar.core.Pose
import org.json.JSONObject
import java.lang.NullPointerException
import kotlin.math.sqrt

// Abstraction layer between position lookup tables for augmentedImages/fiducials/tools and our code
class Workspace(fileName : String, context : Context) {

    companion object {
        const val TAG = "Workspace"
    }

    private val markerMap : JSONObject

    init {
        // Load fiducial mapping
        val assetManager: AssetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val out = inputStream.readBytes().toString(Charsets.UTF_8)
        Log.v("Reading from json:", out)
        markerMap = JSONObject(out)
    }

    // Given a marker id, looks up the absolute position and orientation associated
    // with it in the workspace database (returns null if the marker is not in the database).
    private fun mapIdToLocation(id : Int, map : JSONObject): Pose? {

        // An addtional rotation that seems to be necessary given how ARCore interprets poses of AugmentedImages
        // (equal to 90 deg rot about -y)
        val halfSqrt2 = sqrt(2.0f)/2
        val rotation2 = Pose.makeRotation(halfSqrt2, 0f, 0f, halfSqrt2)

        if (map.has(id.toString())) {
            val data = map.getJSONObject(id.toString());
            Log.v(TAG, data.toString())

            val translation = Pose.makeTranslation(
                (data.get("x") as Double).toFloat(),
                (data.get("y") as Double).toFloat(),
                (data.get("z") as Double).toFloat()
            )
            val rotation = Pose.makeRotation(
                (data.get("qx") as Double).toFloat(),
                (data.get("qy") as Double).toFloat(),
                (data.get("qz") as Double).toFloat(),
                (data.get("qw") as Double).toFloat()
            )
//            return translation.compose(rotation2.compose(rotation))
            return translation.compose(rotation.compose(rotation2))
        }
        Log.e(TAG, String.format("fiducial %s not in the workspace mapping", id))
        return null
    }

    fun getCalibrationPointPose(index: Int): Pose {
//        // An addtional rotation that seems to be necessary given how ARCore interprets poses of AugmentedImages
//        // (equal to 90 deg rot about -y)
//        val halfSqrt2 = sqrt(2.0f)/2
//        val rotation2 = Pose.makeRotation(0f, halfSqrt2, 0f, halfSqrt2)

//        val translation = Pose.makeTranslation(0f, 0f, 0f)
//        val rotation = Pose.makeRotation(-0.5f, -0.5f, -0.5f, -0.5f)
//        return translation.compose(rotation2.compose(rotation))

        return mapIdToLocation(index, markerMap)
            ?: throw NullPointerException("index is not linked to any pose in the workspace")
    }
}