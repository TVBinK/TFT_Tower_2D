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
    
    val bgBitmap: ImageBitmap = remember { BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.background).asImageBitmap() }
    
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
    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Debug: Log canvas drawing
            android.util.Log.d("PlayArea", "🎨 CANVAS DRAW: enemies=${gameState.enemies.size}, bullets=${gameState.bullets.size}, effects=${gameState.effects.size}")
            
            // Vẽ background image phủ toàn bộ canvas
            drawBackground(bgBitmap)
            
            // Scale nội dung game (tọa độ dựa trên GameState.SCREEN_*) vào kích thước canvas
            val scaleX = size.width / com.baothanhbin.game2d.game.model.GameState.SCREEN_WIDTH
            val scaleY = size.height / com.baothanhbin.game2d.game.model.GameState.SCREEN_HEIGHT
            
            android.util.Log.d("PlayArea", "🎨 SCALE: scaleX=$scaleX, scaleY=$scaleY, canvasSize=${size}")
            
            withTransform({ scale(scaleX = scaleX, scaleY = scaleY) }) {
                // Vẽ enemies
                gameState.enemies.forEach { enemy ->
                    android.util.Log.d("PlayArea", "👹 DRAW ENEMY: ${enemy.id} at (${enemy.x}, ${enemy.y})")
                    if (enemy.enemyType == EnemyType.ROBE) {
                        val frames = when (enemy.sprite) {
                            EnemySprite.BASIC_1 -> basic1Frames
                            EnemySprite.BASIC_2 -> basic2Frames
                            else -> basic1Frames
                        }
                        if (frames != null && frames.isNotEmpty()) {
                            val frame = getGifFrameForTime(frames)
                            drawEnemy(enemy, frame)
                        }
                    }
                }
                
                // Vẽ bullets
                gameState.bullets.forEach { bullet ->
                    android.util.Log.d("PlayArea", "🔫 DRAW BULLET: ${bullet.id} at (${bullet.x}, ${bullet.y})")
                    drawBullet(bullet, bulletKim)
                }
                
                // Vẽ effects (bao gồm FIRE_ROW)
                gameState.effects.forEach { effect ->
                    android.util.Log.d("PlayArea", "✨ DRAW EFFECT: ${effect.type} at (${effect.x}, ${effect.y})")
                    when (effect.type) {
                        EffectType.FIRE_ROW -> {
                            if (fireRowFrames != null && fireRowFrames.isNotEmpty()) {
                                val frame = getGifFrameForTime(fireRowFrames)
                                val width = size.width
                                val height = effect.size
                                val dstX = 0
                                val dstY = (effect.y - height / 2f).toInt()
                                drawImage(
                                    image = frame,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(frame.width, frame.height),
                                    dstOffset = IntOffset(dstX, dstY),
                                    dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
                                    filterQuality = FilterQuality.High
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

/**
 * Vẽ background với 5 cột rõ ràng
 */
private fun DrawScope.drawBackground(image: ImageBitmap) {
    drawImage(
        image = image,
        srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
        dstSize = IntSize(this.size.width.toInt().coerceAtLeast(1), this.size.height.toInt().coerceAtLeast(1))
    )
}

// Removed boar bitmap swap logic

/**
 * Vẽ enemy sử dụng boar drawable
 */
private fun DrawScope.drawEnemy(enemy: Enemy, boarBitmap: ImageBitmap) {
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
    // Enlarge bullet visual size
    val width = bullet.size * 5.0f
    val height = bullet.size * 10f
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
    }
}