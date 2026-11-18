package com.zentones.app.store

import android.content.Context
import com.zentones.app.BuildConfig
import com.zentones.app.tones.ToneKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class StoreCategory(val id: String, val name: String)

data class StoreTone(
    val id: String,
    val name: String,
    val category: String,
    val kind: ToneKind,
    val url: String,
    val duration: Double,
    val author: String,
    val license: String,
    val source: String,
)

/**
 * Dos fuentes:
 *  - Catálogo Zentones: catalog/catalog.json en tu repo de GitHub. Se actualiza con un push,
 *    sin publicar versión nueva de la app.
 *  - Freesound.org: cientos de miles de sonidos con licencias Creative Commons (necesita API key).
 */
object Store {
    private val base get() = "https://raw.githubusercontent.com/${BuildConfig.GITHUB_REPO}/main/catalog/"
    private var cache: Pair<List<StoreCategory>, List<StoreTone>>? = null

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Zentones-Android")
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        try {
            if (c.responseCode == 401) error("API key de Freesound no válida")
            if (c.responseCode !in 200..299) error("El servidor respondió ${c.responseCode}")
            return c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }

    suspend fun catalog(ctx: Context, force: Boolean = false): Pair<List<StoreCategory>, List<StoreTone>> =
        withContext(Dispatchers.IO) {
            cache?.takeIf { !force }?.let { return@withContext it }
            val disk = File(ctx.cacheDir, "catalog.json")
            val text = runCatching { get(base + "catalog.json").also { disk.writeText(it) } }
                .getOrElse { e -> if (disk.exists()) disk.readText() else throw e }
            val j = JSONObject(text)
            val cats = j.getJSONArray("categories").let { a ->
                (0 until a.length()).map { a.getJSONObject(it).let { o -> StoreCategory(o.getString("id"), o.getString("name")) } }
            }
            val tones = j.getJSONArray("tones").let { a ->
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    val file = o.getString("file")
                    StoreTone(
                        "z:" + o.getString("id"), o.getString("name"), o.optString("category"),
                        if (o.optString("kind") == "call") ToneKind.CALL else ToneKind.MESSAGE,
                        if (file.startsWith("http")) file else base + file,
                        o.optDouble("duration", 0.0), o.optString("author", "Zentones"),
                        o.optString("license", "CC0"), "Zentones",
                    )
                }
            }
            (cats to tones).also { cache = it }
        }

    suspend fun freesound(query: String, key: String, page: Int = 1): List<StoreTone> = withContext(Dispatchers.IO) {
        require(key.isNotBlank()) { "Falta la API key de Freesound" }
        val q = URLEncoder.encode(query, "UTF-8")
        val filter = URLEncoder.encode("duration:[0.3 TO 40]", "UTF-8")
        val url = "https://freesound.org/apiv2/search/text/?query=$q&filter=$filter&page=$page&page_size=30" +
            "&fields=id,name,duration,previews,username,license&token=$key"
        val j = JSONObject(get(url))
        val arr = j.getJSONArray("results")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val d = o.optDouble("duration", 0.0)
            StoreTone(
                "fs:" + o.getLong("id"),
                o.getString("name").substringBeforeLast('.'),
                "freesound",
                if (d >= 5.0) ToneKind.CALL else ToneKind.MESSAGE,
                o.getJSONObject("previews").getString("preview-hq-mp3"),
                d, o.optString("username"),
                o.optString("license").substringAfter("licenses/").trimEnd('/').uppercase().ifBlank { "CC" },
                "Freesound",
            )
        }
    }

    /** Descarga el audio a la caché y devuelve el archivo. */
    suspend fun download(ctx: Context, tone: StoreTone): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "store").apply { mkdirs() }
        val ext = tone.url.substringAfterLast('.', "audio").take(4)
        val f = File(dir, tone.id.replace(':', '_') + "." + ext)
        if (f.exists() && f.length() > 0) return@withContext f
        val c = URL(tone.url).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Zentones-Android")
        try {
            c.inputStream.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        } finally {
            c.disconnect()
        }
        f
    }
}
