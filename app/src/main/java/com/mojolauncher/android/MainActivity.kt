package com.mojolauncher.android

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    private fun buildUi() { showMainMenu() }

    private fun showMainMenu() {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,24,28,24) }
        root.addView(TextView(this).apply { text="Mojolauncher-Android"; textSize=28f })
        root.addView(TextView(this).apply { text="Minecraft Java • mobil launcher"; textSize=14f })
        root.addView(Space(this), LinearLayout.LayoutParams(1,24))
        root.addView(Button(this).apply { text="▶ OYNA"; textSize=22f; setOnClickListener { startSelectedGame() } }, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(Button(this).apply { text="Sürümler"; setOnClickListener { showVersions() } })
        root.addView(Button(this).apply { text="Ayarlar"; setOnClickListener { showSettings() } })
        setContentView(root)
    }

    private fun showVersions() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,24,28,24) }
        root.addView(TextView(this).apply { text="Sürümler"; textSize=26f })
        versionsBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(versionsBox) }, LinearLayout.LayoutParams(-1,0,1f))
        val row=LinearLayout(this)
        row.addView(Button(this).apply { text="JAR Ekle"; setOnClickListener { pickJar.launch(arrayOf("application/java-archive","application/octet-stream","*/*")) } }, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(Button(this).apply { text="Ana Menü"; setOnClickListener { showMainMenu() } }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(row); setContentView(root); renderVersions()
    }

    private fun scanVersions() {
        val base = File(filesDir, "Minecraft/versions").apply { mkdirs() }
        versions.clear()
        base.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
            val jar = File(folder, "Minecraft_" + folder.name + ".jar")
            if (jar.isFile) versions += GameVersion(folder.name, jar)
        }
        versions.sortByDescending { it.id }
        if (::versionsBox.isInitialized) renderVersions()
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
                text = "Minecraft " + version.id
                setOnClickListener { selectVersion(version) }
            })
        }
    }

    private fun importJar(uri: Uri) {
        val rawName = (uri.lastPathSegment ?: "Minecraft_custom.jar").substringAfterLast('/')
        val id = Regex("Minecraft_(.+)\\.jar", RegexOption.IGNORE_CASE).find(rawName)?.groupValues?.get(1)
            ?: Regex("([0-9]+(?:\\.[0-9]+){1,3})").find(rawName)?.value
            ?: "custom"
        val folder = File(filesDir, "Minecraft/versions/" + id).apply { mkdirs() }
        val output = File(folder, "Minecraft_" + id + ".jar")
        contentResolver.openInputStream(uri)?.use { input ->
            output.outputStream().use { outputStream -> input.copyTo(outputStream) }
        }
        prefs.edit().putString("lastVersion", id).apply()
        scanVersions()
        Toast.makeText(this, "Eklendi: Minecraft_" + id + ".jar", Toast.LENGTH_LONG).show()
    }

    private fun selectVersion(version: GameVersion) {
        prefs.edit().putString("lastVersion", version.id).apply()
        Toast.makeText(this, "Seçildi: Minecraft_" + version.id + ".jar", Toast.LENGTH_SHORT).show()
    }

    private fun startSelectedGame() {
        val id=prefs.getString("lastVersion",null)
        if(id==null){ AlertDialog.Builder(this).setTitle("Sürüm seçilmedi").setMessage("Önce Sürümler bölümünden JAR ekleyip seç.").setPositiveButton("Sürümler"){_,_->showVersions()}.setNegativeButton("İptal",null).show(); return }
        val jar=File(filesDir,"Minecraft/versions/"+id+"/Minecraft_"+id+".jar")
        if(!jar.isFile){Toast.makeText(this,"JAR bulunamadı.",Toast.LENGTH_LONG).show();return}
        requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        showGameSettings(id)
    }

    private fun showGameSettings(id:String) {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,24)}
        root.addView(TextView(this).apply{text="Minecraft "+id+" • Oyun Ayarları";textSize=24f})
        root.addView(Button(this).apply{text="▶ Oyuna Geç";setOnClickListener{showTouchGameShell(id)}})
        root.addView(Button(this).apply{text="Kontrolleri Ayarla";setOnClickListener{showControlsSettings()}})
        root.addView(Button(this).apply{text="Dokunmatik";setOnClickListener{showTouchSettings()}})
        root.addView(Button(this).apply{text="Performans";setOnClickListener{showSettings()}})
        root.addView(Button(this).apply{text="Ana Menü";setOnClickListener{showMainMenu()}})
        setContentView(root)
    }

    private fun showControlsSettings(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val sens=EditText(this).apply{hint="Kamera hassasiyeti";setText(prefs.getString("sensitivity","1.0"))}
        val joy=EditText(this).apply{hint="Joystick boyutu";setText(prefs.getString("joystick","1.0"))}
        val sprint=CheckBox(this).apply{text="Otomatik sprint";isChecked=prefs.getBoolean("autoSprint",false)}
        box.addView(sens);box.addView(joy);box.addView(sprint)
        AlertDialog.Builder(this).setTitle("Kontroller").setView(box).setPositiveButton("Kaydet"){_,_->prefs.edit().putString("sensitivity",sens.text.toString()).putString("joystick",joy.text.toString()).putBoolean("autoSprint",sprint.isChecked).apply()}.setNegativeButton("İptal",null).show()
    }

    private fun showTouchSettings(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val left=CheckBox(this).apply{text="Solak mod";isChecked=prefs.getBoolean("leftHanded",false)}
        val tap=CheckBox(this).apply{text="Dokunarak kır";isChecked=prefs.getBoolean("tapBreak",true)}
        val quick=CheckBox(this).apply{text="Hızlı eşya kullan";isChecked=prefs.getBoolean("quickUse",true)}
        box.addView(left);box.addView(tap);box.addView(quick)
        AlertDialog.Builder(this).setTitle("Dokunmatik").setView(box).setPositiveButton("Kaydet"){_,_->prefs.edit().putBoolean("leftHanded",left.isChecked).putBoolean("tapBreak",tap.isChecked).putBoolean("quickUse",quick.isChecked).apply()}.setNegativeButton("İptal",null).show()
    }

    private fun showTouchGameShell(id:String){
        requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val frame=FrameLayout(this)
        frame.addView(TextView(this).apply{text="Minecraft "+id;textSize=22f;setPadding(24,18,0,0)})
        frame.addView(TextView(this).apply{text="🕹";textSize=52f},FrameLayout.LayoutParams(180,180,android.view.Gravity.BOTTOM or android.view.Gravity.START).apply{setMargins(24,0,0,24)})
        val names=arrayOf("↑","⌄","L","R","E")
        for(i in names.indices) frame.addView(Button(this).apply{text=names[i]},FrameLayout.LayoutParams(110,80,android.view.Gravity.BOTTOM or android.view.Gravity.END).apply{setMargins(0,0,40+i*125,40)})
        setContentView(frame)
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