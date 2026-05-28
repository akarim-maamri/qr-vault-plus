package com.example.qrcodescanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescanner.data.ScanResultEntity
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<ScanResultEntity>,
    onDeleteResult: (ScanResultEntity) -> Unit,
    onClearAll: () -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        L10n.get("history_title", lang).uppercase(),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = L10n.get("btn_clear_all", lang),
                                tint = Color(0xFFFE4A49)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121212))
        ) {
            if (historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = L10n.get("history_empty", lang),
                        tint = Color.DarkGray,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = L10n.get("history_empty", lang),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.AR) "سيتم عرض الرموز الممسوحة ضوئياً هنا تلقائياً." else "Les codes QR scannés apparaîtront automatiquement ici.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = LocalTextStyle.current.copy(lineHeight = 20.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList, key = { it.id }) { result ->
                        HistoryItemRow(
                            result = result,
                            lang = lang,
                            onItemClick = {
                                handleItemInteraction(context, result.content, result.type, lang)
                            },
                            onDelete = { onDeleteResult(result) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    result: ScanResultEntity,
    lang: AppLanguage,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (result.type) {
        "URL" -> Icons.Default.Language
        "WIFI" -> Icons.Default.Wifi
        "PHONE" -> Icons.Default.Phone
        "EMAIL" -> Icons.Default.Email
        else -> Icons.Default.TextSnippet
    }

    val iconColor = when (result.type) {
        "URL" -> Color(0xFF00F2FE)
        "WIFI" -> Color(0xFF4FACFE)
        "PHONE" -> Color(0xFF00FF87)
        "EMAIL" -> Color(0xFFFF5E62)
        else -> Color(0xFFFFD97D)
    }

    val categoryKey = when (result.type) {
        "URL" -> "category_url"
        "WIFI" -> "category_wifi"
        "PHONE" -> "category_phone"
        "EMAIL" -> "category_email"
        else -> "category_text"
    }
    val localizedCategory = L10n.get(categoryKey, lang)

    val relativeTime = DateUtils.getRelativeTimeSpanString(
        result.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
            .clickable(onClick = onItemClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container with neon glow
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f))
                .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = localizedCategory,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedCategory,
                    color = iconColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = relativeTime,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.content,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = if (lang == AppLanguage.AR) "حذف العنصر" else "Supprimer l'élément",
                tint = Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun handleItemInteraction(context: Context, content: String, type: String, lang: AppLanguage) {
    if (type == "URL") {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
            context.startActivity(intent)
        } catch (e: Exception) {
            copyToClipboard(context, content, lang)
        }
    } else {
        copyToClipboard(context, content, lang)
    }
}

private fun copyToClipboard(context: Context, text: String, lang: AppLanguage) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Scanned QR", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, L10n.get("copied_toast", lang), Toast.LENGTH_SHORT).show()
}
