package com.enph.plab.java.combobulator.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.enph.plab.java.combobulator.CombobulatorMainActivity
import com.enph.plab.java.combobulator.OpenCVHelpers
import com.enph.plab.java.combobulator.classifier.Classifier
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.NullPointerException
import kotlin.math.sqrt

// Abstraction layer between position lookup tables for augmentedImages/fiducials/tools and our code
class Workspace(fileName : String, val context : Context, private val imgdbPath : String?) {

    constructor(fileName: String, context: CombobulatorMainActivity) : this(fileName, context, null)

    companion object {
        const val TAG = "Workspace"

        // Used for parsing each JSON object
        val translationKeys = arrayOf("x", "y", "z")
        val rotationKeys = arrayOf("qx", "qy", "qz", "qw")

        // A constant transform to be applied to every pose, to account for the difference between
        // ARCore and our orientations of spatial coordinate axes.
        val extraRotation : Pose
        init {
            val halfSqrt2 = sqrt(2.0f)/2
            // equal to a 90 deg rotation about x axis
            extraRotation = Pose.makeRotation(halfSqrt2, 0f, 0f, halfSqrt2)
        }
    }

    private val poseMap : MutableMap<Int, Pose> = HashMap()
    private val markerList : JSONArray
    private val objectList : JSONArray

    init {
        // Load fiducial mapping
        val inputStream = context.assets.open(fileName)
        val out = inputStream.readBytes().toString(Charsets.UTF_8)
        val jsonObject = JSONObject(out)
        Log.v("Reading from json:", out)
        markerList = jsonObject.getJSONArray("markers")
        objectList = jsonObject.getJSONArray("objects")
    }

    fun getCalibrationPointPose(index: Int): Pose {
        return mapIdToLocation(index)
            ?: throw NullPointerException("index is not linked to any pose in the workspace")
    }

    fun setupTrackedItemDatabase() {
        for (i in 0 until objectList.length()) {
            val data = objectList.getJSONObject(i)


            try {
                val name = data.get("name") as String
                val pose = parsePoseFromJSON(data, translationOnly = true)

                val imagesPath = data.get("images_path") as String
                val allFiles = context.assets.list(imagesPath)

                if (allFiles != null) {
                    val images = allFiles.map { file ->
                        OpenCVHelpers.readImageMatFromAsset("$imagesPath/$file", context)
                    }.toMutableList()

                    val item = TrackedItem(name, pose, images)


                    if (data.has("reference_image")) {
                        val ref_image =OpenCVHelpers.readImageMatFromAsset(data.get("reference_image") as String, context)
                        item.locationRefImage = ref_image
                    }

                    Classifier.addItem(item)
                }



            } catch (e : JSONException) {
                Log.e(
                    TAG, "Could not add entry into database: " +
                        "JSON not properly formatted.", e
                )
            }


        }
    }


    fun setupAugmentedImagesWorkspace(session : Session) : AugmentedImageDatabase? {

        val augmentedImageDatabase = if (imgdbPath == null) {
            // Create a new AugmentedImageDatabase
            AugmentedImageDatabase(session)
        } else {
            // This is an alternative way to initialize an AugmentedImageDatabase instance,
            // load a pre-existing augmented image database.
            try {
                context.assets.open(imgdbPath).use { `is` ->
                    AugmentedImageDatabase.deserialize(session, `is`)
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO exception loading augmented image database.", e)
                return null
            }
        }


        // Parse data from JSON object
        for (i in 0 until markerList.length()) {
            val data = markerList.getJSONObject(i)

            try {
                val imageIndex = if (imgdbPath == null) {
                    // Add image to the database, and use the returned index as the key in location mapping
                    val imagePath: String = data.get("image") as String
                    val augmentedImageBitmap = loadAugmentedImageBitmap(imagePath)

                    if(data.has("width")) {
                        val width = (data.get("width") as Double).toFloat()
                        augmentedImageDatabase.addImage(imagePath, augmentedImageBitmap, width)
                    } else {
                        augmentedImageDatabase.addImage(imagePath, augmentedImageBitmap)
                    }
                } else i // if a database file is specified, assume the order is same as the workspace json

                poseMap[imageIndex] = parsePoseFromJSON(data)
            }
            catch (e : JSONException){
                Log.e(
                    TAG, "Could not add entry into workspace: " +
                            "JSON not properly formatted.", e
                )
            }
            catch (e : IOException) {
                Log.e(
                    TAG, "Could not add entry into workspace: " +
                            "IOException loading augmented image bitmap.", e
                )
            }
        }

        return augmentedImageDatabase
    }



    // Given a marker id, looks up the absolute position and orientation associated
    // with it in the workspace database (returns null if the marker is not in the database).
    private fun mapIdToLocation(id : Int): Pose? {
        return if(poseMap.containsKey(id)) poseMap[id] else null
    }

    private fun parsePoseFromJSON(data : JSONObject, translationOnly : Boolean = false) : Pose {
        val translation = Pose.makeTranslation(
            translationKeys.map { (data.get(it) as Double).toFloat() }.toFloatArray()
        )
        if (translationOnly) return translation;

        val rotation = Pose.makeRotation(
            rotationKeys.map { (data.get(it) as Double).toFloat() }.toFloatArray()
        )

        return translation.compose(rotation.compose(extraRotation))
    }


    private fun loadAugmentedImageBitmap(fileName: String): Bitmap {
        context.assets.open(fileName).use { `is` -> return BitmapFactory.decodeStream(`is`) }
    }

}