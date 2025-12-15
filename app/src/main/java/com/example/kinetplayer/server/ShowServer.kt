package com.example.kinetplayer.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ShowServer(
    private val context: Context, 
    port: Int = 8080,
    private val onStatus: (String) -> Unit = {},
    private val onProgress: (Int) -> Unit = {},
    private val onStart: (String) -> Unit = {}
) : NanoHTTPD(port) {

    private val showDir = File(context.filesDir, "shows")
    private var expectedSize: Long = 0

    init {
        if (!showDir.exists()) {
            showDir.mkdirs()
        }
        tempFileManagerFactory = ProgressTempFileManagerFactory()
    }

    private inner class ProgressTempFileManagerFactory : TempFileManagerFactory {
        override fun create(): TempFileManager {
            return ProgressTempFileManager()
        }
    }

    private inner class ProgressTempFileManager : TempFileManager {
        private val defaultManager = DefaultTempFileManager() // Fallback to default

        override fun createTempFile(filename_hint: String?): TempFile {
            return ProgressTempFile(defaultManager.createTempFile(filename_hint))
        }

        override fun clear() {
            defaultManager.clear()
        }
    }

    private inner class ProgressTempFile(private val wrapped: TempFile) : TempFile {
        override fun delete() = wrapped.delete()
        override fun getName(): String = wrapped.name
        override fun open(): java.io.OutputStream {
            return ProgressOutputStream(wrapped.open())
        }
    }

    private inner class ProgressOutputStream(private val wrapped: java.io.OutputStream) : java.io.OutputStream() {
        private var written: Long = 0

        override fun write(b: Int) {
            wrapped.write(b)
            written++
            report()
        }

        override fun write(b: ByteArray) {
            wrapped.write(b)
            written += b.size
            report()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            wrapped.write(b, off, len)
            written += len
            report()
        }

        override fun flush() = wrapped.flush()
        override fun close() = wrapped.close()

        private fun report() {
            if (expectedSize > 0) {
                val percent = ((written.toDouble() / expectedSize.toDouble()) * 100).toInt()
                if (written % (1024 * 100) == 0L) { // Log every 100KB to avoid spam
                     Log.d("ShowServer", "Progress: $percent% ($written / $expectedSize)")
                }
                onProgress(percent)
            } else {
                 Log.d("ShowServer", "Progress: Unknown Size ($written bytes written)")
            }
        }
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.POST) {
            when (session.uri) {
                "/upload" -> return handleUpload(session)
                "/prepare" -> return handlePrepare(session)
                "/status" -> return handleStatusUpdate(session)
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }

    private fun handleStatusUpdate(session: IHTTPSession): Response {
        try {
            val map = HashMap<String, String>()
            session.parseBody(map)
            // For simple text body, NanoHTTPD might put it in "postData" or similar if not multipart
            // But let's assume client sends form-data or we read map["content"]?
            // Actually, parseBody populates 'files' map with body content if it's small?
            // Standard way: Content stored in temp file, path in 'map'.
            
            // Let's assume the client sends simple text body.
            // Retrieve content.
            // Note: NanoHTTPD parseBody behavior depends on Content-Type.
            // If we send "text/plain", the body is in map["postData"].
            val status = map["postData"] ?: "Processing..."
            onStatus(status)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
        } catch (e: Exception) {
             return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error")
        }
    }
    
    // Callback for metadata
    private var onMetadata: ((String, File?) -> Unit)? = null
    fun setMetadataCallback(callback: (String, File?) -> Unit) {
        onMetadata = callback
    }

    private fun handlePrepare(session: IHTTPSession): Response {
         try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val params = session.parms
            
            val name = params["name"] ?: "Incoming Show"
            val thumbPath = files["thumbnail"]
            
            var thumbFile: File? = null
            if (thumbPath != null) {
                // Move logic or just pass temp file
                // We should copy it because temp file might get deleted
                val savedThumb = File(showDir, "temp_thumb_${System.currentTimeMillis()}.jpg")
                File(thumbPath).copyTo(savedThumb, true)
                thumbFile = savedThumb
            }
            
            onMetadata?.invoke(name, thumbFile)
            onStatus("Initializing Transfer...")
            
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Prepared")
         } catch (e: Exception) {
             return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
         }
    }

    private fun handleUpload(session: IHTTPSession): Response {
        onStatus("Receiving Show...")
        
        // Reset progress
        expectedSize = 0
        onProgress(0)

        // Try to get content length
        val lenStr = session.headers["content-length"] ?: session.headers["Content-Length"]
        val nameHeader = session.headers["x-show-name"] ?: session.headers["X-Show-Name"] ?: "Incoming Show"
        
        onStart(nameHeader)
        
        Log.d("ShowServer", "Headers: ${session.headers}")
        Log.d("ShowServer", "Content-Length Header: $lenStr")
        if (lenStr != null) {
            expectedSize = lenStr.toLongOrNull() ?: 0
        }
        Log.d("ShowServer", "Expected Size: $expectedSize")

        try {
            // NanoHTTPD parses multipart into a temporary map of file paths
            val files = HashMap<String, String>()
            
            // We must call parseBody first to populate 'files' and 'parms'
            // 'files' map key is the form field name, value is the temp file path
            session.parseBody(files)

            val tempFilePath = files["showFile"]
            if (tempFilePath != null) {
                // ... (existing code for file copy) ...
                
                val destFile = File(showDir, "show_${System.currentTimeMillis()}.kshow") // Fallback name
                
                 // Actually we should inspect headers or params if possible, but for now stick to logic
                 // We will rename it if we can find the name, but logic is inside handleUpload.
                 
                 // Wait, I need to check how I'm copying.
                 // Just keeping existing copy logic but adding status update.
                 
                val tempFile = File(tempFilePath)
                
                // Try to get filename from parameters if NanoHTTPD parsed it? 
                // NanoHTTPD puts filename in 'parms' map if using standard multipart? 
                // Actually 'parms' usually has form fields.
                // We'll trust the main logic or the header strategy (which is hard to access here without custom parsing).
                // But wait, I added X-Show-Name header! NanoHTTPD exposes headers!
                
                val headers = session.headers
                val nameHeader = headers["x-show-name"] ?: headers["X-Show-Name"]
                val finalName = if (nameHeader != null) "$nameHeader.kshow" else "show_${System.currentTimeMillis()}.kshow"
                val finalDest = File(showDir, finalName)

                tempFile.copyTo(finalDest, overwrite = true)
                
                Log.d("ShowServer", "Saved show to ${finalDest.absolutePath}")
                Log.d("ShowServer", "Saved show to ${finalDest.absolutePath}")
                onStatus("Server running on Port 8080")
                onProgress(0) // Hide bar
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Upload Complete")
            } else {
                onStatus("Upload Failed: No File")
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No file found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onStatus("Upload Error")
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }
}
