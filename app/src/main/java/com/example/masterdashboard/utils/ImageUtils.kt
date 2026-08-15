package com.example.masterdashboard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Compresses and resizes an image from a Uri.
     * Returns a ByteArray ready for Firebase Storage upload.
     */
    fun compressImage(context: Context, imageUri: Uri, quality: Int = 70, maxDimension: Int = 800): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 1. Calculate new dimensions while maintaining aspect ratio
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val (newWidth, newHeight) = if (width > height) {
                if (width > maxDimension) {
                    val ratio = maxDimension.toFloat() / width
                    maxDimension to (height * ratio).toInt()
                } else width to height
            } else {
                if (height > maxDimension) {
                    val ratio = maxDimension.toFloat() / height
                    (width * ratio).toInt() to maxDimension
                } else width to height
            }

            // 2. Resize
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // 3. Compress
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            
            val result = outputStream.toByteArray()
            
            // Cleanup
            originalBitmap.recycle()
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
