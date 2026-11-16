package com.example.devfitness.ui.dashboard

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.devfitness.BuildConfig
import com.example.devfitness.data.api.ApiClient
import com.example.devfitness.data.api.ApiResult
import com.example.devfitness.data.api.DevFitnessRepository
import com.example.devfitness.data.model.GalleryItem
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(token: String) {
    val repository = remember(token) {
        DevFitnessRepository(ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }))
    }
    val moments = remember { mutableStateListOf<GalleryItem>() }
    var selected by remember { mutableStateOf<GalleryItem?>(null) }
    var message by remember { mutableStateOf("Loading club moments…") }
    LaunchedEffect(token) {
        when (val result = withContext(Dispatchers.IO) { repository.listGallery() }) {
            is ApiResult.Success -> {
                moments.clear()
                moments.addAll(result.value)
                message = if (moments.isEmpty()) "No club moments have been added yet." else ""
            }
            is ApiResult.Failure -> message = result.message
        }
    }
    Box(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        Column(Modifier.fillMaxSize()) {
            MemberPageHeader("Club gallery", "Photos, videos and unforgettable moments", Icons.Rounded.PhotoLibrary)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(155.dp),
                contentPadding = PaddingValues(14.dp, 6.dp, 14.dp, 30.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(moments, key = { it.id }) { moment ->
                    val video = moment.mimeType.startsWith("video/")
                    Box(
                        Modifier.fillMaxWidth().height(if (video) 190.dp else 220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FitnessPalette.InkRaised)
                            .clickable { selected = moment },
                    ) {
                    if (!video) {
                        AsyncImage(
                            model = moment.url,
                            contentDescription = moment.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(FitnessPalette.Ink.copy(alpha = 0.04f), FitnessPalette.Ink.copy(alpha = 0.92f)),
                            ),
                        ),
                    )
                    if (video) {
                        Icon(
                            Icons.Rounded.PlayCircle,
                            "Play video",
                            tint = FitnessPalette.Lime,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text(
                            moment.caption.ifBlank { if (video) "Club video" else "DEV Fitness moment" },
                            color = FitnessPalette.Label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(moment.momentDate.orEmpty(), color = FitnessPalette.Secondary, fontSize = 10.sp)
                    }
                    }
                }
                if (message.isNotBlank()) item { DataMessage(message) }
            }
        }
        selected?.let { moment ->
            GalleryMediaViewer(moment = moment, onClose = { selected = null })
        }
    }
}

@Composable
private fun GalleryMediaViewer(moment: GalleryItem, onClose: () -> Unit) {
    val video = moment.mimeType.startsWith("video/")
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        if (video) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        val controls = MediaController(context)
                        controls.setAnchorView(this)
                        setMediaController(controls)
                        setVideoURI(Uri.parse(moment.url))
                        setOnPreparedListener { player ->
                            player.isLooping = false
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = moment.url,
                contentDescription = moment.caption,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FitnessPalette.InkRaised),
        ) {
            Icon(Icons.Rounded.ArrowBack, "Close media viewer", tint = FitnessPalette.Label)
        }
        Text(
            moment.caption.ifBlank { if (video) "Club video" else "DEV Fitness moment" },
            color = FitnessPalette.Label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(FitnessPalette.Ink.copy(alpha = 0.82f))
                .padding(20.dp),
        )
    }
}
