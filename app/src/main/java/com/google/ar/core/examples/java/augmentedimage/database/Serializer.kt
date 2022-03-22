package com.google.ar.core.examples.java.augmentedimage.database

import android.graphics.BitmapFactory
import android.media.Image
import android.media.ImageWriter
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.augmentedimage.OpenCVHelpers
import io.realm.RealmObject
import org.bson.types.Binary
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class DBUtil {
    class ImageBytes(val bytes : ByteArray, val width : Int, val height : Int) : RealmObject() {
    }

    companion object {
        fun serialize(pose : Pose): FloatArray {
            val out = FloatArray(12)
            pose.getTranslation(out, 0)
            pose.getRotationQuaternion(out, 3)

            return out
        }

        fun deserialize_pose(pose : FloatArray): Pose {
            val trans = pose.sliceArray(IntRange(0, 2))
            val quat = pose.sliceArray(IntRange(3, 6))
            return Pose(trans, quat)
        }

        fun serialize(image : Image) : ImageBytes {
            val frame = image.planes[0].buffer
            val bytebuffer = frame.duplicate().clear()
            val int_size = 4
            val bytearray = ByteArray(frame.capacity() + int_size * 2)
            frame.get(bytearray)
            return ImageBytes(bytearray, image.width, image.height)
        }

        fun deserialize_mat(img : ImageBytes) : Mat {
            val mat = Mat(img.height, img.width, CvType.CV_8UC4)
            mat.put(0, 0, img.bytes)
            return mat
        }
    }
}