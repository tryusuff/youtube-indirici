package com.ytindirici

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Accent = Color(0xFFFF3B4E)
private val Bg = Color(0xFF121216)
private val CardBg = Color(0xFF1C1C22)
private val Chip = Color(0xFF2A2A33)
private val Muted = Color(0xFF8B8B99)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent, onPrimary = Color.White,
                    background = Bg, surface = CardBg,
                    onBackground = Color.White, onSurface = Color.White
                )
            ) { Screen() }
        }
    }
}

@Composable
fun Screen(vm: MainViewModel = viewModel()) {
    val clip = LocalClipboardManager.current
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = Chip, labelColor = Color.White,
        selectedContainerColor = Accent, selectedLabelColor = Color.White
    )
    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("YT İndirici", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("YouTube ve YouTube Music playlist, şarkı ve videolarını indir", color = Muted, fontSize = 13.sp)

            // Linkler
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Linkler (her satıra bir tane)", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            clip.getText()?.text?.let { vm.urls = (vm.urls + "\n" + it).trim() }
                        }) { Text("Yapıştır", color = Accent) }
                    }
                    OutlinedTextField(
                        value = vm.urls, onValueChange = { vm.urls = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                        placeholder = { Text("https://music.youtube.com/playlist?list=…", color = Muted, fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Format
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Format", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = vm.audio, onClick = { vm.audio = true },
                            label = { Text("Ses (MP3)") }, colors = chipColors)
                        FilterChip(selected = !vm.audio, onClick = { vm.audio = false },
                            label = { Text("Video (MP4)") }, colors = chipColors)
                    }
                    if (vm.audio) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kapak resmini göm", modifier = Modifier.weight(1f), color = Muted)
                            Switch(checked = vm.cover, onCheckedChange = { vm.cover = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Accent))
                        }
                    } else {
                        Text("Maks. çözünürlük", color = Muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1080", "720", "480", "360").forEach { q ->
                                FilterChip(selected = vm.quality == q, onClick = { vm.quality = q },
                                    label = { Text(q) }, colors = chipColors)
                            }
                        }
                    }
                }
            }

            // Butonlar
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.start() }, enabled = !vm.running,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text("İndir", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { vm.cancel() }, enabled = vm.running,
                    modifier = Modifier.height(52.dp), shape = RoundedCornerShape(14.dp)
                ) { Text("İptal", color = Color.White) }
            }

            Text(vm.status, color = Muted, fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { vm.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Accent, trackColor = Chip
            )

            // Log
            val listState = rememberLazyListState()
            LaunchedEffect(vm.logs.size) {
                if (vm.logs.isNotEmpty()) listState.animateScrollToItem(vm.logs.size - 1)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(220.dp)
            ) {
                LazyColumn(state = listState, modifier = Modifier.padding(12.dp)) {
                    items(vm.logs) { Text(it, fontSize = 12.sp, color = Muted) }
                }
            }
        }
    }
}
