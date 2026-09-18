package com.mojolauncher.android

import java.io.File
import java.util.concurrent.Executors

class MinecraftProcess(
    private val root: File,
    private val runtime: File,
    private val versionJar: File,
    private val versionId: String,
    private val ramMb: Int,
    private val jvmArgs: List<String>,
    private val onOutput: (String) -> Unit,
    private val onExit: (Int) -> Unit
) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var process: Process? = null

    fun start() {
        executor.execute {
            try {
                val java = File(runtime, "bin/java")
                require(java.isFile) { "Java runtime: bin/java bulunamadı" }
                java.setExecutable(true)
                val libraries = File(root, "libraries")
                val jars = if (libraries.isDirectory) libraries.walkTopDown().filter { it.isFile && it.extension.equals("jar", true) }.toList() else emptyList()
                val classpath = (listOf(versionJar) + jars).joinToString(File.pathSeparator) { it.absolutePath }
                val gameDir = File(root, "game").apply { mkdirs() }
                val command = mutableListOf<String>()
                command += java.absolutePath
                command += "-Xms512m"
                command += "-Xmx" + ramMb.coerceAtLeast(512) + "m"
                command += jvmArgs
                command += "-Djava.library.path=" + File(root, "natives").absolutePath
                command += "-cp"; command += classpath
                command += "net.minecraft.client.main.Main"
                command += "--version"; command += versionId
                command += "--gameDir"; command += gameDir.absolutePath
                command += "--assetsDir"; command += File(root, "assets").absolutePath
                command += "--assetIndex"; command += versionId
                command += "--username"; command += "Player"
                command += "--accessToken"; command += "0"
                command += "--userType"; command += "msa"
                val pb = ProcessBuilder(command).directory(gameDir).redirectErrorStream(true)
                pb.environment()["HOME"] = root.absolutePath
                pb.environment()["TMPDIR"] = File(root, "tmp").apply { mkdirs() }.absolutePath
                val p = pb.start()
                process = p
                p.inputStream.bufferedReader().useLines { lines -> lines.forEach { onOutput(it) } }
                onExit(p.waitFor())
            } catch (e: Exception) {
                onOutput("Launcher error: " + (e.message ?: e.javaClass.simpleName))
                onExit(-1)
            }
        }
    }

    fun stop() { process?.destroy() }
}