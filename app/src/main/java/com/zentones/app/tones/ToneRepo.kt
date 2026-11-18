package com.zentones.app.tones

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Elemento que se muestra en listas y selectores. */
data class ToneItem(val id: String, val name: String, val desc: String, val kind: ToneKind, val mine: Boolean)

/** Tono guardado por el usuario: importado, editado o bajado de la tienda. */
data class LibTone(val id: String, val name: String, val kind: ToneKind, val file: String, val origin: String, val added: Long)

object Library {
    val items = MutableStateFlow<List<LibTone>>(emptyList())
    private var loaded = false

    fun dir(ctx: Context) = File(ctx.filesDir, "library").apply { mkdirs() }
    private fun index(ctx: Context) = File(ctx.filesDir, "library.json")

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        items.value = runCatching {
            val arr = JSONArray(index(ctx).readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LibTone(
                    o.getString("id"), o.getString("name"),
                    if (o.optString("kind") == "CALL") ToneKind.CALL else ToneKind.MESSAGE,
                    o.getString("file"), o.optString("origin"), o.optLong("added"),
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    private fun save(ctx: Context, list: List<LibTone>) {
        items.value = list
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind.name)
                .put("file", it.file).put("origin", it.origin).put("added", it.added))
        }
        index(ctx).writeText(arr.toString())
    }

    /** Guarda PCM mono 16 bit como un tono nuevo de la biblioteca. */
    fun addPcm(ctx: Context, name: String, kind: ToneKind, pcm: ShortArray, sampleRate: Int, origin: String): LibTone {
        init(ctx)
        val id = "lib:" + UUID.randomUUID().toString().take(8)
        val fileName = id.removePrefix("lib:") + ".wav"
        Wav.write(File(dir(ctx), fileName), pcm, sampleRate)
        val t = LibTone(id, name.ifBlank { "Mi tono" }, kind, fileName, origin, System.currentTimeMillis())
        save(ctx, listOf(t) + items.value)
        return t
    }

    fun remove(ctx: Context, id: String) {
        init(ctx)
        items.value.find { it.id == id }?.let { File(dir(ctx), it.file).delete() }
        save(ctx, items.value.filterNot { it.id == id })
    }

    fun update(ctx: Context, id: String, name: String, kind: ToneKind) {
        init(ctx)
        save(ctx, items.value.map { if (it.id == id) it.copy(name = name, kind = kind) else it })
    }
}

object ToneRepo {

    fun choices(kind: ToneKind): List<ToneItem> {
        val mine = Library.items.value.filter { it.kind == kind }
            .map { ToneItem(it.id, it.name, it.origin, it.kind, true) }
        val builtin = BuiltinTones.all.filter { it.kind == kind }
            .map { ToneItem(it.id, it.name, it.desc, it.kind, false) }
        return mine + builtin
    }

    fun name(id: String?): String? {
        if (id == null) return null
        BuiltinTones.byId(id)?.let { return it.name }
        return Library.items.value.find { it.id == id }?.name
    }

    /** Devuelve el WAV del tono, sintetizándolo y cacheándolo la primera vez. Bloqueante. */
    @Synchronized
    fun file(ctx: Context, id: String): File? {
        Library.init(ctx)
        BuiltinTones.byId(id)?.let { tone ->
            val dir = File(ctx.filesDir, "tones").apply { mkdirs() }
            val f = File(dir, tone.id.removePrefix("syn:") + ".wav")
            if (!f.exists() || f.length() < 100) {
                val tmp = File(dir, f.name + ".tmp")
                Wav.write(tmp, ToneSynth.render(tone), ToneSynth.SR)
                tmp.renameTo(f)
            }
            return f
        }
        val lib = Library.items.value.find { it.id == id } ?: return null
        return File(Library.dir(ctx), lib.file).takeIf { it.exists() }
    }

    /**
     * Copia el tono a la carpeta pública (Ringtones / Notifications / Alarms) para que el sistema
     * pueda usarlo como tono predeterminado.
     */
    suspend fun export(ctx: Context, id: String, type: Int): Uri = withContext(Dispatchers.IO) {
        val f = file(ctx, id) ?: error("No encuentro el tono")
        val name = name(id) ?: "Tono"
        val (dir, flag) = when (type) {
            RingtoneManager.TYPE_RINGTONE -> Environment.DIRECTORY_RINGTONES to MediaStore.Audio.AudioColumns.IS_RINGTONE
            RingtoneManager.TYPE_ALARM -> Environment.DIRECTORY_ALARMS to MediaStore.Audio.AudioColumns.IS_ALARM
            else -> Environment.DIRECTORY_NOTIFICATIONS to MediaStore.Audio.AudioColumns.IS_NOTIFICATION
        }
        val rel = "$dir/Zentones/"
        val safe = id.substringAfter(':').replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val display = "Zentones_$safe.wav"
        val coll = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val cr = ctx.contentResolver
        cr.query(
            coll, arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(display, rel), null,
        )?.use { c ->
            if (c.moveToFirst()) {
                val existing = ContentUris.withAppendedId(coll, c.getLong(0))
                // Reescribimos por si el tono se ha editado
                runCatching { cr.openOutputStream(existing, "wt")?.use { o -> f.inputStream().use { it.copyTo(o) } } }
                return@withContext existing
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, display)
            put(MediaStore.MediaColumns.TITLE, "Zentones · $name")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            put(flag, 1)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = cr.insert(coll, values) ?: error("No se pudo crear el archivo de tono")
        cr.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        cr.update(uri, values, null, null)
        uri
    }

    fun systemTitle(ctx: Context, type: Int): String? {
        val uri = runCatching { RingtoneManager.getActualDefaultRingtoneUri(ctx, type) }.getOrNull() ?: return null
        return runCatching { RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx)?.removePrefix("Zentones · ") }.getOrNull()
    }
}

/** Reproduce vistas previas: tonos locales (por id) o streams de la tienda (por URL). */
object Previewer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mp: MediaPlayer? = null
    val playing = MutableStateFlow<String?>(null)

    fun toggle(ctx: Context, key: String) = if (playing.value == key) stop() else play(ctx, key)

    fun play(ctx: Context, key: String) {
        stop()
        playing.value = key
        scope.launch {
            val source: String = if (key.startsWith("http")) key else {
                withContext(Dispatchers.IO) { ToneRepo.file(ctx, key)?.absolutePath } ?: run { stop(); return@launch }
            }
            if (playing.value != key) return@launch
            runCatching {
                mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                    )
                    setDataSource(source)
                    setOnPreparedListener { if (playing.value == key) it.start() }
                    setOnCompletionListener { if (playing.value == key) stop() }
                    setOnErrorListener { _, _, _ -> stop(); true }
                    prepareAsync()
                }
            }.onFailure { stop() }
        }
    }

    /** Reproduce un archivo concreto (el editor lo usa para escuchar el recorte). */
    fun playFile(path: String, key: String) {
        stop()
        playing.value = key
        runCatching {
            mp = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { if (playing.value == key) stop() }
                prepare(); start()
            }
        }.onFailure { stop() }
    }

    fun stop() {
        mp?.let { p -> runCatching { p.stop() }; p.release() }
        mp = null
        playing.value = null
    }
}
