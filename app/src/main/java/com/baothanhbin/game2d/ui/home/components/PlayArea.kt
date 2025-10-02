package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import com.baothanhbin.game2d.game.model.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Movie
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Khu vực chơi game - hiển thị enemies, bullets, effects
 */
@Composable
fun PlayArea(
    gameState: GameState,
    onForceCombat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val bulletKim: ImageBitmap = remember {
        // Load and chroma-key the bullet image to remove connected gray-ish background
        val raw = BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.bullet_kim)
            .copy(Bitmap.Config.ARGB_8888, true)
        val keyColor = raw.getPixel(0, 0)
        chromaKeyBitmap(raw, keyColor, tolerance = 60).asImageBitmap()
    }
    
    val bulletBang: ImageBitmap = remember {
        // Load and chroma-key the bullet băng image
        val raw = BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.bullet_bang)
            .copy(Bitmap.Config.ARGB_8888, true)
        val keyColor = raw.getPixel(0, 0)
        chromaKeyBitmap(raw, keyColor, tolerance = 60).asImageBitmap()
    }
    
    val bgBitmap: ImageBitmap = remember {
        // Decode background downsampled to screen size to avoid huge bitmaps
        val dm = context.resources.displayMetrics
        decodeDownsampledResource(
            context = context,
            resId = com.baothanhbin.game2d.R.drawable.bg_winter,
            reqWidth = dm.widthPixels,
            reqHeight = dm.heightPixels
        )
    }
    
    // Decode multiple GIF enemies
    val basic1Frames: List<ImageBitmap>? = remember {
        try { decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.enermy_basic_1, frameSamples = 12) } catch (_: Throwable) { null }
    }
    val basic2Frames: List<ImageBitmap>? = remember {
        try { decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.enermy_basic_2, frameSamples = 12) } catch (_: Throwable) { null }
    }
    val fireRowFrames: List<ImageBitmap>? = remember {
        try { decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.fire_row, frameSamples = 10) } catch (_: Throwable) { null }
    }
    val waveFrames: List<ImageBitmap>? = remember {
        try { decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.wave, frameSamples = 12) } catch (_: Throwable) { null }
    }
    
    val freezeImage: ImageBitmap = remember {
        // Load freeze.png image
        val raw = BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.freeze)
            .copy(Bitmap.Config.ARGB_8888, true)
        val keyColor = raw.getPixel(0, 0)
        chromaKeyBitmap(raw, keyColor, tolerance = 60).asImageBitmap()
    }
    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Vẽ background image phủ toàn bộ canvas
            drawBackground(bgBitmap)
            
            // Scale nội dung game (tọa độ dựa trên GameState.SCREEN_*) vào kích thước canvas
            val scaleX = size.width / com.baothanhbin.game2d.game.model.GameState.SCREEN_WIDTH
            val scaleY = size.height / com.baothanhbin.game2d.game.model.GameState.SCREEN_HEIGHT
            

            withTransform({ scale(scaleX = scaleX, scaleY = scaleY) }) {
                // Vẽ enemies
                gameState.enemies.forEach { enemy ->
                    if (enemy.enemyType == EnemyType.ROBE) {
                        val frames = when (enemy.sprite) {
                            EnemySprite.BASIC_1 -> basic1Frames
                            EnemySprite.BASIC_2 -> basic2Frames
                            else -> basic1Frames
                        }
                        if (frames != null && frames.isNotEmpty()) {
                            val frame = getGifFrameForTime(frames)
                            drawEnemy(enemy, frame, freezeImage)
                        }
                    }
                }
                
                // Vẽ bullets
                gameState.bullets.forEach { bullet ->
                    val bulletBitmap = when (bullet.heroType) {
                        HeroType.ICE -> bulletBang
                        else -> bulletKim
                    }
                    drawBullet(bullet, bulletBitmap)
                }
                
                // Vẽ effects (bao gồm FIRE_ROW và WAVE)
                gameState.effects.forEach { effect ->
                    when (effect.type) {
                        EffectType.FIRE_ROW -> {
                            if (fireRowFrames != null && fireRowFrames.isNotEmpty()) {
                                val frame = getGifFrameForTime(fireRowFrames)
                                // Use game-space dimensions; the outer transform will scale to canvas
                                val width = com.baothanhbin.game2d.game.model.GameState.SCREEN_WIDTH
                                val height = effect.size
                                val dstX = 0
                                val dstY = (effect.y - height / 2f).toInt()
                                drawImage(
                                    image = frame,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(frame.width, frame.height),
                                    dstOffset = IntOffset(dstX, dstY),
                                    dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
                                    filterQuality = FilterQuality.Medium
                                )
                            }
                        }
                        EffectType.WAVE -> {
                            if (waveFrames != null && waveFrames.isNotEmpty()) {
                                val frame = getGifFrameForTime(waveFrames)
                                // Use game-space dimensions; let the transform handle scaling
                                val width = effect.width
                                val height = effect.size
                                val dstX = effect.x.toInt()
                                val dstY = (effect.y - height / 2f).toInt()
                                drawImage(
                                    image = frame,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(frame.width, frame.height),
                                    dstOffset = IntOffset(dstX, dstY),
                                    dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
                                    filterQuality = FilterQuality.Medium,
                                    alpha = effect.alpha
                                )
                            }
                        }
                        else -> drawEffect(effect)
                    }
                }
            }
        }
    }
}

