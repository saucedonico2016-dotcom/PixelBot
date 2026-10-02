package com.pixelbot.body.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixelbot.body.model.Skin
import com.pixelbot.body.model.SkinMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.core.readBytes
import kotlinx.io.core.use
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile

class SkinLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadFromAssets(skinId: String): SkinLoadResult {
        return withContext(Dispatchers.IO) {
            try {
                val skinJson = context.assets.open("skins/$skinId/skin.json").readBytes().decodeToString()
                val skin = json.decodeFromString<Skin>(skinJson)
                val bitmap = BitmapFactory.decodeStream(context.assets.open("skins/$skinId/sprites.png"))
                SkinLoadResult.Success(skin, bitmap)
            } catch (e: Exception) {
                SkinLoadResult.Error("Error cargando skin $skinId: ${e.message}")
            }
        }
    }

    suspend fun loadFromZip(zipUri: Uri): SkinLoadResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(zipUri) ?: 
                    return@withContext SkinLoadResult.Error("No se pudo abrir el archivo")
                
                val tempFile = File(context.cacheDir, "skin_import_${System.currentTimeMillis()}.zip")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                ZipFile(tempFile).use { zipFile ->
                    val skinJsonEntry = zipFile.getEntry("skin.json") ?: 
                        return@withContext SkinLoadResult.Error("skin.json no encontrado en el zip")
                    val spritesEntry = zipFile.getEntry("sprites.png") ?: 
                        return@withContext SkinLoadResult.Error("sprites.png no encontrado en el zip")
                    
                    zipFile.getInputStream(skinJsonEntry).use { stream ->
                        val skinJson = stream.readBytes().decodeToString()
                        val skin = json.decodeFromString<Skin>(skinJson)
                        
                        zipFile.getInputStream(spritesEntry).use { imgStream ->
                            val bitmap = BitmapFactory.decodeStream(imgStream)
                            SkinLoadResult.Success(skin, bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                SkinLoadResult.Error("Error importando skin: ${e.message}")
            }
        }
    }

    suspend fun loadFromDirectory(dir: File): SkinLoadResult {
        return withContext(Dispatchers.IO) {
            try {
                val skinJsonFile = File(dir, "skin.json")
                val spritesFile = File(dir, "sprites.png")
                
                if (!skinJsonFile.exists()) return@withContext SkinLoadResult.Error("skin.json no encontrado")
                if (!spritesFile.exists()) return@withContext SkinLoadResult.Error("sprites.png no encontrado")
                
                val skinJson = skinJsonFile.readText()
                val skin = json.decodeFromString<Skin>(skinJson)
                val bitmap = BitmapFactory.decodeFile(spritesFile.absolutePath)
                
                SkinLoadResult.Success(skin, bitmap)
            } catch (e: Exception) {
                SkinLoadResult.Error("Error cargando skin: ${e.message}")
            }
        }
    }

    fun getBuiltInSkins(): List<SkinMetadata> {
        return listOf(
            SkinMetadata("pixel_classic", "Pixel Clásico", "PixelBot"),
            SkinMetadata("pixel_neon", "Pixel Neón", "PixelBot")
        )
    }
}

sealed class SkinLoadResult {
    data class Success(val skin: Skin, val bitmap: Bitmap) : SkinLoadResult()
    data class Error(val message: String) : SkinLoadResult()
}