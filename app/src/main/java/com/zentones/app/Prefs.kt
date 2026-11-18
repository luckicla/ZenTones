package com.zentones.app

import android.content.Context
import org.json.JSONObject

/** Tonos de una app concreta. null = no tocar (suena el de la app). */
data class AppRule(val call: String?, val msg: String?) {
    val isEmpty get() = call == null && msg == null
}

/** Estilo que corresponde a la marca del móvil. */
fun detectSkin(): String {
    val brand = (android.os.Build.MANUFACTURER + " " + android.os.Build.BRAND).lowercase()
    return when {
        listOf("xiaomi", "redmi", "poco").any { it in brand } -> "hyper"
        "samsung" in brand -> "oneui"
        "sony" in brand -> "sony"
        else -> "pixel"
    }
}

fun brandName(): String = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("zentones", Context.MODE_PRIVATE)

    /** "" = automático según la marca del móvil */
    var skin: String
        get() = sp.getString("skin", "") ?: ""
        set(v) = sp.edit().putString("skin", v).apply()

    /** 0 sistema, 1 claro, 2 oscuro */
    var darkMode: Int
        get() = sp.getInt("dark", 0)
        set(v) = sp.edit().putInt("dark", v).apply()

    var autoUpdate: Boolean
        get() = sp.getBoolean("autoUpdate", true)
        set(v) = sp.edit().putBoolean("autoUpdate", v).apply()

    var freesoundKey: String
        get() = sp.getString("fsKey", "") ?: ""
        set(v) = sp.edit().putString("fsKey", v.trim()).apply()

    /** Último tono general aplicado por tipo: "id|uri" */
    fun general(type: Int): Pair<String, String>? =
        sp.getString("gen_$type", null)?.split("|", limit = 2)?.takeIf { it.size == 2 }?.let { it[0] to it[1] }

    fun setGeneral(type: Int, id: String, uri: String) = sp.edit().putString("gen_$type", "$id|$uri").apply()

    fun rules(): Map<String, AppRule> = runCatching {
        val o = JSONObject(sp.getString("rules", "{}") ?: "{}")
        o.keys().asSequence().associateWith { k ->
            val r = o.getJSONObject(k)
            AppRule(r.optString("call").ifEmpty { null }, r.optString("msg").ifEmpty { null })
        }
    }.getOrDefault(emptyMap())

    fun setRule(pkg: String, rule: AppRule?) {
        val o = runCatching { JSONObject(sp.getString("rules", "{}") ?: "{}") }.getOrDefault(JSONObject())
        if (rule == null || rule.isEmpty) o.remove(pkg)
        else o.put(pkg, JSONObject().put("call", rule.call ?: "").put("msg", rule.msg ?: ""))
        sp.edit().putString("rules", o.toString()).apply()
    }
}
