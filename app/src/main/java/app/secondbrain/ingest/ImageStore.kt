package app.secondbrain.ingest

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the original bytes of every saved image in app-private storage. */
@Singleton
class ImageStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val dir get() = File(context.filesDir, "images").apply { mkdirs() }
    private val captureDir get() = File(context.cacheDir, "capture").apply { mkdirs() }

    /** Copies [uri] untouched into private storage and returns the absolute path. */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri)
        val ext = when {
            mime == "image/png" -> "png"
            mime == "image/webp" -> "webp"
            mime == "image/gif" -> "gif"
            else -> "jpg"
        }
        val target = File(dir, "${UUID.randomUUID()}.$ext")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: error("Cannot open $uri")
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
        target.absolutePath
    }

    fun newCaptureTarget(): Pair<File, Uri> {
        val file = File(captureDir, "${UUID.randomUUID()}.jpg")
        return file to FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}
