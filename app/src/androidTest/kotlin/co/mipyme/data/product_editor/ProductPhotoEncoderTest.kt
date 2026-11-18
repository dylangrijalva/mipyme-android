package co.mipyme.data.product_editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPhotoEncoderTest {
    @Test
    fun encodesOrientedContentUriAsWebp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "product-photos").apply { mkdirs() }
        val input = File.createTempFile("tangerines-", ".jpg", directory)
        try {
            val jpeg = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888)
            try {
                input.outputStream().use { assertTrue(jpeg.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            } finally {
                jpeg.recycle()
            }
            ExifInterface(input.path).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.product-photo", input)

            val webp = ProductPhotoEncoder(context.contentResolver).encode(uri).getOrThrow()

            assertTrue(webp.size in 12..(5 * 1024 * 1024))
            assertEquals("RIFF", webp.decodeToString(endIndex = 4))
            assertEquals("WEBP", webp.decodeToString(startIndex = 8, endIndex = 12))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(webp, 0, webp.size, bounds)
            assertEquals(16, bounds.outWidth)
            assertEquals(32, bounds.outHeight)
        } finally {
            input.delete()
        }
    }
}
