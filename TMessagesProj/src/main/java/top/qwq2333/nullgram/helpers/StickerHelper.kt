
package top.qwq2333.nullgram.helpers

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.InputStickerSet
import org.telegram.ui.ActionBar.AlertDialog
import top.qwq2333.nullgram.utils.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StickerHelper {
    private enum class Error {
        NoError,
        IncompleteDownload,
        StickerPackNotFound,
        UnknownError
    }

    @JvmStatic
    fun saveStickerSet(context: Context, stickerSet: InputStickerSet) = CoroutineScope(Dispatchers.Main).launch {
        val connectionsHelper = ConnectionsHelper.getInstance(UserConfig.selectedAccount)
        val fileLoader = FileLoader.getInstance(UserConfig.selectedAccount)

        val progressDialog = AlertDialog(context, AlertDialog.ALERT_TYPE_LOADING).apply {
            setCancelable(false)
            setTitle(LocaleController.getString("DownloadingStickerSet", R.string.DownloadingStickerSet))
        }

        val job = CoroutineScope(Dispatchers.IO).async {
            val req = TLRPC.TL_messages_getStickerSet().apply {
                stickerset = stickerSet
            }
            val stickerSet = connectionsHelper.sendRequestAndDo(req) { response, error ->
                if (error != null) {
                    return@sendRequestAndDo null
                }
                if (response is TLRPC.TL_messages_stickerSet) {
                    return@sendRequestAndDo response
                }
                return@sendRequestAndDo null
            }

            if (stickerSet == null || stickerSet.documents.isEmpty()) {
                return@async Pair(null, Error.StickerPackNotFound)
            }

            var result = true
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
            val zipFile = File(AndroidUtilities.getCacheDir(), "StickerPack-${stickerSet.set.short_name}-$date.zip")
            val outputStream = ZipOutputStream(FileOutputStream(zipFile))
            outputStream.use { zip ->
                for (i in 0 until stickerSet.documents.size) {
                    val document = stickerSet.documents[i]
                    var file: File = fileLoader.getPathToAttach(document)
                    if (!file.exists()) {
                        fileLoader.loadFile(document, "sticker", 0, 0)
                        var maxWait = 0
                        while (!file.exists()) {
                            maxWait++
                            if (maxWait > 100) {
                                // 100 * 100ms = 10s
                                result = false
                                break
                            }
                            delay(100)
                        }
                        file = fileLoader.getPathToAttach(document)
                    }
                    val fileName = "${document.id}." + if (document.mime_type == "application/x-tgsticker") {
                        // application/x-tgsticker is not a standard mime type, so we specify it as
                        "tgs"
                    } else {
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(document.mime_type)
                    }
                    zip.putNextEntry(ZipEntry(fileName))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                    withContext(Dispatchers.Main) {
                        progressDialog.setProgress((((i + 1).toDouble() / stickerSet.documents.size) * 100).toInt())
                    }
                }
            }

            Log.d("StickerHelper", "saveStickerSet: ${zipFile.absolutePath}")

            if (!result) {
                Log.d("StickerHelper", "saveStickerSet: failed to download sticker set")
                return@async Pair(null, Error.IncompleteDownload)
            }

            return@async Pair(zipFile, Error.NoError)
        }

        progressDialog.apply {
            setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel)) { _, _ ->
                job.cancel()
                dismiss()
            }
            show()
        }

        val (file, error) = job.await()
        progressDialog.dismiss()

        if (file != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.also {
                context.startActivity(it)
            }
        } else {
            AlertDialog(context, AlertDialog.ALERT_TYPE_MESSAGE).apply {
                when(error) {
                    Error.IncompleteDownload -> {
                        setMessage(LocaleController.getString("IncompleteDownload", R.string.IncompleteDownload))
                    }
                    Error.StickerPackNotFound -> {
                        setMessage(LocaleController.getString("StickerPackNotFound", R.string.StickerPackNotFound))
                    }
                    else -> {
                        setMessage(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred))
                    }
                }
                setPositiveButton(LocaleController.getString("OK", R.string.OK), null)
                show()
            }
        }
    }
}
