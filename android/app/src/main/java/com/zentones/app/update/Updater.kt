package com.zentones.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.zentones.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Actualizaciones desde las Releases de GitHub del repo con el que se compiló la app. */
object Updater {
    data class Release(val version: String, val notes: String, val apkUrl: String?, val pageUrl: String, val isNewer: Boolean)

    val repo: String get() = BuildConfig.GITHUB_REPO
    val repoUrl: String get() = "https://github.com/$repo"

    /**
     * Cada push a main publica Zentones.apk en la release "android-latest".
     * Hay actualización si ese APK se subió bastante después de compilar el que tienes instalado.
     */
    suspend fun check(): Release = withContext(Dispatchers.IO) {
        val c = (URL("https://api.github.com/repos/$repo/releases/tags/android-latest").openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Zentones-Android")
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            when (c.responseCode) {
                200 -> Unit
                404 -> error("Todavía no hay ninguna APK publicada en $repo")
                else -> error("GitHub respondió ${c.responseCode}")
            }
            val j = JSONObject(c.inputStream.bufferedReader().readText())
            val assets = j.optJSONArray("assets")
            val apk = if (assets == null) null else (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
            val uploaded = apk?.optString("updated_at")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
            val label = if (uploaded > 0) DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("es", "ES"))
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(uploaded)) else "?"
            Release(
                label, j.optString("body"), apk?.getString("browser_download_url"),
                j.optString("html_url", repoUrl),
                uploaded > BuildConfig.BUILD_TIME + 15 * 60 * 1000,
            )
        } finally {
            c.disconnect()
        }
    }

    fun compare(a: String, b: String): Int {
        val x = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }

    suspend fun download(ctx: Context, url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "zentones-update.apk")
        val c = URL(url).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Zentones-Android")
        val total = c.contentLengthLong
        var lastPct = -1
        c.inputStream.use { inp ->
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var read = 0L
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    o.write(buf, 0, n)
                    read += n
                    if (total > 0) {
                        val pct = (read * 100 / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            withContext(Dispatchers.Main) { onProgress(pct / 100f) }
                        }
                    }
                }
            }
        }
        c.disconnect()
        out
    }

    /** Devuelve false si primero hay que dar permiso para instalar apps. */
    fun install(ctx: Context, f: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}
