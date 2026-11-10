package com.aether.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/** SAF operations. No raw storage paths or all-files permission required. */
object FileOps {
    fun list(folder: DocumentFile): List<DocumentFile> = folder.listFiles().sortedWith(
        compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase() ?: "" }
    )

    fun copy(context: Context, source: DocumentFile, targetDir: DocumentFile): DocumentFile {
        if (!targetDir.isDirectory || !targetDir.canWrite()) throw IOException("Destino sem permissão de escrita")
        val name = source.name ?: throw IOException("Nome inválido")
        if (source.isDirectory) {
            val dest = targetDir.createDirectory(uniqueName(targetDir, name))
                ?: throw IOException("Não foi possível criar pasta")
            source.listFiles().forEach { copy(context, it, dest) }
            return dest
        }
        val dest = targetDir.createFile(source.type ?: "application/octet-stream", uniqueName(targetDir, name))
            ?: throw IOException("Não foi possível criar arquivo")
        try {
            context.contentResolver.openInputStream(source.uri).use { input ->
                context.contentResolver.openOutputStream(dest.uri, "w").use { output ->
                    if (input == null || output == null) throw IOException("Falha ao abrir arquivo")
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            dest.delete()
            throw e
        }
        return dest
    }

    private fun uniqueName(folder: DocumentFile, name: String): String {
        if (folder.findFile(name) == null) return name
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (folder.findFile("$stem ($i)$ext") != null) i++
        return "$stem ($i)$ext"
    }

    fun isInside(context: Context, folder: DocumentFile, possibleAncestor: DocumentFile): Boolean {
        // Document providers can use opaque IDs; walk parent navigation in the UI instead.
        // Copying a folder into itself is explicitly blocked by URI equality.
        return folder.uri == possibleAncestor.uri
    }
}
