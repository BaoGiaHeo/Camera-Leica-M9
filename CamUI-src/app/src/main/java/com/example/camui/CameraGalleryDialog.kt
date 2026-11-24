package com.example.camui

import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(refreshToken, initialUri) {
        isLoading = true
        val captured = withContext(Dispatchers.IO) {
            queryFujiFilmPhotos(context)
        }
        photos = buildList {
            initialUri?.takeIf { it !in captured }?.let(::add)
            addAll(captured)
        }
        selectedIndex = if (photos.isEmpty()) null else selectedIndex?.coerceIn(0, photos.lastIndex)
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
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "ẢNH ĐÃ CHỤP",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp
                        )
                        Text(
                            "ĐÓNG",
                            color = Color(0xFFE4002B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { selectedIndex = null }.padding(10.dp)
                        )
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        key = { page -> photos[page].toString() }
                    ) { page ->
                        GalleryPhoto(
                            uri = photos[page],
                            target = Size(2048, 2048),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                            fit = ContentScale.Fit
                        )
                    }
                    Text(
                        "${pagerState.currentPage + 1} / ${photos.size}",
                        color = Color(0xFFBDBDBD),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp)
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
}

@Composable
private fun keyPagerState(initialPage: Int, pageCount: Int) =
    androidx.compose.runtime.key(initialPage, pageCount) {
        rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
    }

@Composable
private fun GalleryPhoto(
    uri: Uri,
    target: Size,
    modifier: Modifier,
    fit: ContentScale
) {
    val context = LocalContext.current
    var bitmap by remember(uri, target) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(uri, target) { mutableStateOf(false) }

    LaunchedEffect(uri, target) {
        failed = false
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, target, null)
            }.getOrNull()
        }
        failed = bitmap == null
    }
    DisposableEffect(bitmap) {
        onDispose {
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    when {
        bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Ảnh đã chụp",
            modifier = modifier,
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
