package com.google.ar.core.examples.java.augmentedimage.classifier

import android.media.Image
import android.util.Log
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.features2d.FlannBasedMatcher
import java.io.*

class Classifier {

    companion object {
        const val TAG = "classifier"
        const val DISTANCE_FACTOR = 0.85
    }

    private val objects : MutableList<TrackedItem> = ArrayList()
    private val flann : FlannBasedMatcher = FlannBasedMatcher()

    val allObjScores : MutableMap<TrackedItem, List<Int>> = HashMap()

    fun addObjects(newObjects : List<TrackedItem>) {
        objects.addAll(newObjects)
    }

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
        val descriptors = OpenCVHelpers.getDescriptors(image)

        // Keeping the full list here (rather than a call to maxByOrNull)
        // in case we want to provide the user with a list of the next few top options
        val objectsWithScores = objects.map {
            Pair(it, getMatchScore(descriptors, it))
        }.sortedByDescending {it.second}

        Log.d("Classifier", objectsWithScores.joinToString { "\n" })

        return objectsWithScores[0].first
    }

    // evaluates the
    fun getMatchScore(targetDescriptors : Mat, obj : TrackedItem) : Int {

        allObjScores[obj] = obj.allDescriptors.map {countMatches(targetDescriptors, it)}

        return obj.allDescriptors.maxOf {
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