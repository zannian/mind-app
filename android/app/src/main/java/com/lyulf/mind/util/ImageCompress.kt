package com.lyulf.mind.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * 上传前压缩：长边不超过 [maxSide]，JPEG quality≈[quality]。
 * ImageDecoder 会按 EXIF 纠正朝向。
 */
object ImageCompress {
    data class Result(
        val bytes: ByteArray,
        val mime: String = "image/jpeg",
        val filename: String = "photo.jpg",
    )

    fun compress(
        context: Context,
        uri: Uri,
        maxSide: Int = 1600,
        quality: Int = 82,
    ): Result {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // compress() 不支持 HARDWARE bitmap
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val w = info.size.width
            val h = info.size.height
            val long = maxOf(w, h)
            if (long > maxSide) {
                val scale = maxSide.toFloat() / long
                decoder.setTargetSize(
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                )
            }
        }
        val out = ByteArrayOutputStream()
        val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 95), out)
        if (!bitmap.isRecycled) bitmap.recycle()
        if (!ok) error("压缩失败")
        val bytes = out.toByteArray()
        if (bytes.isEmpty()) error("压缩结果为空")
        return Result(bytes = bytes)
    }
}
