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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Khu vực chơi game - hiển thị enemies, bullets, effects
 */
@Composable
fun PlayArea(
    gameState: GameState,
    season: Season,
    onForceCombat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val bulletKim: ImageBitmap = remember {
        // Load and chroma-key the bullet image to remove connected gray-ish background
        val raw = BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.bullet_metal)
            .copy(Bitmap.Config.ARGB_8888, true)
        val keyColor = raw.getPixel(0, 0)
        chromaKeyBitmap(raw, keyColor, tolerance = 60).asImageBitmap()
    }
    
    val bulletBang: ImageBitmap = remember {
        // Load and chroma-key the bullet băng image
        val raw = BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.bullet_ice)
            .copy(Bitmap.Config.ARGB_8888, true)
        val keyColor = raw.getPixel(0, 0)
        chromaKeyBitmap(raw, keyColor, tolerance = 60).asImageBitmap()
    }
    
    // Animated background using Movie API (giống SplashScreen)
    var bgMovie by remember(season) { mutableStateOf<Movie?>(null) }
    var bgCurrentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var bgStartTime by remember { mutableStateOf(0L) }
    
    // Load background GIF
    LaunchedEffect(season) {
        withContext(Dispatchers.IO) {
            val resId = when (season) {
                Season.SPRING -> com.baothanhbin.game2d.R.raw.bg_spring
                Season.SUMMER -> com.baothanhbin.game2d.R.raw.bg_summer
                Season.AUTUMN -> com.baothanhbin.game2d.R.raw.bg_autumn
                Season.WINTER -> com.baothanhbin.game2d.R.raw.bg_winter
            }
            try {
                val input = context.resources.openRawResource(resId)
                bgMovie = Movie.decodeStream(input)
                input.close()
                bgStartTime = System.currentTimeMillis()
            } catch (e: Exception) {
                bgMovie = null
            }
        }
    }
    
    // Animate background frames at native GIF speed
    LaunchedEffect(bgMovie) {
        val movie = bgMovie
        if (movie != null) {
            while (true) {
                val currentTime = System.currentTimeMillis() - bgStartTime
                val duration = movie.duration()
                val time = if (duration > 0) (currentTime % duration).toInt() else 0
                
                withContext(Dispatchers.IO) {
                    val bitmap = Bitmap.createBitmap(
                        movie.width(),
                        movie.height(),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    movie.setTime(time)
                    movie.draw(canvas, 0f, 0f)
                    bgCurrentFrame = bitmap.asImageBitmap()
                }
                
                kotlinx.coroutines.delay(16) // ~60 FPS
            }
        }
    }
    
    // Decode multiple GIF enemies on background thread
    var monter_basic by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var monter_fast by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var monter_tank by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var boss1_frames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var boss2_frames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var boss3_frames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var murid_frames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var fireRowFrames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var waveFrames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    
    // Load GIFs on background thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                monter_basic = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.monter_basic, frameSamples = 12)
            } catch (_: Throwable) { 
                monter_basic = null 
            }
            
            try {
                monter_fast = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.monter_paper_hp_fast_walk, frameSamples = 12)
            } catch (_: Throwable) { 
                monter_fast = null 
            }
            
            try {
                monter_tank = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.monter_big_hp_slow_walk, frameSamples = 12)
            } catch (_: Throwable) { 
                monter_tank = null 
            }
            
            try {
                boss1_frames = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.boss1, frameSamples = 12)
                println("BOSS DEBUG: ✅ Boss1 frames loaded successfully, count: ${boss1_frames?.size}")
            } catch (e: Throwable) { 
                boss1_frames = null 
                println("BOSS DEBUG: ❌ Failed to load boss1 frames: ${e.message}")
            }
            
            try {
                boss2_frames = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.boss2, frameSamples = 12)
                println("BOSS DEBUG: ✅ Boss2 frames loaded successfully, count: ${boss2_frames?.size}")
            } catch (e: Throwable) { 
                boss2_frames = null 
                println("BOSS DEBUG: ❌ Failed to load boss2 frames: ${e.message}")
            }
            
            try {
                boss3_frames = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.boss3, frameSamples = 12)
                println("BOSS DEBUG: ✅ Boss3 frames loaded successfully, count: ${boss3_frames?.size}")
            } catch (e: Throwable) { 
                boss3_frames = null 
                println("BOSS DEBUG: ❌ Failed to load boss3 frames: ${e.message}")
            }
            
            try {
                murid_frames = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.murid, frameSamples = 12)
                println("MURID DEBUG: ✅ Murid frames loaded successfully, count: ${murid_frames?.size}")
            } catch (e: Throwable) { 
                murid_frames = null 
                println("MURID DEBUG: ❌ Failed to load murid frames: ${e.message}")
            }
            
            try {
                fireRowFrames = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.fire_row, frameSamples = 10)
            } catch (_: Throwable) { 
                fireRowFrames = null 
            }
            
            try {
                waveFrames = decodeGifFrames(context = context, resId = com.baothanhbin.game2d.R.raw.wave, frameSamples = 12)
            } catch (_: Throwable) { 
                waveFrames = null 
            }
        }
    }
    
    // Load freeze image on background thread
    var freezeImage by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val raw = BitmapFactory.decodeResource(context.resources, com.baothanhbin.game2d.R.drawable.freeze)
                    .copy(Bitmap.Config.ARGB_8888, true)
                val keyColor = raw.getPixel(0, 0)
                freezeImage = chromaKeyBitmap(raw, keyColor, tolerance = 60).asImageBitmap()
            } catch (_: Throwable) {
                freezeImage = null
            }
        }
    }
    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Vẽ background GIF frame với tốc độ gốc
            bgCurrentFrame?.let { frame ->
                drawBackground(frame)
            }
            
            // Scale nội dung game (tọa độ dựa trên GameState.SCREEN_*) vào kích thước canvas
            val scaleX = size.width / com.baothanhbin.game2d.game.model.GameState.SCREEN_WIDTH
            val scaleY = size.height / com.baothanhbin.game2d.game.model.GameState.SCREEN_HEIGHT
            

            withTransform({ scale(scaleX = scaleX, scaleY = scaleY) }) {
                // Vẽ enemies dựa theo enemyType và archetype
                gameState.enemies.forEach { enemy ->
                    val frames = when {
                        enemy.enemyType == EnemyType.BOSS1 -> {
                            println("BOSS DEBUG: 🐉 Drawing BOSS1 enemy! Boss1 frames available: ${boss1_frames != null}, count: ${boss1_frames?.size}")
                            boss1_frames
                        }
                        enemy.enemyType == EnemyType.BOSS2 -> {
                            println("BOSS DEBUG: 🐉 Drawing BOSS2 enemy! Boss2 frames available: ${boss2_frames != null}, count: ${boss2_frames?.size}")
                            boss2_frames
                        }
                        enemy.enemyType == EnemyType.BOSS3 -> {
                            println("BOSS DEBUG: 🐉 Drawing BOSS3 enemy! Boss3 frames available: ${boss3_frames != null}, count: ${boss3_frames?.size}")
                            boss3_frames
                        }
                        enemy.enemyType == EnemyType.MURID -> {
                            println("MURID DEBUG: 🏃 Drawing MURID enemy! Murid frames available: ${murid_frames != null}, count: ${murid_frames?.size}")
                            murid_frames
                        }
                        enemy.archetype == EnemyArchetype.BASIC -> monter_basic
                        enemy.archetype == EnemyArchetype.FAST -> monter_fast
                        enemy.archetype == EnemyArchetype.TANK -> monter_tank
                        else -> monter_basic
                    }
                    if (frames != null && frames.isNotEmpty() && freezeImage != null) {
                        val frame = getGifFrameForTime(frames)
                        drawEnemy(enemy, frame, freezeImage!!)
                    } else {
                        println("BOSS DEBUG: ❌ Using placeholder for enemy type: ${enemy.enemyType}, frames: ${frames?.size}")
                        drawEnemyPlaceholder(enemy)
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
                val currentFireRowFrames = fireRowFrames
                val currentWaveFrames = waveFrames
                
                gameState.effects.forEach { effect ->
                    when (effect.type) {
                        EffectType.FIRE_ROW -> {
                            if (currentFireRowFrames != null && currentFireRowFrames.isNotEmpty()) {
                                val frame = getGifFrameForTime(currentFireRowFrames)
                                // su dung full width of game area
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
                            if (currentWaveFrames != null && currentWaveFrames.isNotEmpty()) {
                        // Làm chậm hoạt ảnh wave để dịu mắt
                        val frame = getGifFrameForTime(currentWaveFrames, durationMs = 6000L)
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
    val dstW = this.size.width.toInt().coerceAtLeast(1)
    val dstH = this.size.height.toInt().coerceAtLeast(1)
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
    val barWidth = enemy.size * 1.2f
    val barHeight = 6f
    val barY = enemy.y - enemy.size / 2f - 10f
    
    // Background bar (luôn vẽ)
    drawRect(
        color = Color.Red,
        topLeft = Offset(enemy.x - barWidth / 2f, barY),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
    )
    
    // HP bar (luôn vẽ)
    drawRect(
        color = Color.Green,
        topLeft = Offset(enemy.x - barWidth / 2f, barY),
        size = androidx.compose.ui.geometry.Size(barWidth * enemy.hpPercentage, barHeight)
    )
    
    // Vẽ text HP trên thanh máu
    drawIntoCanvas { canvas ->
        val hpText = "${enemy.currentHp.toInt()}/${enemy.maxHp.toInt()}"
        val textY = enemy.y - enemy.size / 2f - 15f
        
        // Tạo paint cho text
        val paint = android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(3f, 2f, 2f, Color.Black.toArgb())
        }
        
        // Vẽ text HP
        canvas.nativeCanvas.drawText(hpText, enemy.x, textY, paint)
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
    val barWidth = enemy.size * 1.2f
    val barHeight = 6f
    val barY = enemy.y - enemy.size / 2f - 10f
    
    // Background bar (luôn vẽ)
    drawRect(
        color = Color.Red,
        topLeft = Offset(enemy.x - barWidth / 2f, barY),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
    )
    
    // HP bar (luôn vẽ)
    drawRect(
        color = Color.Green,
        topLeft = Offset(enemy.x - barWidth / 2f, barY),
        size = androidx.compose.ui.geometry.Size(barWidth * enemy.hpPercentage, barHeight)
    )
    
    // Vẽ text HP trên thanh máu
    drawIntoCanvas { canvas ->
        val hpText = "${enemy.currentHp.toInt()}/${enemy.maxHp.toInt()}"
        val textY = enemy.y - enemy.size / 2f - 15f
        
        // Tạo paint cho text
        val paint = android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textSize = 16f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(3f, 2f, 2f, Color.Black.toArgb())
        }
        
        // Vẽ text HP
        canvas.nativeCanvas.drawText(hpText, enemy.x, textY, paint)
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
private fun getGifFrameForTime(frames: List<ImageBitmap>, durationMs: Long = 1000L): ImageBitmap {
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
