package com.example.qrcodescanner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalkthroughDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161618)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1E22), Color(0xFF121214))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Slides Horizontal Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        val slideInfo = when (page) {
                            0 -> SlideInfo(
                                title = L10n.get("wt_welcome_title", lang),
                                description = L10n.get("wt_welcome_desc", lang),
                                icon = Icons.Filled.Slideshow,
                                neonColor = Color(0xFF00F2FE)
                            )
                            1 -> SlideInfo(
                                title = L10n.get("wt_scan_title", lang),
                                description = L10n.get("wt_scan_desc", lang),
                                icon = Icons.Filled.QrCodeScanner,
                                neonColor = Color(0xFF00FF87)
                            )
                            2 -> SlideInfo(
                                title = L10n.get("wt_vault_title", lang),
                                description = L10n.get("wt_vault_desc", lang),
                                icon = Icons.Filled.Security,
                                neonColor = Color(0xFFFF007F)
                            )
                            else -> SlideInfo(
                                title = L10n.get("wt_widget_title", lang),
                                description = L10n.get("wt_widget_desc", lang),
                                icon = Icons.Filled.Widgets,
                                neonColor = Color(0xFFFFD700)
                            )
                        }

                        SlideContent(slideInfo)
                    }

                    // Bottom Navigation Elements (Indicators and Buttons)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dot Indicators
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF00F2FE) else Color.Gray.copy(alpha = 0.5f))
                                )
                            }
                        }

                        // Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back / Skip Button
                            if (pagerState.currentPage > 0) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = L10n.get("wt_back", lang),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                TextButton(onClick = onDismiss) {
                                    Text(
                                        text = L10n.get("wt_skip", lang),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Next / Start Button
                            if (pagerState.currentPage < 3) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F2FE)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = L10n.get("wt_next", lang),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FF87)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = L10n.get("wt_start", lang),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
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

private data class SlideInfo(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val neonColor: Color
)

@Composable
private fun SlideContent(slideInfo: SlideInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Neon Glowing Icon Ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(slideInfo.neonColor.copy(alpha = 0.15f), CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slideInfo.icon,
                contentDescription = null,
                tint = slideInfo.neonColor,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Slide Title
        Text(
            text = slideInfo.title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Slide Description
        Text(
            text = slideInfo.description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