/**
 * Remove solid background color (chroma key) from a bitmap by turning near-key pixels transparent.
 */
private fun chromaKeyBitmap(src: Bitmap, keyColor: Int, tolerance: Int = 16): Bitmap {
    val width = src.width
    val height = src.height
    val pixels = IntArray(width * height)
    src.getPixels(pixels, 0, width, 0, 0, width, height)
    val kr = (keyColor shr 16) and 0xFF
    val kg = (keyColor shr 8) and 0xFF
    val kb = keyColor and 0xFF
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        val dr = r - kr
        val dg = g - kg
        val db = b - kb
        if (kotlin.math.abs(dr) <= tolerance && kotlin.math.abs(dg) <= tolerance && kotlin.math.abs(db) <= tolerance) {
            pixels[i] = (0x00 shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun decodeDownsampledResource(
    context: android.content.Context,
    resId: Int,
    reqWidth: Int,
    reqHeight: Int
): ImageBitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, options)
    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.ARGB_8888
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    val bmp = BitmapFactory.decodeResource(context.resources, resId, options)
    val safe = (bmp ?: Bitmap.createBitmap(reqWidth.coerceAtLeast(1), reqHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888))
    // Hard cap the largest dimension to avoid Canvas huge bitmap draws
    val maxDim = 2048
    val maxOf = kotlin.math.max(safe.width, safe.height)
    return if (maxOf > maxDim) {
        val scale = maxDim.toFloat() / maxOf.toFloat()
        val targetW = kotlin.math.max(1, (safe.width * scale).toInt())
        val targetH = kotlin.math.max(1, (safe.height * scale).toInt())
        Bitmap.createScaledBitmap(safe, targetW, targetH, true).asImageBitmap()
    } else {
        safe.asImageBitmap()
    }
}

/**
 * Vẽ background với 5 cột rõ ràng
 */
private fun DrawScope.drawBackground(image: ImageBitmap) {
    val dstW = kotlin.math.min(this.size.width.toInt().coerceAtLeast(1), image.width)
    val dstH = kotlin.math.min(this.size.height.toInt().coerceAtLeast(1), image.height)
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(dstW, dstH),
        filterQuality = FilterQuality.Low
    )
}


/**
 * Vẽ enemy sử dụng boar drawable
 */
