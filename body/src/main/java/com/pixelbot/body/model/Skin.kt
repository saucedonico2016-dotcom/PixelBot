package com.pixelbot.body.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class Skin(
    val id: String,
    val name: String,
    val author: String = "PixelBot",
    val version: Int = 1,
    val frameSize: Int = 32,
    val palette: List<String> = listOf("#000000", "#FFFFFF", "#6C5CE7", "#00CEC9"),
    val rows: Map<String, RowData> = emptyMap(),
    val fps: Map<String, Int> = mapOf(
        "dormido" to 4,
        "escuchando" to 12,
        "pensando" to 8,
        "hablando" to 10,
        "feliz" to 8,
        "error" to 6,
        "confundido" to 8
    )
) {
    data class RowData(
        val frames: Int,
        val rowIndex: Int,
        val loop: Boolean = true
    )

    val states: List<AnimationState> get() = listOf(
        AnimationState("dormido", rows["dormido"]?.frames ?: 2, rows["dormido"]?.rowIndex ?: 6, rows["dormido"]?.loop ?: true, fps["dormido"] ?: 4),
        AnimationState("escuchando", rows["escuchando"]?.frames ?: 6, rows["escuchando"]?.rowIndex ?: 1, rows["escuchando"]?.loop ?: true, fps["escuchando"] ?: 12),
        AnimationState("pensando", rows["pensando"]?.frames ?: 6, rows["pensando"]?.rowIndex ?: 2, rows["pensando"]?.loop ?: true, fps["pensando"] ?: 8),
        AnimationState("hablando", rows["hablando"]?.frames ?: 4, rows["hablando"]?.rowIndex ?: 3, rows["hablando"]?.loop ?: true, fps["hablando"] ?: 10),
        AnimationState("feliz", rows["feliz"]?.frames ?: 4, rows["feliz"]?.rowIndex ?: 4, rows["feliz"]?.loop ?: true, fps["feliz"] ?: 8),
        AnimationState("error", rows["error"]?.frames ?: 4, rows["error"]?.rowIndex ?: 5, rows["error"]?.loop ?: false, fps["error"] ?: 6),
        AnimationState("confundido", rows["confundido"]?.frames ?: 4, rows["confundido"]?.rowIndex ?: 7, rows["confundido"]?.loop ?: true, fps["confundido"] ?: 8)
    )

    fun getState(name: String): AnimationState? = states.find { it.name == name }
}

@Serializable
data class AnimationState(
    val name: String,
    val frames: Int,
    val rowIndex: Int,
    val loop: Boolean,
    val fps: Int
) {
    val frameDurationMs: Long = (1000.0 / fps).toLong()
}

enum class BodyState {
    DORMIDO, ESCUCHANDO, PENSANDO, EJECUTANDO, HABLANDO, FELIZ, ERROR, CONFUNDIDO
}

@Serializable
data class SkinMetadata(
    val id: String,
    val name: String,
    val author: String,
    val previewBase64: String? = null
)