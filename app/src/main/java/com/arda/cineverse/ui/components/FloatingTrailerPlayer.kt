package com.arda.cineverse.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.viewmodel.TrailerPlayerUiState
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppYouTubePlayer(
    videoKey: String,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    showControls: Boolean = true,
) {
    val context = LocalContext.current

    val htmlContent = remember(videoKey, isMuted, showControls) {
        val muteParam = if (isMuted) 1 else 0
        val controlsParam = if (showControls) 1 else 0
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                .embed-container { position: relative; width: 100%; height: 100%; }
                .embed-container iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
            </style>
        </head>
        <body>
            <div class="embed-container">
                <iframe 
                    src="https://www.youtube-nocookie.com/embed/$videoKey?autoplay=1&mute=$muteParam&playsinline=1&enablejsapi=1&rel=0&controls=$controlsParam&modestbranding=1&loop=1&playlist=$videoKey" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                    allowfullscreen>
                </iframe>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    val webView = remember(videoKey, isMuted, showControls) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // Dışarıya yönlendirme yapma, video tamamen CineVerse içinde kalsın
                    return false
                }
            }
            loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "utf-8", null)
        }
    }

    DisposableEffect(videoKey, isMuted, showControls) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}

@Composable
fun FloatingTrailerPlayer(
    uiState: TrailerPlayerUiState,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    val videoKey = uiState.videoKey ?: return

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!uiState.isMinimized) {
            // UYGULAMA İÇİ ODAK MODU (Tam Ekran Overlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Üst Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.title.takeIf { !it.isNullOrBlank() } ?: "Fragman",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onMinimize) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Küçült (Yüzen Pencere)",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Video Oynatıcı Ekranı (16:9)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        InAppYouTubePlayer(
                            videoKey = videoKey,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            // YÜZEN MİNİ PENCERE MODU (In-App PiP - Sürüklenebilir)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Card(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .width(250.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        // Mini Başlık Barı
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Primary.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.title.takeIf { !it.isNullOrBlank() } ?: "Fragman",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInFull,
                                    contentDescription = "Büyüt",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onExpand() }
                                        .padding(1.dp)
                                )
                                Box(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onClose() }
                                        .padding(1.dp)
                                )
                            }
                        }

                        // Mini Video Oynatıcı Ekranı
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            InAppYouTubePlayer(
                                videoKey = videoKey,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
