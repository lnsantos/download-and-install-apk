package com.example.downloadapkandinstall

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object AppDownloadManager {

        private const val FILE_NAME = "shell_pagamento.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""

    class Builder{

        private var ctx: Context? = null
        private var linkDownload: String? = null

        fun setContext(context: Context) : Builder = apply {
            ctx = context
        }

        fun setURLDownload(url: String) : Builder = apply {
            linkDownload = url
        }

        fun enqueueDownload(context: Context, url: String, onFinishedDownload: (Int) -> Unit ) {

            val destination = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()}/$FILE_NAME"
            val uri = Uri.parse("$FILE_BASE_PATH$destination")
            val file = File(destination)
            if (file.exists()) file.delete()

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val downloadUri = Uri.parse(url)
            val request = DownloadManager.Request(downloadUri)
            request.setMimeType(MIME_TYPE)
            request.setTitle("Atualizando")
            request.setDescription("Baixando aplicativo")

            request.setDestinationUri(uri)
            showInstallOption(destination, uri, context)

            val downloadId = downloadManager.enqueue(request)

            Thread{
                try {
                    var downloading = true

                    while (downloading) {
                        val q = DownloadManager.Query()
                        q.setFilterById(downloadId)

                        val cursor: Cursor = downloadManager.query(q)
                        var bytesDownloaded: Float = 0f
                        var bytesTotal: Float = 0f
                        var downloadStatus: Int = 0
                        var currentState = 0

                        if (cursor.moveToFirst()){
                            bytesDownloaded = cursor.getFloat(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            bytesTotal = cursor.getFloat(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            currentState = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                            Log.i("DOWNLOAD_MANAGER", "bytesDownloaded $bytesDownloaded")
                            Log.i("DOWNLOAD_MANAGER", "bytesTotal $bytesTotal")
                        }

                        cursor.close()

                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.i("DOWNLOAD_MANAGER", "Progress finished")
                            downloading = false
                        }

                        val progress = if(bytesDownloaded < 0) 0 else (bytesDownloaded * 100F) / bytesTotal

                        Log.i("DOWNLOAD_MANAGER", "Progress ${progress.toInt()} :" + statusMessage(currentState).toString())
                        onFinishedDownload(progress.toInt())
                    }

                } catch (e : Exception){
                    Log.i("DOWNLOAD_MANAGER", e.toString())
                }
            }.start()

        }

        private fun statusMessage(state: Int): String? {
            var msg = ""
            msg = when (state) {
                    DownloadManager.STATUS_FAILED -> "Download failed!"
                    DownloadManager.STATUS_PAUSED -> "Download paused!"
                    DownloadManager.STATUS_PENDING -> "Download pending!"
                    DownloadManager.STATUS_RUNNING -> "Download in progress!"
                    DownloadManager.STATUS_SUCCESSFUL -> "Download complete!"
                    else -> "Download is nowhere in sight"
                }
            return msg
        }

        private fun showInstallOption(
            destination: String,
            uri: Uri,
            context: Context
        ) {

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                            File(destination)
                        )
                        val install = Intent(Intent.ACTION_VIEW)
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        install.data = contentUri
                        context.startActivity(install)
                        context.unregisterReceiver(this)
                    } else {
                        val install = Intent(Intent.ACTION_VIEW)
                        install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        install.setDataAndType(
                            uri,
                            APP_INSTALL_PATH
                        )
                        context.startActivity(install)
                        context.unregisterReceiver(this)
                    }
                }
            }

            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

    }

}