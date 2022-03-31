package com.google.ar.core.examples.java.augmentedimage

import android.content.Context
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.SIFT
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

class OpenCVHelpers {

    companion object {

        val sift = SIFT.create()

        fun getDescriptors(image : Mat) : Mat {
            val descriptors = Mat()
            sift.detectAndCompute(image, Mat(), MatOfKeyPoint(), descriptors)
            return descriptors
        }

        @JvmStatic
        fun readImageMatFromAsset(fileName : String, context : Context) : Mat {
            val assetManager = context.assets
            var stream = assetManager.open(fileName);
            val bitmap = BitmapFactory.decodeStream(stream);
            stream.close()

            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            return mat
        }

        // Converts Java Image to OpenCV Mat
        fun imageToMat(image: Image): Mat {
            // Construct OpenCV Mat object from Image
            val frame = image.planes[0].buffer
            val data = ByteArray(frame.capacity())
            (frame.duplicate().clear() as ByteBuffer)[data]
            val mat = Mat(image.height, image.width, CvType.CV_8UC4)
            mat.put(0, 0, data)
            return mat
        }

        // Converts OpenCV Mat to ByteBuffer (necessary if we want to render any OpenCV images on screen)
        fun matToByteBuffer(mat: Mat): ByteBuffer {
            val buffer = ByteArray(mat.total().toInt() * mat.channels())
            mat[0, 0, buffer]
            return ByteBuffer.wrap(buffer)
        }
    }
}