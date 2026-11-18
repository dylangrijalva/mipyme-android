package co.mipyme.data.product_editor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ProductPhotoEncoder(private val resolver: ContentResolver) {
    suspend fun encode(uri: Uri): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(encodeWebp(uri))
            } catch (error: IOException) {
                Result.failure(error)
            } catch (error: SecurityException) {
                Result.failure(error)
            } catch (error: IllegalArgumentException) {
                Result.failure(error)
            }
        }
    }

    private fun encodeWebp(uri: Uri): ByteArray {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Could not decode the photo")

        val orientation = open(uri).use { source ->
            try {
                ExifInterface(source).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (_: IOException) {
                ExifInterface.ORIENTATION_NORMAL
            }
        }
        options.inJustDecodeBounds = false
        val longestEdge = maxOf(options.outWidth, options.outHeight)
        var sampleSize = 1
        while (longestEdge / sampleSize > MAX_EDGE_PIXELS) {
            sampleSize *= 2
        }
        options.inSampleSize = sampleSize
        val decoded = open(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Could not decode the photo")

        try {
            val oriented = orient(decoded, orientation)
            try {
                return compress(oriented)
            } finally {
                if (oriented !== decoded) oriented.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }

    private fun open(uri: Uri): InputStream {
        return resolver.openInputStream(uri) ?: throw IOException("Could not open the photo")
    }

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @Suppress("DEPRECATION")
    private fun compress(bitmap: Bitmap): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        val output = ByteArrayOutputStream()
        for (quality in WEBP_QUALITIES) {
            output.reset()
            if (bitmap.compress(format, quality, output) && output.size() in 12..MAX_BYTES) {
                return output.toByteArray()
            }
        }
        throw IOException("Could not fit the photo within the upload size limit")
    }

    private companion object {
        private const val MAX_EDGE_PIXELS = 2048
        private const val MAX_BYTES = 5 * 1024 * 1024
        private val WEBP_QUALITIES = intArrayOf(85, 70, 50)
    }
}
