package com.senle.widgets

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(TextView(this).apply {
            setPadding(48, 96, 48, 48); textSize = 18f
            text = "Senle Widgets\n\nAna ekranda boş bir yere uzun bas → Widget'lar → Senle Widgets.\n\n" +
                "• Saat, Hava Durumu, Link: 2x1, 2x2, 3x1, 3x2\n• Tarih Takvim: 2x2\n\n" +
                "Ayarlar için widget'a uzun bas → Ayarla."
        })
    }
}
