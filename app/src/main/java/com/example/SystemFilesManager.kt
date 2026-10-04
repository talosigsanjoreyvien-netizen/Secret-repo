package com.example

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class SystemFileItem(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String
)

object SystemFilesManager {
    private const val TAG = "SystemFilesManager"
    private const val TEMP_FOLDER_NAME = "uniblox_system_files"

    fun getTempDir(context: Context): File {
        val tempDir = File(context.cacheDir, TEMP_FOLDER_NAME)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    fun ensureExtracted(context: Context): File {
        val targetDir = getTempDir(context)
        try {
            // Copy all files from assets/system_files if not already extracted or empty
            val marker = File(targetDir, ".extracted_marker")
            if (!marker.exists() || targetDir.listFiles().isNullOrEmpty()) {
                Log.d(TAG, "Extracting system files to temporary directory: ${targetDir.absolutePath}")
                copyAssetsRecursively(context.assets, "system_files", targetDir)
                marker.writeText("extracted_version_1.0\n${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting system files from assets", e)
        }
        return targetDir
    }

    private fun copyAssetsRecursively(assetManager: AssetManager, assetPath: String, targetDir: File) {
        val list = assetManager.list(assetPath) ?: return
        if (list.isEmpty()) {
            // Single file
            targetDir.parentFile?.mkdirs()
            try {
                assetManager.open(assetPath).use { input ->
                    FileOutputStream(targetDir).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not open asset $assetPath: ${e.message}")
            }
        } else {
            // Directory
            targetDir.mkdirs()
            for (child in list) {
                val childAssetPath = if (assetPath.isEmpty()) child else "$assetPath/$child"
                val childTarget = File(targetDir, child)
                val subList = assetManager.list(childAssetPath)
                if (subList != null && subList.isNotEmpty()) {
                    copyAssetsRecursively(assetManager, childAssetPath, childTarget)
                } else {
                    try {
                        assetManager.open(childAssetPath).use { input ->
                            childTarget.parentFile?.mkdirs()
                            FileOutputStream(childTarget).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        childTarget.mkdirs()
                    }
                }
            }
        }
    }

    fun listFiles(context: Context, subPath: String = ""): List<SystemFileItem> {
        val baseDir = getTempDir(context)
        val targetDir = if (subPath.isBlank()) baseDir else File(baseDir, subPath)
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        val files = targetDir.listFiles() ?: return emptyList()
        return files
            .filter { !it.name.startsWith(".") }
            .map { file ->
                val rel = file.relativeTo(baseDir).path.replace("\\", "/")
                SystemFileItem(
                    name = file.name,
                    relativePath = rel,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) (file.listFiles()?.size?.toLong() ?: 0L) else file.length(),
                    lastModified = file.lastModified(),
                    extension = file.extension.lowercase(Locale.ROOT)
                )
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
    }

    fun readFile(context: Context, relativePath: String): String? {
        val baseDir = getTempDir(context)
        val file = File(baseDir, relativePath)
        return if (file.exists() && file.isFile) {
            try {
                file.readText()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read $relativePath", e)
                null
            }
        } else {
            null
        }
    }

    fun writeFile(context: Context, relativePath: String, content: String): Boolean {
        val baseDir = getTempDir(context)
        val file = File(baseDir, relativePath)
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $relativePath", e)
            false
        }
    }

    fun createNewFile(context: Context, subPath: String, name: String, content: String): File? {
        val baseDir = getTempDir(context)
        val folder = if (subPath.isBlank()) baseDir else File(baseDir, subPath)
        folder.mkdirs()
        val file = File(folder, name)
        return try {
            file.writeText(content)
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create file $name", e)
            null
        }
    }

    fun deleteFile(context: Context, relativePath: String): Boolean {
        val baseDir = getTempDir(context)
        val file = File(baseDir, relativePath)
        return try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun executeScript(
        context: Context,
        relativePath: String,
        onAction: (actionType: String, param: String) -> Unit = { _, _ -> }
    ): String {
        val content = readFile(context, relativePath) ?: return "Error: File not found at $relativePath"
        val lowerRel = relativePath.lowercase(Locale.ROOT)

        val sb = StringBuilder()
        sb.appendLine("Executing temporary script: $relativePath")
        sb.appendLine("----------------------------------------")

        when {
            lowerRel.endsWith("cmd.uea") -> {
                sb.appendLine("[CMD.UEA] Application: Terminal")
                sb.appendLine("[CMD.UEA] Local Build: 8080 | Build: 2441")
                sb.appendLine("[CMD.UEA] Triggering function_run cmd() -> Launching Terminal")
                onAction("open_terminal", "")
            }
            lowerRel.endsWith("uniblox.uea") -> {
                sb.appendLine("[UNIBLOX.UEA] Type: game-engine / webview")
                sb.appendLine("[UNIBLOX.UEA] Loading: https://uniblox-fun.lovable.app/pocket")
                onAction("open_pocket", "https://uniblox-fun.lovable.app/pocket")
            }
            lowerRel.endsWith("start_menu.uea") -> {
                sb.appendLine("[START_MENU.UEA] Type: system/jpml")
                sb.appendLine("[START_MENU.UEA] Broadcast received: start_menu -> Toggling Menu")
                onAction("toggle_start_menu", "")
            }
            lowerRel.endsWith("taskbar.uea") -> {
                sb.appendLine("[TASKBAR.UEA] Type: system/jpml")
                sb.appendLine("[TASKBAR.UEA] Creating Taskbar drawer component and buttons...")
                sb.appendLine("[TASKBAR.UEA] Successfully bound start_menu_button & search_bar_btn.")
            }
            lowerRel.endsWith("hal.uea") -> {
                sb.appendLine("[HAL.UEA] UNIBLOX OS 7.0 Hardware Abstraction Layer")
                sb.appendLine("[HAL.UEA] Boot attempt cmd.run: uniblox start ./d fun.uniblox.system.boot [OK]")
                sb.appendLine("[HAL.UEA] Bios check: fun.uniblox.bios [OK]")
                sb.appendLine("[HAL.UEA] Status: System core online. Error escape handlers active.")
            }
            lowerRel.endsWith("api_read_write.upk") -> {
                sb.appendLine("[API_READ_WRITE.UPK] Parsing Uniblox Package Enclave...")
                sb.appendLine("[API_READ_WRITE.UPK] Token Read: xzxxx-7272726666")
                sb.appendLine("[API_READ_WRITE.UPK] Token Write: xzxxx-6225516666")
                sb.appendLine("[API_READ_WRITE.UPK] Export status: default package active.")
            }
            lowerRel.endsWith("cpp.compile.java") -> {
                sb.appendLine("[CASM COMPILER] Invoking g++ compiler wrapper...")
                sb.appendLine("[CASM COMPILER] Checking include path: /usr/include/uniblox...")
                sb.appendLine("[CASM COMPILER] Success! Compiled target created: build/uniblox_core.bin")
            }
            else -> {
                sb.appendLine("Executed script successfully. Lines: ${content.lines().size}")
            }
        }
        return sb.toString()
    }
}
