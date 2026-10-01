package com.senle.widgets

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.*

class ConfigActivity : Activity() {
    private var id = 0
    private var etLat: EditText? = null
    private var etLon: EditText? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setResult(RESULT_CANCELED)
        id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0) ?: 0
        if (id == 0) { finish(); return }
        val cn = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className
        if (cn == null) { finish(); return }
        val type = when {
            cn.contains("Clock") -> "clock"; cn.contains("Weather") -> "weather"
            cn.contains("Link") -> "link"; else -> "date"
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        setContentView(ScrollView(this).apply { addView(root) })
        val saves = ArrayList<() -> Unit>()

        fun label(t: String) { root.addView(TextView(this).apply { text = t; setPadding(0, 24, 0, 4) }) }
        fun ed(l: String, k: String, def: String, g: Boolean = false): EditText {
            label(l)
            val e = EditText(this); e.setSingleLine(); e.setText(P.s(this, if (g) 0 else id, k, def))
            root.addView(e)
            saves.add { P.put(this, if (g) 0 else id, k, e.text.toString().trim()) }
            return e
        }
        fun cb(l: String, k: String, def: Boolean) {
            val c = CheckBox(this); c.text = l; c.isChecked = P.b(this, id, k, def)
            root.addView(c)
            saves.add { P.put(this, id, k, c.isChecked) }
        }

        when (type) {
            "clock" -> {
                cb("Tarihi göster", "showDate", true)
                label("Tarih konumu")
                val rg = RadioGroup(this)
                val top = RadioButton(this).apply { text = "Saatin üstünde"; this.id = 1001 }
                val bot = RadioButton(this).apply { text = "Saatin altında"; this.id = 1002 }
                rg.addView(top); rg.addView(bot); root.addView(rg)
                if (P.s(this, id, "pos", "bottom") == "top") top.isChecked = true else bot.isChecked = true
                saves.add { P.put(this, id, "pos", if (top.isChecked) "top" else "bottom") }
                cb("Gün adını göster", "wd", true)
                cb("24 saat formatı", "h24", true)
                ed("Yazı rengi (#RRGGBB)", "text", "#FFFFFF")
                cb("Şeffaf arka plan", "transp", false)
                ed("Arka plan rengi (#AARRGGBB)", "bg", "#CC1E1E2E")
            }
            "weather" -> {
                ed("Google Weather API anahtarı", "apikey", "", true)
                etLat = ed("Enlem", "lat", "41.0082", true)
                etLon = ed("Boylam", "lon", "28.9784", true)
                root.addView(Button(this).apply {
                    text = "Konumumu kullan"
                    setOnClickListener {
                        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) fillLoc()
                        else requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
                    }
                })
                ed("Yazı rengi", "text", "#FFFFFF")
                cb("Şeffaf arka plan", "transp", false)
                ed("Arka plan rengi", "bg", "#CC1565C0")
            }
            "link" -> {
                ed("Web sitesi adresi", "url", "https://www.google.com")
                ed("Büyük yazı (widget üzerinde görünen)", "top", "TIKLA")
                ed("Alt yazı (boş bırakılabilir)", "label", "")
                ed("Yazı rengi", "text", "#FFFFFF")
                cb("Şeffaf arka plan", "transp", false)
                ed("Arka plan rengi", "bg", "#FF1565C0")
            }
            else -> {
                ed("Üst şerit rengi", "head", "#E53935")
                ed("Üst şerit yazı rengi", "headtext", "#FFFFFF")
                ed("Gövde rengi", "body", "#FFFFFF")
                ed("Gün/yazı rengi", "text", "#222222")
            }
        }
        root.addView(Button(this).apply {
            text = "Kaydet"
            setOnClickListener {
                saves.forEach { it() }
                sendBroadcast(Intent(this@ConfigActivity, Class.forName(cn))
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id)))
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                finish()
            }
        })
    }

    private fun fillLoc() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val l = lm.getProviders(true).mapNotNull {
            try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null }
        }.maxByOrNull { it.time }
        if (l != null) { etLat?.setText(l.latitude.toString()); etLon?.setText(l.longitude.toString()) }
        else Toast.makeText(this, "Konum bulunamadı, elle gir", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        if (r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) fillLoc()
    }
}
