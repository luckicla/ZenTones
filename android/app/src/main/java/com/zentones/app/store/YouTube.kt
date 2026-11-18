package com.zentones.app.store

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Búsqueda y descarga de audio con yt-dlp (vía youtubedl-android, que trae Python y yt-dlp dentro). */
object YouTube {
    data class Video(val id: String, val title: String, val channel: String, val duration: Double, val url: String)

    private val mutex = Mutex()
    @Volatile private var ready = false
    private const val PROC = "zentones-yt"
    private val main = Handler(Looper.getMainLooper())

    /** Prepara yt-dlp y lo actualiza como mucho una vez al día (YouTube cambia a menudo). */
    suspend fun ensure(ctx: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (ready) return@withLock
            val app = ctx.applicationContext
            YoutubeDL.getInstance().init(app)
            val sp = app.getSharedPreferences("zentones", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (now - sp.getLong("ytdlpUpdate", 0L) > 24 * 3600 * 1000L) {
                runCatching { YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel.STABLE) }
                    .onSuccess { sp.edit().putLong("ytdlpUpdate", now).apply() }
            }
            ready = true
        }
    }

    fun ytdlpVersion(ctx: Context): String? = runCatching { YoutubeDL.getInstance().version(ctx.applicationContext) }.getOrNull()

    /** Texto de búsqueda o enlace directo de YouTube. */
    suspend fun search(ctx: Context, query: String, n: Int = 15): List<Video> = withContext(Dispatchers.IO) {
        ensure(ctx)
        val q = query.trim()
        val isUrl = q.startsWith("http://") || q.startsWith("https://")
        val req = YoutubeDLRequest(if (isUrl) q else "ytsearch$n:$q")
            .addOption("--flat-playlist")
            .addOption("--dump-single-json")
            .addOption("--no-warnings")
        val out = YoutubeDL.getInstance().execute(req).out
        val j = JSONObject(out.substring(out.indexOf('{')))
        val entries = j.optJSONArray("entries")
        val list = if (entries == null) listOf(j) else (0 until entries.length()).map { entries.getJSONObject(it) }
        list.mapNotNull { e ->
            val id = e.optString("id").ifEmpty { return@mapNotNull null }
            Video(
                id = id,
                title = e.optString("title").ifEmpty { id },
                channel = e.optString("channel").ifEmpty { e.optString("uploader") },
                duration = e.optDouble("duration", 0.0).let { if (it.isNaN()) 0.0 else it },
                url = e.optString("webpage_url").ifEmpty { "https://www.youtube.com/watch?v=$id" },
            )
        }
    }

    /** Baja solo la pista de audio (sin vídeo). onProgress de 0 a 1, en el hilo principal. */
    suspend fun downloadAudio(ctx: Context, v: Video, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        ensure(ctx)
        val dir = File(ctx.cacheDir, "youtube").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val req = YoutubeDLRequest(v.url)
            .addOption("-f", "bestaudio[ext=m4a]/bestaudio")
            .addOption("--no-playlist")
            .addOption("--no-warnings")
            .addOption("-o", dir.absolutePath + "/%(id)s.%(ext)s")
        YoutubeDL.getInstance().execute(req, PROC) { p, _, _ -> main.post { onProgress((p / 100f).coerceIn(0f, 1f)) } }
        dir.listFiles()?.filter { it.isFile && it.length() > 0 && !it.name.endsWith(".part") }?.maxByOrNull { it.length() }
            ?: error("yt-dlp no ha devuelto ningún archivo")
    }

    fun cancel() {
        runCatching { YoutubeDL.getInstance().destroyProcessById(PROC) }
    }

    /** Copia el audio original a Descargas/Zentones. */
    suspend fun saveToDownloads(ctx: Context, file: File, title: String): String = withContext(Dispatchers.IO) {
        val ext = file.extension.ifEmpty { "m4a" }
        val safe = title.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().take(80).ifEmpty { "audio" }
        val name = "$safe.$ext"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (ext == "webm") "audio/webm" else "audio/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Zentones/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val cr = ctx.contentResolver
        val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("No se pudo crear el archivo")
        cr.openOutputStream(uri)?.use { o -> file.inputStream().use { it.copyTo(o) } }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        cr.update(uri, values, null, null)
        "Descargas/Zentones/$name"
    }
}
