package com.baothanhbin.game2d.media

import android.content.Context
import android.media.MediaPlayer

/**
 * App-wide background music manager.
 */
object MusicManager {
    @Volatile private var mediaPlayer: MediaPlayer? = null
    @Volatile private var enabled: Boolean = true

    fun initialize(context: Context) {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(context.applicationContext, com.baothanhbin.game2d.R.raw.sound_bg).apply {
                    isLooping = true
                    if (enabled) start()
                }
            } catch (_: Exception) { }
        }
    }

    fun setEnabled(context: Context, isEnabled: Boolean) {
        enabled = isEnabled
        val player = mediaPlayer ?: return initialize(context)
        try {
            if (enabled) {
                if (!player.isPlaying) player.start()
            } else if (player.isPlaying) {
                player.pause()
            }
        } catch (_: Exception) { }
    }
}


