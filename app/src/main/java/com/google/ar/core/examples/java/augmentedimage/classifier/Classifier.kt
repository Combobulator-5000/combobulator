package com.google.ar.core.examples.java.augmentedimage.classifier

import android.media.Image
import android.util.Log
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import com.google.ar.core.examples.java.augmentedimage.database.TrackedItem
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.features2d.FlannBasedMatcher

class Classifier {
    companion object {
        // Maps each known item to an array of its descriptors
        var allDescriptors : HashMap<TrackedItem, MutableList<Mat>> = HashMap();
    }

//    private val objects : MutableList<TrackedItem> = ArrayList()
//
//    fun addObjects(newObjects : List<TrackedItem>) {
//        objects.addAll(newObjects)
//    }

    // Returns the TrackedItem that is the most visually similar to `image`
    fun evaluate(image : Image) : TrackedItem {
        return evaluate(OpenCVHelpers.imageToMat(image))
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
        return referenceDescriptors.maxOf {
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