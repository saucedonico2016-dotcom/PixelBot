package com.pixelbot.body.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelbot.body.generator.SkinGenerator
import com.pixelbot.body.loader.SkinLoader
import com.pixelbot.body.model.Skin
import com.pixelbot.body.model.SkinMetadata
import kotlinx.coroutines.launch

@Composable
fun SkinSelectionScreen(
    onSkinSelected: (Skin, android.graphics.Bitmap) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    val skinLoader = remember { SkinLoader(context) }
    val builtInSkins = remember { skinLoader.getBuiltInSkins() }
    
    var selectedSkinId by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    
    val pickZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importing = true
                androidx.lifecycle.lifecycleScope.launch {
                    val result = skinLoader.loadFromZip(uri)
                    importing = false
                    when (result) {
                        is SkinLoader.SkinLoadResult.Success -> onSkinSelected(result.skin, result.bitmap)
                        is SkinLoader.SkinLoadResult.Error -> {
                            // TODO: Show error
                        }
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elegir Skin") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Skins integrados", style = MaterialTheme.typography.titleMedium)
            
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(builtInSkins) { meta ->
                    BuiltInSkinCard(
                        metadata = meta,
                        isSelected = selectedSkinId == meta.id,
                        onClick = {
                            selectedSkinId = meta.id
                            loadBuiltInSkin(meta.id, skinLoader, onSkinSelected)
                        }
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(16.dp))
            
            Text("Importar skin personalizado", style = MaterialTheme.typography.titleMedium)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "application/zip"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        pickZipLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = !importing
                ) {
                    if (importing) {
                        Text("Importando...")
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(painterResource(id = android.R.drawable.ic_menu_upload), contentDescription = null)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(8.dp))
                            Text("Seleccionar archivo .zip (skin.json + sprites.png)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuiltInSkinCard(
    metadata: SkinMetadata,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview placeholder - en producción sería el frame idle
            androidx.compose.foundation.Box(
                modifier = Modifier.size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Aquí iría el PixelCharacter con frame idle
            }
            
            Text(metadata.name, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(metadata.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            if (isSelected) {
                Icon(
                    painter = painterResource(id = android.R.drawable.checkbox_on_background),
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun loadBuiltInSkin(
    skinId: String,
    loader: SkinLoader,
    onSkinSelected: (Skin, android.graphics.Bitmap) -> Unit
) {
    androidx.lifecycle.lifecycleScope.launch {
        val result = loader.loadFromAssets(skinId)
        when (result) {
            is SkinLoader.SkinLoadResult.Success -> onSkinSelected(result.skin, result.bitmap)
            is SkinLoader.SkinLoadResult.Error -> {
                // Fallback: generar skin por código
                when (skinId) {
                    "pixel_classic" -> {
                        val generated = SkinGenerator.generateClassicSkin()
                        onSkinSelected(generated.skin, generated.bitmap)
                    }
                    "pixel_neon" -> {
                        val generated = SkinGenerator.generateNeonSkin()
                        onSkinSelected(generated.skin, generated.bitmap)
                    }
                }
            }
        }
    }
}