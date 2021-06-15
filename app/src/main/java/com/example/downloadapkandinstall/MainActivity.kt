package com.example.downloadapkandinstall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress.visibility = View.VISIBLE
        progress.max = 100
        progress.progress = 0

        buttonPanel.setOnClickListener {
            checkStoragePermission()
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkStoragePermission() {

        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            progress.visibility = View.VISIBLE
            progress.progress = 0

            val urlAPK = "LINK-APK"
            AppDownloadManager.Builder()
                .enqueueDownload(applicationContext, urlAPK){
                    Thread{
                        runOnUiThread {
                            Log.i("NOTIFY", it.toString())
                            onUpdateView(it)
                        }
                    }.start()
                }

        } else {
            requestStoragePermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun onUpdateView(it: Int){
        progress.progress = it
        description.text = it.toString()
    }

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            layout.showSnackbar(
                "Você gostaria de atualizar o aplicativo ?",
                Snackbar.LENGTH_INDEFINITE, "Atualizar aplicativo"
            ) {
                requestPermissionsCompat(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE
                )
            }
        } else {
            requestPermissionsCompat(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_STORAGE
            )
        }
    }

}