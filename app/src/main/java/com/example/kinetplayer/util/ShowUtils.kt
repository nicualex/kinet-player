package com.example.kinetplayer.util

import android.util.Log
import com.example.kinetplayer.model.ShowManifest
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ShowUtils {

    fun extractAndParse(kshowFile: File, outputDir: File): ShowManifest? {
        try {
            if (!outputDir.exists()) outputDir.mkdirs()

            // Unzip
            val buffer = ByteArray(1024)
            val zis = ZipInputStream(FileInputStream(kshowFile))
            var zipEntry: ZipEntry? = zis.nextEntry

            while (zipEntry != null) {
                val newFile = File(outputDir, zipEntry.name)
                // Prevent Zip Slip
                if (!newFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
                    throw SecurityException("Zip Slip detected")
                }

                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    File(newFile.parent).mkdirs()
                    val fos = FileOutputStream(newFile)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
            zis.close()

            // Parse Manifest
            val manifestFile = File(outputDir, "manifest.json")
            if (manifestFile.exists()) {
                val gson = Gson()
                val reader = InputStreamReader(FileInputStream(manifestFile))
                val manifest = gson.fromJson(reader, ShowManifest::class.java)
                reader.close()
                return manifest
            }

        } catch (e: Exception) {
            Log.e("ShowUtils", "Error extracting show", e)
        }
        return null
    }
}
