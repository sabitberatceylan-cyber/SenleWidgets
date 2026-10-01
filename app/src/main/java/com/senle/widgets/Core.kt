package com.senle.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.AlarmClock
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object P {
    private fun sp(c: Context) = c.getSharedPreferences("w", Context.MODE_PRIVATE)
    fun s(c: Context, id: Int, k: String, d: String): String = sp(c).getString("${id}_$k", d) ?: d
    fun b(c: Context, id: Int, k: String, d: Boolean) = sp(c).getBoolean("${id}_$k", d)
    fun put(c: Context, id: Int, k: String, v: Any) {
        val e = sp(c).edit()
        if (v is Boolean) e.putBoolean("${id}_$k", v) else e.putString("${id}_$k", v.toString())
        e.apply()
    }
    fun clear(c: Context, id: Int) {
        val e = sp(c).edit()
        sp(c).all.keys.filter { it.startsWith("${id}_") }.forEach { e.remove(it) }
        e.apply()
    }
}

fun col(s: String, d: Int): Int = try { Color.parseColor(s.trim()) } catch (e: Exception) { d }

object U {
    private fun bg(color: Int, cols: Int, rows: Int, header: Int? = null): Bitmap {
        val w = cols * 200; val h = rows * 200
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rad = minOf(w, h) * 0.16f
        val path = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rad, rad, Path.Direction.CW) }
        cv.clipPath(path)
        p.color = color; cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        if (header != null) { p.color = header; cv.drawRect(0f, 0f, w.toFloat(), h * 0.28f, p) }
        return bmp
    }

    private fun pi(c: Context, id: Int, i: Intent) =
        PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun update(c: Context, m: AppWidgetManager, id: Int, type: String, cols: Int, rows: Int, cls: Class<*>) {
        val rv = when (type) {
            "clock" -> clock(c, id, cols, rows)
            "weather" -> weather(c, id, cols, rows, cls)
            "link" -> link(c, id, cols, rows)
            else -> date(c, id)
        }
        m.updateAppWidget(id, rv)
    }

    private fun clock(c: Context, id: Int, cols: Int, rows: Int): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.w_clock)
        val tc = col(P.s(c, id, "text", "#FFFFFF"), Color.WHITE)
        rv.setImageViewBitmap(R.id.bg, bg(if (P.b(c, id, "transp", false)) Color.TRANSPARENT else col(P.s(c, id, "bg", "#CC1E1E2E"), Color.DKGRAY), cols, rows))
        val tf = if (P.b(c, id, "h24", true)) "HH:mm" else "h:mm"
        rv.setCharSequence(R.id.time, "setFormat24Hour", tf)
        rv.setCharSequence(R.id.time, "setFormat12Hour", tf)
        rv.setTextColor(R.id.time, tc)
        val df = if (P.b(c, id, "wd", true)) "d MMMM EEEE" else "d MMMM yyyy"
        val show = P.b(c, id, "showDate", true); val pos = P.s(c, id, "pos", "bottom")
        for (v in listOf(R.id.dateTop, R.id.dateBottom)) {
            rv.setCharSequence(v, "setFormat24Hour", df)
            rv.setCharSequence(v, "setFormat12Hour", df)
            rv.setTextColor(v, tc)
        }
        rv.setViewVisibility(R.id.dateTop, if (show && pos == "top") View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.dateBottom, if (show && pos != "top") View.VISIBLE else View.GONE)
        rv.setOnClickPendingIntent(R.id.root, pi(c, id, Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
        return rv
    }

    private fun emo(t: String) = when {
        t.contains("THUNDER") -> "⛈️"
        t.contains("SNOW") || t.contains("HAIL") -> "❄️"
        t.contains("RAIN") || t.contains("SHOWER") -> "🌧️"
        t.contains("CLOUDY") -> if (t.contains("PARTLY")) "⛅" else "☁️"
        t.contains("WIND") -> "💨"
        t.contains("CLEAR") -> "☀️"
        else -> "🌡️"
    }

    fun fetchWeather(c: Context) {
        val key = P.s(c, 0, "apikey", "")
        if (key.isEmpty()) { P.put(c, 0, "w_desc", "API anahtarı gir"); return }
        val lat = P.s(c, 0, "lat", "41.0082"); val lon = P.s(c, 0, "lon", "28.9784")
        val u = URL("https://weather.googleapis.com/v1/currentConditions:lookup?key=$key&location.latitude=$lat&location.longitude=$lon&languageCode=tr")
        val con = u.openConnection() as HttpURLConnection
        con.connectTimeout = 10000; con.readTimeout = 10000
        if (con.responseCode == 200) {
            val j = JSONObject(con.inputStream.bufferedReader().readText())
            val t = j.getJSONObject("temperature").getDouble("degrees")
            val wc = j.getJSONObject("weatherCondition")
            val d = wc.optJSONObject("description")?.optString("text") ?: ""
            P.put(c, 0, "w_temp", "${emo(wc.optString("type"))} ${Math.round(t)}°")
            P.put(c, 0, "w_desc", d)
        } else P.put(c, 0, "w_desc", "Hata ${con.responseCode}")
    }

    private fun weather(c: Context, id: Int, cols: Int, rows: Int, cls: Class<*>): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.w_weather)
        val tc = col(P.s(c, id, "text", "#FFFFFF"), Color.WHITE)
        rv.setImageViewBitmap(R.id.bg, bg(if (P.b(c, id, "transp", false)) Color.TRANSPARENT else col(P.s(c, id, "bg", "#CC1565C0"), Color.BLUE), cols, rows))
        rv.setTextViewText(R.id.temp, P.s(c, 0, "w_temp", "--°"))
        rv.setTextViewText(R.id.desc, P.s(c, 0, "w_desc", "Yükleniyor…"))
        rv.setTextColor(R.id.temp, tc); rv.setTextColor(R.id.desc, tc)
        val i = Intent(c, cls).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
        rv.setOnClickPendingIntent(R.id.root, PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        return rv
    }

    private fun link(c: Context, id: Int, cols: Int, rows: Int): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.w_link)
        val tc = col(P.s(c, id, "text", "#FFFFFF"), Color.WHITE)
        rv.setImageViewBitmap(R.id.bg, bg(if (P.b(c, id, "transp", false)) Color.TRANSPARENT else col(P.s(c, id, "bg", "#FF1565C0"), Color.BLUE), cols, rows))
        rv.setTextViewText(R.id.top, P.s(c, id, "top", "TIKLA"))
        val lb = P.s(c, id, "label", "")
        rv.setTextViewText(R.id.label, lb)
        rv.setViewVisibility(R.id.label, if (lb.isEmpty()) View.GONE else View.VISIBLE)
        rv.setTextColor(R.id.top, tc); rv.setTextColor(R.id.label, tc)
        var url = P.s(c, id, "url", "https://www.google.com")
        if (!url.startsWith("http")) url = "https://$url"
        rv.setOnClickPendingIntent(R.id.root, pi(c, id, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
        return rv
    }

    private fun date(c: Context, id: Int): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.w_date)
        val head = col(P.s(c, id, "head", "#E53935"), Color.RED)
        val body = col(P.s(c, id, "body", "#FFFFFF"), Color.WHITE)
        rv.setImageViewBitmap(R.id.bg, bg(body, 2, 2, head))
        rv.setTextColor(R.id.month, col(P.s(c, id, "headtext", "#FFFFFF"), Color.WHITE))
        val tc = col(P.s(c, id, "text", "#222222"), Color.BLACK)
        rv.setTextColor(R.id.day, tc); rv.setTextColor(R.id.weekday, tc)
        val launchIntent = c.packageManager.getLaunchIntentForPackage("com.samsung.android.calendar")
            ?: Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        rv.setOnClickPendingIntent(R.id.root, pi(c, id, launchIntent))
        return rv
    }
}
