package com.example.kinetplayer

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinetplayer.engine.EffectEngine
import com.example.kinetplayer.engine.effects.RainbowEffect
import com.example.kinetplayer.model.PixelMap
import com.example.kinetplayer.model.ShowManifest
import com.example.kinetplayer.network.KinetSender
import com.example.kinetplayer.server.ShowServer
import com.example.kinetplayer.source.VideoFrameSource
import com.example.kinetplayer.util.ShowUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // State
    // var showFiles by mutableStateOf<List<File>>(emptyList()) // Removed
    
    // Unified UI Model
    data class ShowUiModel(
        val id: String,
        val file: File?, // Null if currently uploading
        val name: String,
        val date: Long,
        val thumbnail: android.graphics.Bitmap? = null,
        val progress: Int? = null, // Null if specific file, 0-100 if uploading
        val status: String? = null // "Packing...", "Receiving..."
    )

    var diskShows by mutableStateOf<List<ShowUiModel>>(emptyList())
    var incomingShow by mutableStateOf<ShowUiModel?>(null)
    
    // Playback State
    var currentManifest by mutableStateOf<ShowManifest?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set
    
    // Merged List for UI
    val uiList: List<ShowUiModel>
        get() = (listOfNotNull(incomingShow) + diskShows)
        
    var serverStatus by mutableStateOf("Starting...")

    private val showServer = ShowServer(application,
        onStatus = { status ->
             viewModelScope.launch { 
                 serverStatus = status
                 // Update incoming status
                 incomingShow = incomingShow?.copy(status = status)
             }
        },
        onProgress = { progress ->
            viewModelScope.launch { 
                // Update incoming progress
                incomingShow = incomingShow?.copy(progress = progress)
                
                // If complete, convert to disk show? 
                // Actually, refreshShowList will pick it up when done.
                // We just need to clear incomingShow eventually.
                if (progress >= 100) {
                     // Wait a bit then clear? Or let the file system refresh handle it?
                     // If we clear it immediately, it might flicker before refreshShowList runs.
                     // We'll clear it when we confirm the file exists in refreshShowList or after delay.
                     if (incomingShow?.status?.contains("Complete") == true || progress == 100) {
                         delay(1000)
                         refreshShowList()
                         incomingShow = null
                     }
                }
            }
        },
        onStart = { name ->
            viewModelScope.launch { 
                // Create Card immediately
                incomingShow = ShowUiModel(
                    id = "upload_${System.currentTimeMillis()}",
                    file = null,
                    name = name,
                    date = System.currentTimeMillis(),
                    thumbnail = null, // Will be set by metadata callback
                    progress = 0,
                    status = "Connecting..."
                )
            }
        }
    )
    
    init {
        showServer.setMetadataCallback { name, file ->
             viewModelScope.launch {
                 val thumb = if (file != null) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                 
                 // Update or Create
                 incomingShow = incomingShow?.copy(
                     name = name, 
                     thumbnail = thumb,
                     status = "Receiving Metadata..."
                 ) ?: ShowUiModel(
                     id = "upload_${System.currentTimeMillis()}",
                     file = null,
                     name = name,
                     date = System.currentTimeMillis(),
                     thumbnail = thumb,
                     progress = 0,
                     status = "Prepared"
                 )
             }
        }
        
        try {
            showServer.start()
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch { serverStatus = "Server Error: ${e.message}" }
        }
        
        refreshShowList()
    }

    // Engine
    private var engine: EffectEngine? = null
    private var sender: KinetSender? = null
    var textureView: TextureView? = null // Bind from UI

    fun refreshShowList() {
        viewModelScope.launch {
            val showsDir = File(getApplication<Application>().filesDir, "shows")
            if (!showsDir.exists()) showsDir.mkdirs()
            
            val files = withContext(Dispatchers.IO) {
                showsDir.listFiles { _, name -> name.endsWith(".kshow") }?.toList() ?: emptyList()
            }
            
            diskShows = files.sortedByDescending { it.lastModified() }.map { file ->
                ShowUiModel(
                    id = file.name,
                    file = file,
                    name = file.nameWithoutExtension, // We might need to parse manifest for real name, but filename is fallback
                    date = file.lastModified()
                )
            }
        }
    }

    fun deleteShow(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
                refreshShowList()
            }
        }
    }

    fun renameShow(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val safeName = newName.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "") // Sanitize
            val finalName = if (safeName.endsWith(".kshow")) safeName else "$safeName.kshow"
            
            val newFile = File(file.parent, finalName)
            if (file.exists() && !newFile.exists()) {
                file.renameTo(newFile)
                refreshShowList()
            }
        }
    }

    fun playShow(showFile: File, context: Context) {
        viewModelScope.launch {
            stopEngine()
            
            // 1. Extract
            val cacheDir = File(context.cacheDir, "current_show")
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            
            val manifest = withContext(Dispatchers.IO) {
                ShowUtils.extractAndParse(showFile, cacheDir)
            }
            
            if (manifest == null) {
                Log.e("MainViewModel", "Failed to parse manifest")
                return@launch
            }
            currentManifest = manifest

            // 2. Setup Resources
            // Video File
            val videoPath = File(cacheDir, manifest.mediaFile)
            if (!videoPath.exists()) {
                Log.e("MainViewModel", "Video file missing: ${manifest.mediaFile}")
                // Fallback or error?
            }
            
            // Pixel Map
            val pixelMap = PixelMap.fromManifest(manifest)
            
            // Sender (assume 1st fixture IP effectively or extract global IP from manifest if present?
            // Manifest has individual IP per fixture. KinetSender currently takes ONE global IP.
            // We need to update KinetSender support multi-IP or just use the first valid IP for now.
            // Or better: Update EffectEngine to use multiple Senders? 
            // For now, let's grab the first non-localhost IP from fixtures.
            val targetIp = manifest.fixtures.firstOrNull()?.ip ?: "192.168.1.100"
            sender = KinetSender(targetIp) // Note: This simplifies to unicast to 1 target. 
            // If fixtures have different IPs, we need a smarter Sender.
            
            // Frame Source
            if (textureView != null && videoPath.exists()) {
                val frameSource = VideoFrameSource(context, Uri.fromFile(videoPath), textureView!!, pixelMap)
                
                // 3. Start Engine
                // Use a dummy effect strategy (e.g. Rainbow) if video fails, 
                // but actually we want VideoFrameSource to drive it.
                // Engine takes (Source OR Effect).
                
                engine = EffectEngine(sender!!, pixelMap, frameSource, RainbowEffect())
                engine?.start(viewModelScope)
                isRunning = true
            }
        }
    }

    fun stopEngine() {
        engine?.stop()
        sender?.close()
        engine = null
        sender = null
        isRunning = false
    }


}
