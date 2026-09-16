package com.nexauren.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFFF7F8FC.toInt())
        }

        val title = TextView(this).apply {
            text = getString(R.string.setup_title)
            textSize = 30f
            setTextColor(0xFF171A24.toInt())
        }

        val subtitle = TextView(this).apply {
            text = getString(R.string.setup_subtitle)
            textSize = 16f
            setTextColor(0xFF606575.toInt())
            setPadding(0, 16, 0, 28)
        }

        val enable = Button(this).apply {
            text = getString(R.string.enable_keyboard)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        val testTitle = TextView(this).apply {
            text = getString(R.string.test_title)
            textSize = 18f
            setTextColor(0xFF171A24.toInt())
            setPadding(0, 40, 0, 12)
        }

        val test = EditText(this).apply {
            hint = getString(R.string.test_hint)
            minHeight = 120
            gravity = Gravity.TOP
            setPadding(20, 18, 20, 18)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(enable, LinearLayout.LayoutParams(-1, -2))
        root.addView(testTitle)
        root.addView(test, LinearLayout.LayoutParams(-1, 140))

        setContentView(root)
    }
}
