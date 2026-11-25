package com.example.camui

import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-app gallery for pictures produced by FujiFilm Mode.
 *
 * Only app-owned Pictures/FujifilmMode media is queried, so Android 13+'s
 * broad READ_MEDIA_IMAGES permission is not needed for this captured-photo view.
 * Thumbnails use ContentResolver.loadThumbnail(Size) to avoid decoding 50–200MP
 * originals into a full-resolution Bitmap.
 */
@Composable
internal fun CameraGalleryDialog(
    refreshToken: Int,
    initialUri: Uri?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var deleteTarget by remember { mutableStateOf<Uri?>(null) }
    var infoText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, initialUri) {
        android.util.Log.i("FUJI_GALLERY", "ALBUM_OPEN initialUri=$initialUri")
        isLoading = true
        val captured = withContext(Dispatchers.IO) {
            queryFujiFilmPhotos(context)
        }
        photos = buildList {
            initialUri?.takeIf { it !in captured }?.let(::add)
            addAll(captured)
        }
        selectedIndex = if (photos.isEmpty()) {
            null
        } else {
            initialUri?.let { uri -> photos.indexOf(uri).takeIf { it >= 0 } }
                ?: selectedIndex?.coerceIn(0, photos.lastIndex)
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color(0xF508090A))
        ) {
            if (selectedIndex != null && photos.isNotEmpty()) {
                val start = selectedIndex!!.coerceIn(0, photos.lastIndex)
                val pagerState = keyPagerState(start, photos.size)
                Column(Modifier.fillMaxSize()) {
                    val currentUri = photos.getOrNull(pagerState.currentPage) ?: photos[start]
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "ẢNH ĐÃ CHỤP",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                            galleryAction("CHIA SẺ") { shareGalleryPhoto(context, currentUri) }
                            galleryAction("THÔNG TIN") {
                                infoText = queryGalleryPhotoInfo(context, currentUri)
                            }
                            galleryAction("MỞ NGOÀI") { openGalleryPhotoExternally(context, currentUri) }
                            galleryAction("XÓA") { deleteTarget = currentUri }
                            galleryAction("ĐÓNG") { selectedIndex = null; infoText = null }
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        key = { page -> photos[page].toString() }
                    ) { page ->
                        GalleryPhoto(
                            uri = photos[page],
                            target = Size(3072, 3072),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                            fit = ContentScale.Fit,
                            allowZoom = true
                        )
                    }
                    Text(
                        "${pagerState.currentPage + 1} / ${photos.size}  ·  CHỤM/MỞ HAI NGÓN ĐỂ PHÓNG TO",
                        color = Color(0xFFBDBDBD),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp)
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "ẢNH ĐÃ CHỤP",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp
                            )
                            Text(
                                "${photos.size} ẢNH · FUJIFILM MODE",
                                color = Color(0xFF939393),
                                fontSize = 9.sp,
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Text(
                            "ĐÓNG",
                            color = Color(0xFFE4002B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp)
                        )
                    }

                    when {
                        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFE4002B))
                        }
                        photos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Chưa có ảnh trong Pictures/FujifilmMode",
                                color = Color(0xFFBDBDBD),
                                fontSize = 13.sp
                            )
                        }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 118.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(photos, key = { it.toString() }) { uri ->
                                val index = photos.indexOf(uri)
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(1.18f)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF202020))
                                        .clickable { selectedIndex = index }
                                ) {
                                    GalleryPhoto(
                                        uri = uri,
                                        target = Size(480, 480),
                                        modifier = Modifier.fillMaxSize(),
                                        fit = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa ảnh này?") },
            text = { Text("Ảnh sẽ bị xóa khỏi thư mục Pictures/FujifilmMode trên thiết bị.") },
            confirmButton = {
                TextButton(onClick = {
                    val deleted = runCatching { context.contentResolver.delete(target, null, null) > 0 }
                        .getOrDefault(false)
                    if (deleted) {
                        photos = photos.filterNot { it == target }
                        selectedIndex = null
                        infoText = null
                    }
                    deleteTarget = null
                }) { Text("XÓA") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("HỦY") } }
        )
    }
    infoText?.let { details ->
        AlertDialog(
            onDismissRequest = { infoText = null },
            title = { Text("Thông tin ảnh") },
            text = { Text(details) },
            confirmButton = { TextButton(onClick = { infoText = null }) { Text("ĐÓNG") } }
        )
    }
}

@Composable
private fun galleryAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color(0xFFF4F4F4),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF25292C))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp)
    )
}

private fun shareGalleryPhoto(context: android.content.Context, uri: Uri) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(context.contentResolver, "FujiFilm Mode photo", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Chia sẻ ảnh"))
    }.onFailure {
        android.util.Log.e("FUJI_GALLERY", "Could not share photo", it)
    }
}

private fun openGalleryPhotoExternally(context: android.content.Context, uri: Uri) {
    runCatching {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(view)
    }.onFailure {
        android.util.Log.e("FUJI_GALLERY", "No external viewer could open image", it)
    }
}

