package com.enph.plab.java.combobulator.database

import android.media.Image
import com.google.ar.core.Pose
import com.enph.plab.java.combobulator.OpenCVHelpers
import io.realm.RealmList
import org.opencv.core.CvType
import org.opencv.core.Mat

class DBUtil {
    companion object {
        fun serialize(pose : Pose): RealmList<Float> {
            val buf = FloatArray(12)
            pose.getTranslation(buf, 0)
            pose.getRotationQuaternion(buf, 3)

            return toRealmList(buf.toList())
        }

        fun <E> toRealmList(data : List<E>) : RealmList<E> {
            val out = RealmList<E>()
            out.addAll(data)
            return out
        }

        fun deserialize_pose(pose : List<Float>): Pose {
            val trans = pose.slice(IntRange(0, 2)).toFloatArray()
            val quat = pose.slice(IntRange(3, 6)).toFloatArray()
            return Pose(trans, quat)
        }

        fun serialize(image : Image) : ImageBytes {
            val frame = image.planes[0].buffer
            val bytebuffer = frame.duplicate().clear()
            val bytearray = ByteArray(frame.capacity())
            frame.get(bytearray)
            return ImageBytes(bytearray, image.width, image.height)
        }

        fun serialize(mat : Mat) : ImageBytes {
            val width = mat.width()
            val height = mat.height()
            val bytes = OpenCVHelpers.matToByteBuffer(mat)
            val bytearray = ByteArray(bytes.capacity())
            bytes.get(bytearray)
            return ImageBytes(bytearray, width, height)
        }

        fun deserialize_mat(img : ImageBytes) : Mat {
            val mat = Mat(img.height!!, img.width!!, CvType.CV_8UC4)
            mat.put(0, 0, img.bytes!!)
            return mat
        }

        fun imageListToRawData(images: MutableList<Mat>): RealmList<ImageBytes> {
            return toRealmList(images.map { img -> DBUtil.serialize(img) })
        }
    }
}