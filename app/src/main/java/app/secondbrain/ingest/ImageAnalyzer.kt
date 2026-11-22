package app.secondbrain.ingest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ImageAnalysis(val text: String?, val labels: List<String>, val failed: Boolean)

/**
 * On-device OCR and generic object/scene labelling with bundled ML Kit models.
 * These are small discriminative models, not generative ones, and need no network.
 */
@Singleton
class ImageAnalyzer @Inject constructor() {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.6f).build())
    }

    suspend fun analyze(path: String): ImageAnalysis = withContext(Dispatchers.Default) {
        val bitmap = decode(path) ?: return@withContext ImageAnalysis(null, emptyList(), failed = true)
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            var failed = false
            val text = runCatching { recognizer.process(image).await().text.trim() }
                .onFailure { failed = true }.getOrNull()?.takeIf { it.isNotEmpty() }
            val labels = runCatching { labeler.process(image).await().sortedByDescending { it.confidence }.map { it.text } }
                .onFailure { failed = true }.getOrDefault(emptyList())
            ImageAnalysis(text, labels, failed)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decode(path: String): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val raw = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
    }.getOrNull()

    private companion object {
        const val MAX_SIDE = 2048
    }
}
