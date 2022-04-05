package com.enph.plab.java.combobulator

import android.content.Context
import android.graphics.*
import android.media.Image
import android.util.Log
import com.enph.plab.java.combobulator.ui.UI
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.SIFT
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer




class OpenCVHelpers {

    companion object {

        private val sift: SIFT = SIFT.create()

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

        private fun YUV_420_888toNV21(image: Image): ByteArray? {
            val nv21: ByteArray
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            nv21 = ByteArray(ySize + uSize + vSize)

            //U and V are swapped
            yBuffer[nv21, 0, ySize]
            vBuffer[nv21, ySize, vSize]
            uBuffer[nv21, ySize + vSize, uSize]
            return nv21
        }

        fun imageToMat(image: Image):Mat {

                val data = YUV_420_888toNV21(image)

                val yuv = YuvImage(data, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuv.compressToJpeg(Rect(0,0, image.width, image.height), 50, out)

                val bytes = out.toByteArray()
                val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val mat = Mat()
                Utils.bitmapToMat(bm, mat)
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