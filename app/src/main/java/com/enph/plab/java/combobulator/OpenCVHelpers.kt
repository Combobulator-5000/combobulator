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

//        fun imageToBitmap(image: Image) : Bitmap {
//            val buffer = image.planes[0].buffer
//            val bytes = ByteArray(buffer.capacity())
////            buffer[bytes]
//            (buffer.duplicate().clear() as ByteBuffer)[bytes]
//
//
////            UI.miscData["buffer"] = bytes.joinToString(" ")
//            val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//
//            return bitmapImage
//        }

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
//            val width = image.width
//            val height = image.height
//            val frame = image.planes[0].buffer
//            val data = ByteArray(frame.capacity())
//            (frame.duplicate().clear() as ByteBuffer)[data]

            try {
                val data = YUV_420_888toNV21(image)
                val width = image.width
                val height = image.height

                val yuv = YuvImage(data, ImageFormat.NV21, width, height, null)
                val out = ByteArrayOutputStream()

                yuv.compressToJpeg(Rect(0,0,width, height), 50, out)

                val bytes = out.toByteArray()
                val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val mat = Mat()

                Utils.bitmapToMat(bm, mat)
                return mat

            } catch (e: Exception){
                Log.e("OpenCV", e.stackTraceToString())
                throw e
            }
        }



//        // Converts Java Image to OpenCV Mat
//        fun imageToMat(image: Image): Mat {
//
//            val width = image.width
//            val height = image.height
//            val frame = image.planes[0].buffer
//            val data = ByteArray(frame.capacity())
//            (frame.duplicate().clear() as ByteBuffer)[data]
//
//            try {
//                val yuv = YuvImage(data, ImageFormat.NV21, width, height, null)
//                val out = ByteArrayOutputStream()
//
//                yuv.compressToJpeg(Rect(0,0,width, height), 100, out)
//
//                val bytes = out.toByteArray()
//                val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//
//                val mat = Mat()
//
//                Utils.bitmapToMat(bm, mat)
////                val mat = Mat(image.height, image.width, CvType.CV_8UC3)
////                mat.put(0, 0, bytes)
//                return mat
//
//            } catch (e: Exception){
//                Log.e("OpenCV", e.stackTraceToString())
//                throw e
//            }



            // Create a buffered image with transparency

//            // Create a buffered image with transparency
//            val bimage: java.awt.image.BufferedImage = java.awt.image.BufferedImage(
//                img.getWidth(null),
//                img.getHeight(null),
//                java.awt.image.BufferedImage.TYPE_INT_ARGB
//            )


            // Construct OpenCV Mat object from Image
//            val frame = image.planes[0].buffer
//            val data = ByteArray(frame.capacity())
//            (frame.duplicate().clear() as ByteBuffer)[data]

//        }

        // Converts OpenCV Mat to ByteBuffer (necessary if we want to render any OpenCV images on screen)
        fun matToByteBuffer(mat: Mat): ByteBuffer {
            val buffer = ByteArray(mat.total().toInt() * mat.channels())
            mat[0, 0, buffer]
            return ByteBuffer.wrap(buffer)
        }
    }
}