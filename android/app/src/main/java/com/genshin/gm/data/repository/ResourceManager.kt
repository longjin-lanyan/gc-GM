package com.genshin.gm.data.repository

import android.content.Context
import com.genshin.gm.data.proto.ProtoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Manages local data file caching with MD5 verification against server.
 * Files are stored in app's internal files/data directory.
 */
class ResourceManager(
    private val context: Context,
    private val protoClient: ProtoClient
) {
    private val dataDir = File(context.filesDir, "data").also { it.mkdirs() }
    private val bgDir = File(dataDir, "bg").also { it.mkdirs() }

    /**
     * Check all local files against server and download updates.
     * Returns list of updated file names.
     */
    suspend fun syncResources(): List<String> = withContext(Dispatchers.IO) {
        val localFiles = getLocalFileMd5s()
        val checkResult = protoClient.checkResources(localFiles)
        val updated = mutableListOf<String>()

        for (update in checkResult.updatesList) {
            try {
                val bytes = protoClient.downloadResource(update.downloadUrl)
                val targetFile = if (update.fileName.startsWith("bg/")) {
                    File(bgDir, update.fileName.removePrefix("bg/"))
                } else {
                    File(dataDir, update.fileName)
                }
                targetFile.writeBytes(bytes)

                // Verify downloaded file MD5
                val downloadedMd5 = computeMd5(targetFile)
                if (downloadedMd5 == update.serverMd5) {
                    updated.add(update.fileName)
                } else {
                    targetFile.delete()
                }
            } catch (e: Exception) {
                // Skip failed downloads, will retry next sync
            }
        }
        updated
    }

    fun getLocalFile(fileName: String): File? {
        val file = if (fileName.startsWith("bg/")) {
            File(bgDir, fileName.removePrefix("bg/"))
        } else {
            File(dataDir, fileName)
        }
        return if (file.exists()) file else null
    }

    fun readLocalFile(fileName: String): String? {
        return getLocalFile(fileName)?.readText(Charsets.UTF_8)
    }

    /**
     * Parse game data from a local text file (format: ID:Name per line)
     */
    fun parseGameData(fileName: String): List<Pair<Int, String>> {
        val content = readLocalFile(fileName) ?: return emptyList()
        return content.lines()
            .filter { it.contains(":") }
            .mapNotNull { line ->
                val cleanLine = line.trimStart('\uFEFF').trim()
                val colonIndex = cleanLine.indexOf(':')
                if (colonIndex < 0) return@mapNotNull null
                val id = cleanLine.substring(0, colonIndex).trim().toIntOrNull() ?: return@mapNotNull null
                val name = cleanLine.substring(colonIndex + 1).trim()
                id to name
            }
    }

    private fun getLocalFileMd5s(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()

        dataDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                computeMd5(file)?.let { md5 ->
                    result.add(file.name to md5)
                }
            }
        }

        bgDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                computeMd5(file)?.let { md5 ->
                    result.add("bg/${file.name}" to md5)
                }
            }
        }

        return result
    }

    private fun computeMd5(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
