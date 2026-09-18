package com.mojolauncher.android

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

data class GameVersion(val id: String, val jar: File)

class MainActivity : AppCompatActivity() {
    private lateinit var versionsBox: LinearLayout
    private val versions = mutableListOf<GameVersion>()
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private val pickJar = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importJar(it) }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildUi()
        scanVersions()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }
        root.addView(TextView(this).apply { text = "Mojolauncher-Android"; textSize = 28f })
        root.addView(TextView(this).apply {
            text = "Minecraft Java • sürüm / profil / performans"
            textSize = 14f
        })
        versionsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(versionsBox, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this)
        row.addView(Button(this).apply {
            text = "JAR Ekle"
            setOnClickListener { pickJar.launch(arrayOf("application/java-archive", "application/octet-stream", "*/*")) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "Ayarlar"
            setOnClickListener { showSettings() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        setContentView(root)
    }

    private fun scanVersions() {
        val base = File(filesDir, "Minecraft/versions").apply { mkdirs() }
        versions.clear()
        base.listFiles()?.filter(File::isDirectory)?.forEach { dir ->
            val jar = File(dir, "Minecraft_\${dir.name}.jar")
            if (jar.isFile) versions += GameVersion(dir.name, jar)
        }
        versions.sortByDescending(GameVersion::id)
        renderVersions()
    }

    private fun renderVersions() {
        versionsBox.removeAllViews()
        if (versions.isEmpty()) {
            versionsBox.addView(TextView(this).apply {
                text = "Sürüm yok. Minecraft_<sürüm>.jar ekle."
                textSize = 16f
            })
            return
        }
        versions.forEach { version ->
            versionsBox.addView(Button(this).apply {
                text = "Minecraft \${version.id}"
                setOnClickListener { selectVersion(version) }
            })
        }
    }

    private fun importJar(uri: Uri) {
        val rawName = (uri.lastPathSegment ?: "Minecraft_custom.jar").substringAfterLast('/')
        val id = Regex("Minecraft_(.+)\\.jar", RegexOption.IGNORE_CASE).find(rawName)?.groupValues?.get(1)
            ?: Regex("([0-9]+(?:\\.[0-9]+){1,3})").find(rawName)?.value
            ?: "custom"
        val dir = File(filesDir, "Minecraft/versions/\$id").apply { mkdirs() }
        val output = File(dir, "Minecraft_\$id.jar")
        contentResolver.openInputStream(uri)?.use { input ->
            output.outputStream().use { outputStream -> input.copyTo(outputStream) }
        }
        prefs.edit().putString("lastVersion", id).apply()
        scanVersions()
        Toast.makeText(this, "Eklendi: Minecraft_\$id.jar", Toast.LENGTH_LONG).show()
    }

    private fun selectVersion(version: GameVersion) {
        prefs.edit().putString("lastVersion", version.id).apply()
        Toast.makeText(this, "Seçildi: Minecraft_\${version.id}.jar", Toast.LENGTH_SHORT).show()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 10, 30, 10)
        }
        fun field(label: String, key: String, def: String) {
            box.addView(EditText(this).apply {
                hint = label
                setText(prefs.getString(key, def))
                tag = key
            })
        }
        field("RAM MB", "ram", "2048")
        field("FPS", "fps", "60")
        field("Çözünürlük %", "scale", "100")
        field("JVM argümanları", "jvmArgs", "-Xms512m -Xmx2048m")
        AlertDialog.Builder(this).setTitle("Performans").setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                for (i in 0 until box.childCount) {
                    val child = box.getChildAt(i)
                    if (child is EditText) prefs.edit().putString(child.tag as String, child.text.toString()).apply()
                }
            }.setNegativeButton("İptal", null).show()
    }
}
