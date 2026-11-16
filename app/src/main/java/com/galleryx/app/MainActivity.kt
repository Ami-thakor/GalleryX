package com.galleryx.app

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF1677E8)
private data class MediaItem(val id: Long, val uri: Uri, val name: String, val album: String, val date: Long, val isVideo: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        if (Build.VERSION.SDK_INT >= 23) window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        AppHolder.context = applicationContext
        setContent { GalleryXApp() }
    }
}

@Composable
private fun GalleryXApp() {
    val context = LocalContext.current
    var dark by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var activeAlbum by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf(emptyList<MediaItem>()) }
    var permissionGranted by remember { mutableStateOf(hasMediaPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionGranted = result.values.any { it } || hasMediaPermission()
        if (permissionGranted) items = loadMedia()
    }
    fun askPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionLauncher.launch(permissions)
    }
    LaunchedEffect(Unit) { if (permissionGranted) items = loadMedia() }
    val bg = if (dark) Color(0xFF101114) else Color.White
    val fg = if (dark) Color.White else Color(0xFF17181A)
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Blue, background = bg, surface = Color(0xFF1B1D21)) else lightColorScheme(primary = Blue, background = bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            if (selected != null) {
                Viewer(item = selected!!, all = items, dark = dark, onBack = { selected = null }, onOpen = { selected = it })
            } else {
                Column(Modifier.fillMaxSize().background(bg)) {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("GalleryX", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = fg)
                            Text(activeAlbum ?: if (tab == 0) "Pictures" else if (tab == 1) "Albums" else "Stories", fontSize = 13.sp, color = if (dark) Color.LightGray else Color.Gray)
                        }
                        IconButton(onClick = { dark = !dark }) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Theme", tint = fg) }
                        IconButton(onClick = { if (permissionGranted) items = loadMedia() else askPermission() }) { Icon(Icons.Default.Refresh, "Refresh", tint = fg) }
                    }
                    if (!permissionGranted) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = Blue, modifier = Modifier.size(58.dp))
                            Spacer(Modifier.height(14.dp)); Text("Allow access to your photos", color = fg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("GalleryX reads media stored on this device.", color = Color.Gray, fontSize = 13.sp)
                            Spacer(Modifier.height(18.dp)); Button(onClick = { askPermission() }) { Text("Allow access") }
                        }
                    } else {
                        when (tab) {
                            0 -> MediaGrid(items.filter { !it.isVideo && (activeAlbum == null || it.album == activeAlbum) }, dark) { selected = it }
                            1 -> AlbumsView(items, dark) { album -> activeAlbum = album; tab = 0 }
                            else -> StoriesView(items, dark) { selected = it }
                        }
                    }
                    Spacer(Modifier.weight(1f, fill = false))
                    NavigationBar(containerColor = if (dark) Color(0xFF1B1D21) else Color(0xFFF8F9FB), tonalElevation = 0.dp) {
                        NavigationBarItem(selected = tab == 0, onClick = { activeAlbum = null; tab = 0 }, icon = { Icon(Icons.Default.Photo, null) }, label = { Text("Pictures") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Albums") })
                        NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.AutoAwesomeMosaic, null) }, label = { Text("Stories") })
                    }
                }
            }
        }
    }
}

private fun hasMediaPermission(): Boolean {
    val context = AppHolder.context ?: return false
    return if (Build.VERSION.SDK_INT >= 33) ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

// A tiny holder keeps permission checks independent of UI recomposition.
private object AppHolder { var context: android.content.Context? = null }

private fun loadMedia(): List<MediaItem> {
    val context = AppHolder.context ?: return emptyList()
    val result = mutableListOf<MediaItem>()
    val collections = listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI to false, MediaStore.Video.Media.EXTERNAL_CONTENT_URI to true)
    for ((collection, video) in collections) {
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED, "bucket_display_name")
        try {
            context.contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val albumCol = cursor.getColumnIndex("bucket_display_name")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    result.add(MediaItem(id, ContentUris.withAppendedId(collection, id), cursor.getString(nameCol) ?: "Media", if (albumCol >= 0) cursor.getString(albumCol) ?: "Other" else "Other", cursor.getLong(dateCol) * 1000, video))
                }
            }
        } catch (_: Exception) { }
    }
    return result.sortedByDescending { it.date }
}

@Composable
private fun MediaGrid(items: List<MediaItem>, dark: Boolean, onClick: (MediaItem) -> Unit) {
    if (items.isEmpty()) { EmptyState("No photos found") ; return }
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(items, key = { "${it.isVideo}-${it.id}" }) { item ->
            Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(2.dp)).clickable { onClick(item) }) {
                AsyncImage(item.uri, contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (item.isVideo) Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
        }
    }
}

@Composable
private fun AlbumsView(items: List<MediaItem>, dark: Boolean, onAlbum: (String) -> Unit) {
    val albums = items.groupBy { it.album }.toList().sortedByDescending { it.second.size }
    if (albums.isEmpty()) { EmptyState("No albums found"); return }
    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(albums.size) { index ->
            val (name, media) = albums[index]
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onAlbum(name) }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(media.first().uri, null, Modifier.size(92.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(name, color = if (dark) Color.White else Color(0xFF17181A), fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text("${media.size} items", color = Color.Gray, fontSize = 13.sp) }
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun StoriesView(items: List<MediaItem>, dark: Boolean, onClick: (MediaItem) -> Unit) {
    val recent = items.filter { !it.isVideo }.take(30)
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Recent memories", fontWeight = FontWeight.SemiBold, color = if (dark) Color.White else Color.Black, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp)); Text("Your latest photos, ready to revisit.", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp)); MediaGrid(recent, dark, onClick)
    }
}

@Composable
private fun Viewer(item: MediaItem, all: List<MediaItem>, dark: Boolean, onBack: () -> Unit, onOpen: (MediaItem) -> Unit) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    val fg = if (dark) Color.White else Color.Black
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Column(Modifier.weight(1f)) { Text(item.name, color = Color.White, maxLines = 1, fontSize = 14.sp); Text(SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.date)), color = Color.LightGray, fontSize = 11.sp) }
            IconButton(onClick = { val share = Intent(Intent.ACTION_SEND).apply { type = if (item.isVideo) "video/*" else "image/*"; putExtra(Intent.EXTRA_STREAM, item.uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(share, "Share")) }) { Icon(Icons.Default.Share, null, tint = Color.White) }
            IconButton(onClick = { val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(i) }) { Icon(Icons.Default.OpenInNew, null, tint = Color.White) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AsyncImage(item.uri, item.name, Modifier.fillMaxSize().pointerInput(item.id) { detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f) } }, contentScale = if (scale > 1f) ContentScale.Fit else ContentScale.Fit)
            if (item.isVideo) Icon(Icons.Default.PlayCircle, "Play video", tint = Color.White, modifier = Modifier.size(64.dp).clickable { val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(item.uri, "video/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(i) })
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { scale = 1f }) { Icon(Icons.Default.ZoomOutMap, "Reset zoom", tint = Color.White) }
            IconButton(onClick = { val i = Intent(Intent.ACTION_EDIT).apply { setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; runCatching { context.startActivity(i) } }) { Icon(Icons.Default.Edit, "Edit", tint = Color.White) }
            IconButton(onClick = { onBack() }) { Icon(Icons.Default.Info, "Details", tint = Color.White) }
        }
    }
}

@Composable private fun EmptyState(text: String) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PhotoLibrary, null, tint = Color.Gray, modifier = Modifier.size(46.dp)); Spacer(Modifier.height(10.dp)); Text(text, color = Color.Gray) } } }