private fun DrawScope.drawEnemy(enemy: Enemy, boarBitmap: ImageBitmap, freezeImage: ImageBitmap) {
    val width = enemy.size * 2.0f
    val height = enemy.size * 2.0f
    val dstX = (enemy.x - width / 2f).toInt()
    val dstY = (enemy.y - height / 2f).toInt()
    
    // Vẽ enemy sử dụng boar image
    drawImage(
        image = boarBitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(boarBitmap.width, boarBitmap.height),
        dstOffset = IntOffset(dstX, dstY),
        dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.High
    )
    
    // Vẽ HP bar
    if (enemy.hpPercentage < 1f) {
        val barWidth = enemy.size * 1.2f
        val barHeight = 6f
        val barY = enemy.y - enemy.size / 2f - 10f
        
        // Background bar
        drawRect(
            color = Color.Red,
            topLeft = Offset(enemy.x - barWidth / 2f, barY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
        
        // HP bar
        drawRect(
            color = Color.Green,
            topLeft = Offset(enemy.x - barWidth / 2f, barY),
            size = androidx.compose.ui.geometry.Size(barWidth * enemy.hpPercentage, barHeight)
        )
    }
    
    // Vẽ trạng thái đóng băng và làm chậm
    if (enemy.isFrozen) {
        // Hiển thị freeze.png trên enemy - to hơn
        val freezeSize = enemy.size * 1.8f
        val freezeX = (enemy.x - freezeSize / 2f).toInt()
        val freezeY = (enemy.y - freezeSize / 2f).toInt()
        
        drawImage(
            image = freezeImage,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(freezeImage.width, freezeImage.height),
            dstOffset = IntOffset(freezeX, freezeY),
            dstSize = IntSize(freezeSize.toInt().coerceAtLeast(1), freezeSize.toInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.High,
            alpha = 0.8f
        )
    } else if (enemy.isSlowed) {
        // Vòng tròn làm chậm
        drawCircle(
            color = Color(0xFF87CEEB).copy(alpha = 0.5f),
            radius = enemy.size * 0.6f,
            center = Offset(enemy.x, enemy.y),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Placeholder vẽ enemy không dùng boar image
 */
private fun DrawScope.drawEnemyPlaceholder(enemy: Enemy) {
    val width = enemy.size * 2.0f
    val height = enemy.size * 2.0f
    val topLeft = Offset(enemy.x - width / 2f, enemy.y - height / 2f)
    // Thân enemy
    drawRect(
        color = Color(enemy.enemyType.color).copy(alpha = 0.9f),
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(width, height)
    )
    // Viền
    drawRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(width, height)
    )
    // HP bar (giống như drawEnemy)
    if (enemy.hpPercentage < 1f) {
        val barWidth = enemy.size * 1.2f
        val barHeight = 6f
        val barY = enemy.y - enemy.size / 2f - 10f
        drawRect(
            color = Color.Red,
            topLeft = Offset(enemy.x - barWidth / 2f, barY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
        drawRect(
            color = Color.Green,
            topLeft = Offset(enemy.x - barWidth / 2f, barY),
            size = androidx.compose.ui.geometry.Size(barWidth * enemy.hpPercentage, barHeight)
        )
    }
}

/**
 * Vẽ bullet
 */
private fun DrawScope.drawBullet(bullet: Bullet, bulletBitmap: ImageBitmap) {
    // Enlarge bullet visual size - khác nhau cho từng loại đạn
    val scaleMultiplier = when (bullet.heroType) {
        HeroType.ICE -> 10.0f  // Đạn băng to hơn
        HeroType.METAL -> 5.0f   // Đạn kim bình thường
        else -> 5.0f           // Các loại khác bình thường
    }
    
    val width = bullet.size * scaleMultiplier
    val height = when (bullet.heroType) {
        HeroType.ICE -> bullet.size * scaleMultiplier  // Đạn băng vuông
        else -> bullet.size * (scaleMultiplier * 2f)    // Các loại khác hình chữ nhật dài
    }
    val dstX = (bullet.x - width / 2f).toInt()
    val dstY = (bullet.y - height / 2f).toInt()

    // Compute rotation angle based on direction toward target (if any)
    val dx = (bullet.targetX ?: bullet.x) - bullet.x
    val dy = (bullet.targetY ?: (bullet.y - 1f)) - bullet.y
    val angleRad = kotlin.math.atan2(dy, dx)
    // Sprite đạn mặc định đang hướng lên, cần bù +90° để khớp hệ trục atan2 (0° là trục X dương)
    val angleDeg = ((angleRad * 180f / Math.PI).toFloat() + 90f)

    withTransform({
        rotate(degrees = angleDeg, pivot = Offset(bullet.x, bullet.y))
    }) {
        drawImage(
            image = bulletBitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bulletBitmap.width, bulletBitmap.height),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.High
        )
    }
}

/**
 * Decode a GIF resource into sampled frames as ImageBitmap.
 */
private fun decodeGifFrames(
    context: android.content.Context,
    resId: Int,
    frameSamples: Int
): List<ImageBitmap> {
    val input = context.resources.openRawResource(resId)
    val movie = Movie.decodeStream(input)
    input.close()
    if (movie == null) return emptyList()
    val duration = if (movie.duration() > 0) movie.duration() else 1000
    val width = movie.width()
    val height = movie.height()
    val frames = mutableListOf<ImageBitmap>()
    val step = duration / frameSamples
    val bitmapConfig = Bitmap.Config.ARGB_8888
    repeat(frameSamples) { i ->
        val time = (i * step).coerceAtMost(duration - 1)
        val bmp = Bitmap.createBitmap(width, height, bitmapConfig)
        val canvas = android.graphics.Canvas(bmp)
        movie.setTime(time)
        movie.draw(canvas, 0f, 0f)
        frames.add(bmp.asImageBitmap())
    }
    return frames
}

/**
 * Choose a frame based on current time for a simple looped animation.
 */
private fun getGifFrameForTime(frames: List<ImageBitmap>): ImageBitmap {
    val durationMs = 1000L
    val now = System.currentTimeMillis() % durationMs
    val index = ((now.toFloat() / durationMs) * frames.size).toInt().coerceIn(0, frames.size - 1)
    return frames[index]
}

/**
 * Vẽ hiệu ứng
 */
private fun DrawScope.drawEffect(effect: Effect) {
    val color = Color(effect.color).copy(alpha = effect.alpha)
    val radius = (effect.size * effect.scale) / 2f
    
    when (effect.type) {
        EffectType.EXPLOSION -> {
            // Vẽ vòng tròn nổ với gradient
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(effect.x, effect.y)
            )
            // Vòng tròn bên ngoài
            drawCircle(
                color = color.copy(alpha = effect.alpha * 0.5f),
                radius = radius * 1.5f,
                center = Offset(effect.x, effect.y)
            )
        }
        EffectType.MUZZLE_FLASH -> {
            // Vẽ lửa nòng súng hình tam giác
            val points = listOf(
                Offset(effect.x, effect.y - radius),
                Offset(effect.x - radius * 0.5f, effect.y + radius * 0.5f),
                Offset(effect.x + radius * 0.5f, effect.y + radius * 0.5f)
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    close()
                },
                color = color
            )
        }
        EffectType.HIT_SPARK -> {
            // Vẽ tia lửa va chạm
            repeat(6) { i ->
                val angle = (i * 60f + effect.rotation) * (Math.PI / 180f)
                val endX = effect.x + kotlin.math.cos(angle).toFloat() * radius
                val endY = effect.y + kotlin.math.sin(angle).toFloat() * radius
                
                drawLine(
                    color = color,
                    start = Offset(effect.x, effect.y),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        EffectType.TRAIL -> {
            // Vẽ vệt đạn
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(effect.x, effect.y)
            )
        }
        EffectType.SMOKE -> {
            // Vẽ khói
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(effect.x, effect.y)
            )
            // Vòng tròn bên trong
            drawCircle(
                color = color.copy(alpha = effect.alpha * 0.3f),
                radius = radius * 0.6f,
                center = Offset(effect.x, effect.y)
            )
        }
        EffectType.FIRE_ROW -> {
            // FIRE_ROW được render riêng phía trên (dùng GIF). Không vẽ tại đây.
        }
        EffectType.WAVE -> {
            // WAVE được render riêng phía trên (dùng GIF). Không vẽ tại đây.
        }
        EffectType.FREEZE -> {
            // Vẽ hiệu ứng đóng băng - vòng tròn băng với hiệu ứng lạnh
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(effect.x, effect.y),
                style = Stroke(width = 3.dp.toPx())
            )
            // Vòng tròn bên trong
            drawCircle(
                color = color.copy(alpha = effect.alpha * 0.5f),
                radius = radius * 0.7f,
                center = Offset(effect.x, effect.y)
            )
            // Các tinh thể băng nhỏ
            repeat(8) { i ->
                val angle = (i * 45f + effect.rotation) * (Math.PI / 180f)
                val endX = effect.x + kotlin.math.cos(angle).toFloat() * radius * 0.8f
                val endY = effect.y + kotlin.math.sin(angle).toFloat() * radius * 0.8f
                
                drawLine(
                    color = color.copy(alpha = effect.alpha * 0.8f),
                    start = Offset(effect.x, effect.y),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}