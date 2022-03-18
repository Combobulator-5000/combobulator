package com.google.ar.core.examples.java.augmentedimage.classifier

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
import java.io.IOException
import java.io.InputStream
import java.lang.NullPointerException

class Classifier {

    private val objects : MutableList<DatabaseObject> = ArrayList()

    fun addObjects(newObjects : List<DatabaseObject>) {
        objects.addAll(newObjects)
    }

    fun evaluate(image : Image) : DatabaseObject {
        return evaluate(OpenCVHelpers.imageToMat(image))
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
        return obj.allDescriptors.maxOf {
            countMatches(targetDescriptors, it)
        }
    }

    fun countMatches(descriptors1 : Mat, descriptors2: Mat) : Int {
        val matches : MutableList<MatOfDMatch> = ArrayList()

        val flann = FlannBasedMatcher()
        flann.knnMatch(descriptors1, descriptors2, matches, 2)
        Log.d("Classifier", "done")

        var goodMatchesCount = 0

        for ((i, match) in matches.withIndex()) {
            val (m,n) = match.toList()
            if (m.distance < 0.7*n.distance) {
                goodMatchesCount+=1
            }
        }

        Log.d("Classifier", "good matches: $goodMatchesCount")
        return goodMatchesCount
    }
}