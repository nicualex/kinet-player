package com.example.kinetplayer

import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.view.TextureView
import android.view.ViewGroup
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.material.icons.filled.Edit
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kinetplayer.ui.theme.KinetPixelMapperTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.border
import java.util.Locale

import com.example.kinetplayer.network.DiscoveryService

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    val discoveryService = DiscoveryService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load name
        val prefs = getSharedPreferences("kinet_prefs", Context.MODE_PRIVATE)
        discoveryService.deviceName = prefs.getString("player_name", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") ?: "Unknown"
        discoveryService.width = prefs.getInt("player_width", 100)
        discoveryService.height = prefs.getInt("player_height", 100)

        discoveryService.start(this)
        
        setContent {
            KinetPixelMapperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    fun updateServiceConfig(newName: String, newWidth: Int, newHeight: Int) {
        discoveryService.deviceName = newName
        discoveryService.width = newWidth
        discoveryService.height = newHeight
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryService.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var ipAddress by remember { mutableStateOf("Checking...") }
    
    // Name State
    val prefs = context.getSharedPreferences("kinet_prefs", Context.MODE_PRIVATE)
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") ?: "Unknown") }
    var playerWidth by remember { mutableStateOf(prefs.getInt("player_width", 100).toString()) }
    var playerHeight by remember { mutableStateOf(prefs.getInt("player_height", 100).toString()) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Update Discovery Service with initial name
    LaunchedEffect(playerName) {
        // We need a way to access the service instance. Since it's in MainActivity, 
        // passing it down or exposing a callback is needed.
        // For simplicity, let's assume MainActivity handles the update if we expose a function or 
        // better yet, just do it in the activity.
        // But here we are in Composable.
        // Let's pass a callback to MainScreen `onNameChange`.
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } else {
            ipAddress = "Permission Denied"
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    // Name Edit Dialog
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(playerName) }
        var tempWidth by remember { mutableStateOf(playerWidth) }
        var tempHeight by remember { mutableStateOf(playerHeight) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Player Settings") },
            text = { 
                Column {
                    OutlinedTextField(
                        value = tempName, 
                        onValueChange = { tempName = it },
                        label = { Text("Name") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = tempWidth,
                            onValueChange = { tempWidth = it },
                            label = { Text("Width") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = tempHeight,
                            onValueChange = { tempHeight = it },
                            label = { Text("Height") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    playerName = tempName
                    playerWidth = tempWidth
                    playerHeight = tempHeight
                    
                    val w = tempWidth.toIntOrNull() ?: 100
                    val h = tempHeight.toIntOrNull() ?: 100
                    
                    prefs.edit()
                        .putString("player_name", tempName)
                        .putInt("player_width", w)
                        .putInt("player_height", h)
                        .apply()
                        
                    showNameDialog = false
                    // Trigger update in service
                    (context as? MainActivity)?.updateServiceConfig(tempName, w, h)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kinet Pixel Player", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { showNameDialog = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit Name")
                    }
                }
                Text("Name: $playerName", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp))
                Text("IP: $ipAddress:8080", style = MaterialTheme.typography.bodyMedium)
                
                // Debug Log
                var debugLog by remember { mutableStateOf("No logs yet") }
                LaunchedEffect(Unit) {
                    (context as? MainActivity)?.discoveryService?.onLog = { log ->
                        debugLog = log
                    }
                }
                Text("Debug: $debugLog", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        
        // List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.uiList) { item ->
                UnifiedShowItem(
                    item = item,
                    onClick = { 
                        if (item.file != null) viewModel.playShow(item.file, context) 
                    },
                    onDelete = { 
                        if (item.file != null) viewModel.deleteShow(item.file)
                    },
                    onRename = { newName -> 
                        if (item.file != null) viewModel.renameShow(item.file, newName) 
                    }
                )
            }
        }
        
        // Active Playback Area
        if (viewModel.isRunning) {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Now Playing: ${viewModel.currentManifest?.name ?: "Unknown"}", style = MaterialTheme.typography.titleMedium)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                 AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            this.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            viewModel.textureView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Button(
                onClick = { viewModel.stopEngine() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("STOP PLAYBACK")
            }
        }
    }
}

@Composable
fun UnifiedShowItem(
    item: MainViewModel.ShowUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val date = Date(item.date)
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(item.name) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Show") },
            text = { 
                OutlinedTextField(
                    value = renameText, 
                    onValueChange = { renameText = it },
                    label = { Text("New Name") }
                ) 
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(renameText)
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = item.file != null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.progress != null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail / Icon
                Box(
                    modifier = Modifier
                        .size(60.dp, 40.dp)
                        .background(Color.Black)
                        .border(1.dp, Color.Gray)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                     val thumb = item.thumbnail
                     if (thumb != null) {
                         androidx.compose.foundation.Image(
                             bitmap = thumb.asImageBitmap(),
                             contentDescription = null,
                             modifier = Modifier.fillMaxSize(),
                             contentScale = androidx.compose.ui.layout.ContentScale.Crop
                         )
                     } else {
                         Icon(
                             if (item.file != null) Icons.Default.PlayArrow else Icons.Default.Refresh, // Use generic icon
                             contentDescription = null, 
                             tint = Color.White
                         )
                     }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(format.format(date), style = MaterialTheme.typography.bodySmall)
                }
                
                // Actions (Only if file exists)
                if (item.file != null) {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.Gray)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            // Progress Bar (Only if uploading)
            if (item.progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = item.progress / 100f,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color.Blue,
                    trackColor = Color.LightGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.progress}%", style = MaterialTheme.typography.labelSmall)
                    Text(item.status ?: "Processing...", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
