package com.nexauren.keyboard

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val prefs by lazy {
        getSharedPreferences("nexa_settings", Context.MODE_PRIVATE)
    }
    private var statusText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
        if (prefs.getBoolean("auto_updates", true)) {
            checkForUpdates(silent = true)
        }
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFFF7F8FC.toInt())
        }

        val title = titleText("Nexa Keyboard")
        val subtitle = bodyText("Um teclado rápido, simples e feito para evoluir.")
        val enable = Button(this).apply {
            text = "Ativar teclado"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
        val settings = Button(this).apply {
            text = "⚙  Definições"
            setOnClickListener { showSettings() }
        }
        val testTitle = titleText("Testar teclado", 18f)
        testTitle.setPadding(0, 28, 0, 10)
        val test = EditText(this).apply {
            hint = "Escreve aqui para testar…"
            minHeight = 120
            gravity = Gravity.TOP
            setPadding(20, 18, 20, 18)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(enable, fullWidth())
        root.addView(settings, fullWidth())
        root.addView(testTitle)
        root.addView(test, LinearLayout.LayoutParams(-1, 140))
        setContentView(root)
    }

    private fun showSettings() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFFF7F8FC.toInt())
        }

        root.addView(titleText("Definições", 28f))
        root.addView(bodyText("Personaliza o Nexa Keyboard e mantém a aplicação atualizada."))

        val auto = Switch(this).apply {
            text = "Verificar atualizações automaticamente"
            textSize = 16f
            isChecked = prefs.getBoolean("auto_updates", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("auto_updates", checked).apply()
            }
            setPadding(0, 24, 0, 20)
        }
        root.addView(auto)

        val update = Button(this).apply {
            text = "Verificar atualizações agora"
            setOnClickListener { checkForUpdates(silent = false) }
        }
        root.addView(update, fullWidth())

        statusText = bodyText("Versão instalada: ${BuildConfig.VERSION_NAME}")
        statusText?.setPadding(0, 20, 0, 0)
        root.addView(statusText)

        val privacy = bodyText(
            "Privacidade\nO teclado não envia o que escreves para servidores. " +
                "As atualizações são verificadas através do GitHub."
        )
        privacy.setPadding(0, 32, 0, 0)
        root.addView(privacy)

        val back = Button(this).apply {
            text = "Voltar"
            setOnClickListener { showHome() }
        }
        back.setPadding(0, 20, 0, 0)
        root.addView(back, fullWidth())

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun checkForUpdates(silent: Boolean) {
        if (!silent) statusText?.text = "A verificar atualizações…"

        thread {
            try {
                val connection = URL(
                    "https://api.github.com/repos/nexauren1/Keyboard-Nexa-/releases/latest"
                ).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 7000
                connection.readTimeout = 7000

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val tag = Regex("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                    .find(json)?.groupValues?.get(1)
                val assetUrl = Regex(
                    "\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]+\\\")"
                ).find(json)?.groupValues?.get(1)

                if (tag == null || assetUrl == null) {
                    runOnUiThread {
                        if (!silent) statusText?.text = "Não foi possível verificar a atualização."
                    }
                    return@thread
                }

                val latest = tag.removePrefix("v").split(".")
                    .mapNotNull { it.toIntOrNull() }
                val current = BuildConfig.VERSION_NAME.split(".")
                    .mapNotNull { it.toIntOrNull() }
                val newer = compareVersions(latest, current) > 0

                runOnUiThread {
                    if (newer) {
                        AlertDialog.Builder(this)
                            .setTitle("Nova atualização disponível")
                            .setMessage("Nexa Keyboard $tag está disponível. Queres atualizar agora?")
                            .setNegativeButton("Depois", null)
                            .setPositiveButton("Atualizar") { _, _ -> downloadUpdate(assetUrl, tag) }
                            .show()
                    } else if (!silent) {
                        statusText?.text = "Estás na versão mais recente: ${BuildConfig.VERSION_NAME}"
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!silent) statusText?.text = "Sem ligação ou sem atualização disponível."
                }
            }
        }
    }

    private fun downloadUpdate(url: String, tag: String) {
        try {
            if (!packageManager.canRequestPackageInstalls()) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                })
                return
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Nexa Keyboard $tag")
                .setDescription("A transferir atualização…")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalFilesDir(
                    this,
                    Environment.DIRECTORY_DOWNLOADS,
                    "nexa-keyboard-$tag.apk"
                )
                .setMimeType("application/vnd.android.package-archive")

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            statusText?.text = "Atualização a transferir…"

            thread {
                var finished = false
                while (!finished) {
                    Thread.sleep(700)
                    val query = DownloadManager.Query().setFilterById(id)
                    manager.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val state = cursor.getInt(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                            )
                            if (state == DownloadManager.STATUS_SUCCESSFUL) {
                                val file = File(
                                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                    "nexa-keyboard-$tag.apk"
                                )
                                finished = true
                                runOnUiThread { installApk(file) }
                            } else if (state == DownloadManager.STATUS_FAILED) {
                                finished = true
                                runOnUiThread {
                                    statusText?.text = "Não foi possível transferir a atualização."
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            statusText?.text = "Não foi possível iniciar a atualização."
        }
    }

    private fun installApk(file: File) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    private fun titleText(text: String, size: Float = 30f) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(0xFF171A24.toInt())
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun bodyText(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(0xFF606575.toInt())
    }

    private fun fullWidth() = LinearLayout.LayoutParams(-1, -2).apply {
        setMargins(0, 8, 0, 8)
    }
}
