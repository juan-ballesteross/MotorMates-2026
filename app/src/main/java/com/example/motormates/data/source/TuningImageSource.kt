package com.example.motormates.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.motormates.data.model.TuningImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Carga y prepara imágenes para Gemini: las reduce a [MAX_SIDE_PX] y las
 * comprime a JPEG (el límite es 7 MB por imagen; así quedan en ~200-500 KB
 * y la petición sale más rápido).
 */
interface TuningImageSource {
    suspend fun loadFromUrl(url: String): TuningImage
    suspend fun loadFromUri(uri: Uri): TuningImage

    /**
     * Recorta la pieza de su fondo con ML Kit Subject Segmentation (en el
     * dispositivo) y la pone sobre fondo blanco, como foto de producto. Si
     * el modelo no está disponible, devuelve la imagen tal cual.
     */
    suspend fun removeBackground(image: TuningImage): TuningImage
}

class AndroidTuningImageSource @Inject constructor(
    @ApplicationContext private val context: Context
) : TuningImageSource {

    override suspend fun loadFromUrl(url: String): TuningImage {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // hace falta un bitmap leíble para comprimirlo
            .size(MAX_SIDE_PX)
            .build()
        return when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> withContext(Dispatchers.Default) {
                result.drawable.toBitmap().scaledDown().toJpeg()
            }
            is ErrorResult -> throw result.throwable
        }
    }

    override suspend fun loadFromUri(uri: Uri): TuningImage = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalArgumentException("No se pudo leer la imagen")
        bitmap.scaledDown().toJpeg()
    }

    override suspend fun removeBackground(image: TuningImage): TuningImage {
        val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size) ?: return image
        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
        )
        return try {
            val result = segmenter.process(InputImage.fromBitmap(bitmap, 0)).await()
            val foreground = result.foregroundBitmap ?: return image
            withContext(Dispatchers.Default) { foreground.onWhite().toJpeg() }
        } catch (e: Exception) {
            // Sin Google Play services o sin el modelo descargado: se sigue
            // con la foto original, la edición funciona igual.
            image
        } finally {
            segmenter.close()
        }
    }

    private fun Bitmap.scaledDown(): Bitmap {
        val longest = max(width, height)
        if (longest <= MAX_SIDE_PX) return this
        val scale = MAX_SIDE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    /** El recorte trae fondo transparente; JPEG no tiene alfa, así que se pinta en blanco. */
    private fun Bitmap.onWhite(): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(this@onWhite, 0f, 0f, null)
        }
        return output
    }

    private fun Bitmap.toJpeg(): TuningImage {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return TuningImage(stream.toByteArray())
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        return sample
    }

    companion object {
        const val MAX_SIDE_PX = 1536
        private const val JPEG_QUALITY = 88
    }
}
