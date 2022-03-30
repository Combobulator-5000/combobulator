package com.google.ar.core.examples.java.augmentedimage.classifier

import android.R.attr
import android.content.Context
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import android.view.View
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.FlannBasedMatcher
import org.opencv.features2d.SIFT
import java.lang.NullPointerException
import android.R.attr.path
import java.io.*
import java.nio.file.Files

class Classifier {

    companion object {
        const val TAG = "classifier"
        const val DISTANCE_FACTOR = 0.85
    }

    private val objects : MutableList<DatabaseObject> = ArrayList()
    private val flann : FlannBasedMatcher = FlannBasedMatcher()

    val allObjScores : MutableMap<DatabaseObject, List<Int>> = HashMap()

    fun addObjects(newObjects : List<DatabaseObject>) {
        objects.addAll(newObjects)
    }

    fun evaluate(image : Image) : DatabaseObject {
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

    fun evaluate(image : Mat) : DatabaseObject {
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
    fun getMatchScore(targetDescriptors : Mat, obj : DatabaseObject) : Int {

        allObjScores[obj] = obj.allDescriptors.map {countMatches(targetDescriptors, it)}

        return obj.allDescriptors.maxOf {
            countMatches(targetDescriptors, it)
        }
    }

    fun countMatches(descriptors1 : Mat, descriptors2: Mat) : Int {
        val matches : MutableList<MatOfDMatch> = ArrayList()


//        FLANN_INDEX_KDTREE = 1
//        index_params = dict(algorithm = FLANN_INDEX_KDTREE, trees = 5)
//        search_params = dict(checks=50)   # or pass empty dictionary

        flann.knnMatch(descriptors1, descriptors2, matches, 2)
        Log.d("Classifier", "done")

        var goodMatchesCount = 0

        for ((i, match) in matches.withIndex()) {
            val (m,n) = match.toList()
            if (m.distance < DISTANCE_FACTOR * n.distance) {
                goodMatchesCount+=1
            }
        }

        Log.d("Classifier", "good matches: $goodMatchesCount")
        return goodMatchesCount
    }
}