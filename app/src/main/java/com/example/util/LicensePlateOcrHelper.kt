package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume

object LicensePlateOcrHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Recognizes text from a Bitmap, targeting vehicle registration plate formats.
     */
    suspend fun recognizePlateFromBitmap(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detected = extractBestPlateNumber(visionText.text)
                    continuation.resume(detected)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    /**
     * Recognizes text from an image Uri (from Photo Picker or Camera capture file).
     */
    suspend fun recognizePlateFromUri(context: Context, uri: Uri): String? = suspendCancellableCoroutine { continuation ->
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val detected = extractBestPlateNumber(visionText.text)
                        continuation.resume(detected)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } else {
                continuation.resume(null)
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    /**
     * Parses and cleans raw OCR text to extract the most accurate vehicle registration plate.
     */
    fun extractBestPlateNumber(rawText: String): String {
        if (rawText.isBlank()) return ""

        val lines = rawText.lines()
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }

        // 1. Check for standard Indian plate formats (e.g. TS09AB1234, KA 01 MH 9999, DL 3C AB 1234, etc.)
        val indianPlateRegex = Regex("([A-Z]{2}\\s?[0-9]{1,2}\\s?[A-Z]{1,3}\\s?[0-9]{4})")
        for (line in lines) {
            val cleanLine = line.replace("-", " ").replace(".", " ").replace("•", " ")
            val match = indianPlateRegex.find(cleanLine)
            if (match != null) {
                return match.value.replace(" ", "").trim()
            }
        }

        // 2. Check for general alphanumeric plate patterns (4 to 12 chars, letters and digits)
        val genericPlateRegex = Regex("([A-Z0-9]{4,12})")
        for (line in lines) {
            val stripped = line.replace(Regex("[^A-Z0-9]"), "")
            if (stripped.length in 4..12 && stripped.any { it.isDigit() } && stripped.any { it.isLetter() }) {
                return stripped
            }
        }

        // 3. Fallback: longest alphanumeric substring or top line stripped
        val bestCandidate = lines
            .map { it.replace(Regex("[^A-Z0-9]"), "") }
            .filter { it.length >= 3 }
            .maxByOrNull { it.length }

        return bestCandidate ?: rawText.trim().replace("\n", " ").take(15)
    }
}
