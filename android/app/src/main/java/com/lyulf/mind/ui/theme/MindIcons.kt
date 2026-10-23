package com.lyulf.mind.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 对照原型 tabbar SVG */
object MindIcons {
    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "mind_chat",
            defaultWidth = 26.dp,
            defaultHeight = 26.dp,
            viewportWidth = 26f,
            viewportHeight = 26f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(21.6f, 12.7f)
                curveTo(21.6f, 16.6f, 17.7f, 19.8f, 13f, 19.8f)
                curveTo(12f, 19.8f, 11f, 19.7f, 10.1f, 19.4f)
                lineTo(5.4f, 21f)
                lineTo(6.5f, 17.6f)
                curveTo(5.1f, 16.3f, 4.3f, 14.5f, 4.3f, 12.6f)
                curveTo(4.3f, 8.7f, 8.2f, 5.5f, 12.9f, 5.5f)
                curveTo(17.6f, 5.5f, 21.6f, 8.7f, 21.6f, 12.7f)
                close()
            }
        }.build()
    }

    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "mind_heart",
            defaultWidth = 26.dp,
            defaultHeight = 26.dp,
            viewportWidth = 26f,
            viewportHeight = 26f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
            ) {
                // M13 21.6s-7.6-4.9-7.6-10.1a4.4 4.4 0 0 1 7.6-3 4.4 4.4 0 0 1 7.6 3c0 5.2-7.6 10.1-7.6 10.1z
                moveTo(13f, 21.6f)
                curveToRelative(0f, 0f, -7.6f, -4.9f, -7.6f, -10.1f)
                curveToRelative(0f, -2.43f, 1.97f, -4.4f, 4.4f, -4.4f)
                curveToRelative(1.3f, 0f, 2.46f, 0.57f, 3.2f, 1.46f)
                curveToRelative(0.74f, -0.89f, 1.9f, -1.46f, 3.2f, -1.46f)
                curveToRelative(2.43f, 0f, 4.4f, 1.97f, 4.4f, 4.4f)
                curveToRelative(0f, 5.2f, -7.6f, 10.1f, -7.6f, 10.1f)
                close()
            }
        }.build()
    }

    val Stats: ImageVector by lazy {
        ImageVector.Builder(
            name = "mind_stats",
            defaultWidth = 26.dp,
            defaultHeight = 26.dp,
            viewportWidth = 26f,
            viewportHeight = 26f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6f, 20.5f)
                lineTo(6f, 12f)
                moveTo(13f, 20.5f)
                lineTo(13f, 5.5f)
                moveTo(20f, 20.5f)
                lineTo(20f, 15f)
            }
        }.build()
    }

    /** 日常右上角齿轮（对照原型 settings） */
    val Gear: ImageVector by lazy {
        ImageVector.Builder(
            name = "mind_gear",
            defaultWidth = 22.dp,
            defaultHeight = 22.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 15.1f)
                curveToRelative(1.71f, 0f, 3.1f, -1.39f, 3.1f, -3.1f)
                curveToRelative(0f, -1.71f, -1.39f, -3.1f, -3.1f, -3.1f)
                curveToRelative(-1.71f, 0f, -3.1f, 1.39f, -3.1f, 3.1f)
                curveToRelative(0f, 1.71f, 1.39f, 3.1f, 3.1f, 3.1f)
                close()
                moveTo(12f, 3.2f)
                verticalLineToRelative(2.1f)
                moveTo(12f, 18.7f)
                verticalLineToRelative(2.1f)
                moveTo(4.9f, 7.8f)
                lineToRelative(1.8f, 1f)
                moveTo(17.3f, 15.2f)
                lineToRelative(1.8f, 1f)
                moveTo(4.9f, 16.2f)
                lineToRelative(1.8f, -1f)
                moveTo(17.3f, 8.8f)
                lineToRelative(1.8f, -1f)
            }
        }.build()
    }
}
