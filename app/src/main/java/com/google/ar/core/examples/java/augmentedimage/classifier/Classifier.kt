package com.google.ar.core.examples.java.augmentedimage.classifier

import android.media.Image
import android.util.Log
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import com.google.ar.core.examples.java.augmentedimage.UI
import com.google.ar.core.examples.java.augmentedimage.database.TrackedItem
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.features2d.FlannBasedMatcher
import java.io.*

class Classifier {

    companion object {
        // Maps each known item to an array of its descriptors
        var allDescriptors : HashMap<TrackedItem, MutableList<Mat>> = HashMap();
        const val TAG = "classifier"
        const val DISTANCE_FACTOR = 0.7
    }

    private val objects : MutableList<TrackedItem> = ArrayList()
    private val flann : FlannBasedMatcher = FlannBasedMatcher()

    val allObjScores : MutableMap<TrackedItem, List<Int>> = HashMap()

    fun linkObjectsToUI(ui: UI) {
        ui.setObjectList(allDescriptors.keys.toList())
    }

    // Returns the TrackedItem that is the most visually similar to `image`
    fun evaluate(image : Image) : TrackedItem {
        return evaluate(OpenCVHelpers.imageToMat(image))
    }

    fun loadMatcherParams(inputStream: InputStream) {

        // Can only access assets via asset manager, but can only read parameters using a string path name.
        // So must copy the asset to a temporary file first.
        val outputF = File.createTempFile("FlannfDetectorParams", ".YAML")
        val outputStream = FileOutputStream(outputF)
        inputStream.copyTo(outputStream)

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        Log.d(TAG, outputF.readBytes().toString())

        flann.read(outputF.path)
    }

    fun evaluate(image : Mat) : TrackedItem {
        val targetDescriptors = OpenCVHelpers.getDescriptors(image)

        // Keeping the full list here (rather than a call to maxByOrNull)
        // in case we want to provide the user with a list of the next few top options
        val itemsWithScores = allDescriptors.keys.map { item ->
            var score = getMatchScore(targetDescriptors, item)
            Pair(item, score)
        }.sortedByDescending { pair -> pair.second }

        Log.d("Classifier", itemsWithScores.joinToString { "\n" })

        return itemsWithScores[0].first
    }

    // evaluates the
    fun getMatchScore(targetDescriptors : Mat, item : TrackedItem) : Int {
        var referenceDescriptors = allDescriptors[item]!!

        allObjScores[item] = referenceDescriptors.map {countMatches(targetDescriptors, it)}
        return referenceDescriptors.maxOf {
            countMatches(targetDescriptors, it)
        }
    }

    fun countMatches(descriptors1 : Mat, descriptors2: Mat) : Int {

        val matches : MutableList<MatOfDMatch> = ArrayList()
        flann.knnMatch(descriptors1, descriptors2, matches, 2)
        Log.d("Classifier", "done")

        var goodMatchesCount = 0

        for (match in matches) {
            val (m,n) = match.toList()
            if (m.distance < DISTANCE_FACTOR * n.distance) {
                goodMatchesCount+=1
            }
        }

        Log.d("Classifier", "good matches: $goodMatchesCount")
        return goodMatchesCount
    }
}