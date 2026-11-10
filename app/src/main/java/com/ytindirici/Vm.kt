package com.ytindirici

import android.app.Application
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var urls by mutableStateOf("")
    var audio by mutableStateOf(true)
    var quality by mutableStateOf("1080")
    var cover by mutableStateOf(true)
    var running by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var status by mutableStateOf("Hazır")
    val logs = mutableStateListOf<String>()

    private var cancelled = false
    private var pid: String? = null

    private fun log(s: String) { logs.add(s) }

    fun cancel() {
        cancelled = true
        status = "İptal ediliyor…"
        val p = pid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try { YoutubeDL.getInstance().destroyProcessById(p) } catch (_: Exception) {}
        }
    }

    fun start() {
        val list = urls.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (list.isEmpty()) { status = "Lütfen en az bir link yapıştır."; return }
        val app = getApplication<App>()
        if (!app.initOk) { log("✖ Başlatma hatası: ${app.initError}"); return }

        running = true
        cancelled = false
        progress = 0f
        status = "Başlıyor…"

        viewModelScope.launch(Dispatchers.IO) {
            val tmp = File(app.cacheDir, "dl")
            tmp.deleteRecursively()
            tmp.mkdirs()
            try {
                for (u in list) {
                    if (cancelled) break
                    runOne(u, tmp)
                }
            } finally {
                val n = saveFiles(tmp)
                tmp.deleteRecursively()
                progress = if (cancelled) progress else 1f
                status = if (cancelled) "İptal edildi." else "Tamamlandı ✔"
                log("$n dosya İndirilenler / YTIndirici klasörüne kaydedildi.")
                running = false
            }
        }
    }

    private fun runOne(url: String, tmp: File) {
        val req = YoutubeDLRequest(url)
        req.addOption("-o", tmp.absolutePath + "/%(playlist_title|)s/%(playlist_index&{} - |)s%(title)s.%(ext)s")
        req.addOption("--ignore-errors")
        req.addOption("--no-mtime")
        req.addOption("--windows-filenames")
        req.addOption("--embed-metadata")
        req.addOption("--no-update")
        // YouTube engellerini aşmak için Android istemci parametresi eklendi:
        req.addOption("--extractor-args", "youtube:player_client=android")
        
        if (audio) {
            req.addOption("-f", "bestaudio/best")
            req.addOption("-x")
            req.addOption("--audio-format", "mp3")
            req.addOption("--audio-quality", "192K")
            if (cover) {
                req.addOption("--embed-thumbnail")
                req.addOption("--convert-thumbnails", "jpg")
            }
        } else {
            val h = quality
            req.addOption(
                "-f",
                "bv*[height<=$h][ext=mp4]+ba[ext=m4a]/b[height<=$h][ext=mp4]/bv*[height<=$h]+ba/b"
            )
            req.addOption("--merge-output-format", "mp4")
        }

        val id = "dl-" + System.nanoTime()
        pid = id
        var item = 1
        var total = 1
        val itemRe = Regex("Downloading item (\\d+) of (\\d+)")
        try {
            YoutubeDL.getInstance().execute(req, id) { p, _, line ->
                itemRe.find(line)?.let {
                    item = it.groupValues[1].toInt()
                    total = it.groupValues[2].toInt()
                }
                if (line.contains("[download] Destination:")) {
                    log("⬇ " + File(line.substringAfter("Destination:").trim()).name)
                }
                val frac = if (p < 0f) 0f else p / 100f
                progress = (((item - 1) + frac) / total).coerceIn(0f, 1f)
                status = "[$item/$total]  %" + (if (p < 0f) 0 else p.toInt())
            }
        } catch (e: Exception) {
            if (!cancelled) log("✖ " + (e.message ?: "Bilinmeyen hata").take(300))
        }
    }

    private fun saveFiles(root: File): Int {
        val resolver = getApplication<Application>().contentResolver
        val audioExt = setOf("mp3", "m4a", "opus", "ogg", "flac", "wav", "aac")
        val videoExt = setOf("mp4", "mkv", "webm")
        val mimes = mapOf(
            "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "opus" to "audio/ogg",
            "ogg" to "audio/ogg", "flac" to "audio/flac", "wav" to "audio/wav",
            "aac" to "audio/aac", "mp4" to "video/mp4", "mkv" to "video/x-matroska",
            "webm" to "video/webm"
        )
        var count = 0
        root.walkTopDown().filter { it.isFile }.forEach { f ->
            val ext = f.extension.lowercase()
            val isAudio = ext in audioExt
            if (!isAudio && ext !in videoExt) return@forEach
            val sub = f.parentFile?.relativeTo(root)?.path?.replace('\\', '/')?.trim('/') ?: ""
            
            val rel = listOf("YTIndirici", sub).filter { it.isNotEmpty() }.joinToString("/")
            
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimes[ext] ?: "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            try {
                val uri = resolver.insert(collection, values) ?: return@forEach
                resolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                count++
            } catch (e: Exception) {
                log("✖ Kaydedilemedi: ${e.message}")
            }
        }
        return count
    }
}
