package com.enph.plab.java.combobulator.classifier

import android.media.Image
import android.util.Log
import com.enph.plab.java.combobulator.OpenCVHelpers
import com.enph.plab.java.combobulator.ui.UI
import com.enph.plab.java.combobulator.database.TrackedItem
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.features2d.FlannBasedMatcher
import java.io.*

class Classifier {

    companion object {
        // Maps each known item to an array of its descriptors
        var allDescriptors : HashMap<TrackedItem, MutableList<Mat>> = HashMap();

        // A separate list kept for the purposes of dynamically updatable UI
        var allObjects : MutableList<TrackedItem> = ArrayList()

        const val TAG = "classifier"
        const val DISTANCE_RATIO = 0.7

        @Synchronized
        fun addItem(item: TrackedItem) {
            val descriptors = item.images.map { OpenCVHelpers.getDescriptors(it) }
            allObjects.add(item)
            allDescriptors[item] = descriptors.toMutableList()
            Log.v("Database", "Added ${item.name} to classifier with ${descriptors.size} descriptors")
        }
    }

    private val flann : FlannBasedMatcher = FlannBasedMatcher()

    val allObjScores : MutableMap<TrackedItem, List<Int>> = HashMap()

    fun linkObjectsToUI(ui: UI) {
        ui.setObjectList(allObjects)
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
        Log.d("Classifier", itemsWithScores[0].first.name)

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

        var goodMatchesCount = 0

        for (match in matches) {
            val (m,n) = match.toList()
            if (m.distance < DISTANCE_RATIO * n.distance) {
                goodMatchesCount+=1
            }
        }
        return goodMatchesCount
    }
}