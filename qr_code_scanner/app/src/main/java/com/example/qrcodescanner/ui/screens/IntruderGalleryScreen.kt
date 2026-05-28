package com.example.qrcodescanner.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescanner.data.AppLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decode a thumbnail-sized bitmap off the main thread to avoid UI lag. */
private suspend fun loadThumbnail(file: File, reqWidth: Int = 300, reqHeight: Int = 300): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            var sampleSize = 1
            while ((opts.outWidth / sampleSize) > reqWidth || (opts.outHeight / sampleSize) > reqHeight) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
        } catch (e: Exception) {
            null
        }
    }

/** Decode a full-resolution bitmap off the main thread. */
private suspend fun loadFullBitmap(file: File): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

@Composable
fun IntruderGalleryScreen(
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var intruderPhotos by remember { mutableStateOf(emptyList<File>()) }
    var selectedPhoto by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Load photo list on mount
    LaunchedEffect(Unit) {
        val intrudersDir = File(context.filesDir, "intruders")
        if (intrudersDir.exists() && intrudersDir.isDirectory) {
            intruderPhotos = intrudersDir.listFiles()
                ?.toList()
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    if (intruderPhotos.isEmpty()) {
        EmptyVaultState(
            title = if (lang == AppLanguage.AR) "لا يوجد دخلاء" else "No Intruders",
            description = if (lang == AppLanguage.AR) "لم يتم تسجيل أي محاولات دخول فاشلة." else "No failed login attempts recorded.",
            icon = Icons.Default.Warning
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == AppLanguage.AR) "صور الدخلاء" else "Intruder Photos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFE4A49))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == AppLanguage.AR) "مسح الكل" else "Clear All")
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(intruderPhotos, key = { it.absolutePath }) { photoFile ->
                    IntruderPhotoItem(
                        file = photoFile,
                        onClick = { selectedPhoto = photoFile }
                    )
                }
            }
        }
    }

    // Photo Details Dialog – full resolution loaded async
    selectedPhoto?.let { photo ->
        var fullBitmap by remember(photo) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(photo) {
            fullBitmap = loadFullBitmap(photo)
        }
        val dateFormatted = remember(photo) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(photo.lastModified()))
        }

        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    if (lang == AppLanguage.AR) "تفاصيل الدخيل" else "Intruder Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFFFE4A49), RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fullBitmap != null) {
                            Image(
                                bitmap = fullBitmap!!.asImageBitmap(),
                                contentDescription = "Intruder",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            CircularProgressIndicator(color = Color(0xFFFE4A49), strokeWidth = 2.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (lang == AppLanguage.AR) "وقت المحاولة: $dateFormatted" else "Attempt Time: $dateFormatted",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        photo.delete()
                        intruderPhotos = intruderPhotos.filter { it != photo }
                        selectedPhoto = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE4A49))
                ) {
                    Text(if (lang == AppLanguage.AR) "حذف الصورة" else "Delete Photo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPhoto = null }) {
                    Text(if (lang == AppLanguage.AR) "إغلاق" else "Close", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    if (lang == AppLanguage.AR) "مسح جميع الصور؟" else "Clear all photos?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (lang == AppLanguage.AR) "هل أنت متأكد من مسح جميع صور الدخلاء؟" else "Are you sure you want to clear all intruder photos?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        intruderPhotos.forEach { it.delete() }
                        intruderPhotos = emptyList()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE4A49))
                ) {
                    Text(if (lang == AppLanguage.AR) "مسح" else "Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (lang == AppLanguage.AR) "إلغاء" else "Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun IntruderPhotoItem(file: File, onClick: () -> Unit) {
    // Load a small thumbnail asynchronously – never blocks the UI thread
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file) {
        bitmap = loadThumbnail(file)
    }

    val dateStr = remember(file) {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
    }

    // Shimmer animation while loading
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Shimmer placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A2A).copy(alpha = shimmerAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.LightGray.copy(alpha = shimmerAlpha)
                    )
                }
            }

            // Gradient / Timestamp overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = dateStr,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
