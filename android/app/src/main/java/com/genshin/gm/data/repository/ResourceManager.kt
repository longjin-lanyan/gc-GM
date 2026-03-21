package com.genshin.gm.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 资源热更管理器
 *
 * 流程：
 * 1. 每次进入APP时，从服务端拉取 GET /api/resource/version 获取 version.txt
 *    格式: 每行 "子目录/文件名:md5"，如 "txt/Item.txt:abc123"
 * 2. 客户端本地计算 data/txt/ 和 data/bg/ 下文件的 MD5
 * 3. 比对：如果服务端MD5 != 本地MD5，删除本地文件并从服务端下载
 * 4. 如果本地不存在该文件，也从服务端下载
 */
class ResourceManager(
    private val context: Context,
    private val baseUrl: String
) {
    private val txtDir = File(context.filesDir, "data/txt").also { it.mkdirs() }
    private val bgDir = File(context.filesDir, "data/bg").also { it.mkdirs() }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "GenshinGM-Android/1.0")
                .build())
        }
        .build()

    /**
     * 执行热更检查和同步
     * @return 更新的文件列表
     */
    suspend fun syncResources(): List<String> = withContext(Dispatchers.IO) {
        val updated = mutableListOf<String>()

        // 1. 从服务端获取 version.txt
        val serverVersions = fetchServerVersions() ?: return@withContext updated

        // 2. 计算本地MD5
        val localVersions = computeLocalMd5s()

        // 3. 对比并更新
        for ((filePath, serverMd5) in serverVersions) {
            val localMd5 = localVersions[filePath]

            if (localMd5 != serverMd5) {
                // MD5不同或本地不存在，删除旧文件并下载新文件
                try {
                    val localFile = getLocalFile(filePath)
                    if (localFile.exists()) {
                        localFile.delete()
                    }

                    val downloadUrl = "$baseUrl/api/resource/download/$filePath"
                    val bytes = downloadFile(downloadUrl)
                    if (bytes != null) {
                        localFile.parentFile?.mkdirs()
                        localFile.writeBytes(bytes)

                        // 验证下载后的MD5
                        val downloadedMd5 = computeMd5(localFile)
                        if (downloadedMd5 == serverMd5) {
                            updated.add(filePath)
                        } else {
                            localFile.delete() // MD5不匹配，删除
                        }
                    }
                } catch (e: Exception) {
                    // 跳过失败的文件，下次同步重试
                }
            }
        }

        // 4. 删除服务端已经移除的文件
        for (filePath in localVersions.keys) {
            if (!serverVersions.containsKey(filePath)) {
                getLocalFile(filePath).delete()
            }
        }

        updated
    }

    /**
     * 从服务端获取 version.txt 内容并解析
     * @return Map<文件路径, MD5> 如 {"txt/Item.txt": "abc123", "bg/bg1.jpg": "def456"}
     */
    private fun fetchServerVersions(): Map<String, String>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/resource/version")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            body.lines()
                .filter { it.contains(":") }
                .associate { line ->
                    val colonIndex = line.lastIndexOf(':')
                    val path = line.substring(0, colonIndex).trim()
                    val md5 = line.substring(colonIndex + 1).trim()
                    path to md5
                }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 计算本地 data/txt/ 和 data/bg/ 下所有文件的MD5
     */
    private fun computeLocalMd5s(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        scanDir(txtDir, "txt", result)
        scanDir(bgDir, "bg", result)

        return result
    }

    private fun scanDir(dir: File, prefix: String, result: MutableMap<String, String>) {
        dir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                computeMd5(file)?.let { md5 ->
                    result["$prefix/${file.name}"] = md5
                }
            }
        }
    }

    private fun getLocalFile(filePath: String): File {
        // filePath 格式: "txt/Item.txt" 或 "bg/bg1.jpg"
        return File(context.filesDir, "data/$filePath")
    }

    /**
     * 读取本地txt文件内容
     */
    fun readLocalFile(fileName: String): String? {
        val file = File(txtDir, fileName)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    /**
     * 获取本地bg文件
     */
    fun getBackgroundFile(fileName: String): File? {
        val file = File(bgDir, fileName)
        return if (file.exists()) file else null
    }

    /**
     * 解析游戏数据文件 (格式: ID:Name per line)
     */
    fun parseGameData(fileName: String): List<Pair<Int, String>> {
        val content = readLocalFile(fileName) ?: return emptyList()
        return content.lines()
            .filter { it.contains(":") }
            .mapNotNull { line ->
                val cleanLine = line.trimStart('\uFEFF').trim()
                val colonIndex = cleanLine.indexOf(':')
                if (colonIndex < 0) return@mapNotNull null
                val id = cleanLine.substring(0, colonIndex).trim().toIntOrNull()
                    ?: return@mapNotNull null
                val name = cleanLine.substring(colonIndex + 1).trim()
                id to name
            }
    }

    private fun downloadFile(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            response.body?.bytes()
        } catch (e: Exception) {
            null
        }
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