private fun queryGalleryPhotoInfo(context: android.content.Context, uri: Uri): String =
    runCatching {
        val columns = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use "Không đọc được siêu dữ liệu ảnh."
            fun value(column: String): String {
                val index = cursor.getColumnIndex(column)
                return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else "—"
            }
            val bytes = value(MediaStore.Images.Media.SIZE).toLongOrNull()
            val mp = value(MediaStore.Images.Media.WIDTH).toLongOrNull()
                ?.times(value(MediaStore.Images.Media.HEIGHT).toLongOrNull() ?: 0L)
                ?.div(1_000_000.0)
            buildString {
                appendLine("Tên: ${value(MediaStore.Images.Media.DISPLAY_NAME)}")
                appendLine("Định dạng: ${value(MediaStore.Images.Media.MIME_TYPE)}")
                appendLine("Kích thước: ${value(MediaStore.Images.Media.WIDTH)} × ${value(MediaStore.Images.Media.HEIGHT)}")
                appendLine("Độ phân giải: ${mp?.let { String.format(java.util.Locale.US, "%.2f MP", it) } ?: "—"}")
                appendLine("Dung lượng: ${bytes?.let { String.format(java.util.Locale.US, "%.2f MB", it / 1_048_576.0) } ?: "—"}")
                append("Thư mục: Pictures/FujifilmMode")
            }
        } ?: "Không đọc được thông tin ảnh."
    }.getOrElse { failure ->
        android.util.Log.w("FUJI_GALLERY", "Could not read image details", failure)
        "Không đọc được thông tin ảnh: ${failure.message ?: "lỗi không xác định"}"
    }

@Composable
private fun keyPagerState(initialPage: Int, pageCount: Int) =
    androidx.compose.runtime.key(initialPage, pageCount) {
        rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
    }

private object GalleryThumbnailCache {
    private const val MAX_BYTES = 32 * 1024 * 1024
    private val cache = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.allocationByteCount.coerceAtLeast(value.byteCount)

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted) android.util.Log.i("FUJI_GALLERY", "BITMAP_CACHE_EVICT key=$key bytes=${oldValue.byteCount}")
            // Never recycle: Compose may still hold this Bitmap through ImageBitmap.
        }
    }

    fun load(context: android.content.Context, uri: Uri, target: Size): Bitmap {
        val modified = runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATE_MODIFIED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)
        val key = "$uri|$modified|${target.width}x${target.height}"
        cache.get(key)?.let { return it }
        val bitmap = context.contentResolver.loadThumbnail(uri, target, null)
        cache.put(key, bitmap)
        return bitmap
    }
}

@Composable
private fun GalleryPhoto(
    uri: Uri,
    target: Size,
    modifier: Modifier,
    fit: ContentScale,
    allowZoom: Boolean = false
) {
    val context = LocalContext.current
    var zoom by remember(uri) { mutableStateOf(1f) }
    var panX by remember(uri) { mutableStateOf(0f) }
    var panY by remember(uri) { mutableStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
        panX += panChange.x
        panY += panChange.y
        if (zoom <= 1.01f) {
            zoom = 1f
            panX = 0f
            panY = 0f
        }
    }
    val zoomModifier = if (allowZoom) {
        Modifier.transformable(state = transformState).graphicsLayer {
            scaleX = zoom
            scaleY = zoom
            translationX = panX
            translationY = panY
        }
    } else Modifier
    var bitmap by remember(uri, target) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(uri, target) { mutableStateOf(false) }

    LaunchedEffect(uri, target) {
        failed = false
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                GalleryThumbnailCache.load(context, uri, target)
            }.onFailure {
                android.util.Log.e("FUJI_GALLERY", "ALBUM_THUMB_LOAD uri=$uri result=fail", it)
            }.getOrNull()?.also {
                android.util.Log.i("FUJI_GALLERY", "ALBUM_THUMB_LOAD uri=$uri result=ok")
            }
        }
        failed = bitmap == null
    }
    when {
        bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Ảnh đã chụp",
            modifier = modifier.then(zoomModifier),
            contentScale = fit
        )
        failed -> Box(modifier.background(Color(0xFF202020)), contentAlignment = Alignment.Center) {
            Text("Không đọc được ảnh", color = Color(0xFF9B9B9B), fontSize = 10.sp)
        }
        else -> Box(modifier.background(Color(0xFF202020)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFFE4002B),
                strokeWidth = 2.dp
            )
        }
    }
}

private fun queryFujiFilmPhotos(context: android.content.Context): List<Uri> {
    val resolver = context.contentResolver
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.RELATIVE_PATH
    )
    val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("Pictures/FujifilmMode%")
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    return runCatching {
        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            buildList {
                while (cursor.moveToNext()) {
                    add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)))
                }
            }
        } ?: emptyList()
    }.getOrElse {
        android.util.Log.e("FUJI_GALLERY", "Could not query captured FujiFilm Mode images", it)
        emptyList()
    }
}
