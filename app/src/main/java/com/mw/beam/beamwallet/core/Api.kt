/*
 * // Copyright 2018 Beam Development
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //    http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 */

package com.mw.beam.beamwallet.core

import com.mw.beam.beamwallet.BuildConfig
import com.mw.beam.beamwallet.core.entities.OnSyncProgressData
import com.mw.beam.beamwallet.core.entities.Wallet
import io.reactivex.subjects.PublishSubject
import java.io.File
import android.content.Context.DOWNLOAD_SERVICE
import android.app.DownloadManager
import android.net.Uri
import android.content.Context
import com.mw.beam.beamwallet.core.utils.LogUtils
import android.os.Handler
import com.mw.beam.beamwallet.R

/**
 *  10/1/18.
 */


object Api {
    private var handler = Handler()
    private var isProgressCheckerRunning = false
    private var progressChecker:Runnable? = null
    private var downloadManager:DownloadManager? = null

    var downloadID: Long = 0

    val subDownloadProgress = PublishSubject.create<OnSyncProgressData>().toSerialized()

    init {
        System.loadLibrary("wallet-jni")
    }

    external fun createWallet(appVersion: String, nodeAddr: String, dbPath: String, pass: String, phrases: String, restore: Boolean = false): Wallet?
    external fun openWallet(appVersion: String, nodeAddr: String, dbPath: String, pass: String): Wallet?
    external fun isWalletInitialized(dbPath: String): Boolean
    external fun createMnemonic(): Array<String>
    external fun getDictionary(): Array<String>
    external fun checkReceiverAddress(address: String?): Boolean
    external fun closeWallet()
    external fun isWalletRunning(): Boolean
    external fun getDefaultPeers(): Array<String>

    fun download(file:File) {
        val link =  when (BuildConfig.FLAVOR) {
            AppConfig.FLAVOR_MAINNET -> "https://mobile-restore.beam.mw/mainnet/mainnet_recovery.bin"
            AppConfig.FLAVOR_TESTNET -> "https://mobile-restore.beam.mw/testnet/testnet_recovery.bin"
            else -> "https://mobile-restore.beam.mw/masternet/masternet_recovery.bin"
        }

        val request = DownloadManager.Request(Uri.parse(link))
                .setTitle(App.self.getString(R.string.app_name))
                .setDescription(App.self.getString(R.string.downloading_blockchain_info))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        downloadManager = App.self.getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
        downloadID = downloadManager!!.enqueue(request)

        startProgressChecker()
    }

    private  fun stopDownload() {
        downloadManager?.remove(downloadID)

        stopProgressChecker()
    }

    private fun startProgressChecker() {
        if (!isProgressCheckerRunning) {

            progressChecker = object:Runnable {
                override fun run() {
                    try {
                        checkProgress()
                    } finally {
                        handler.postDelayed(progressChecker, 300)
                    }
                }
            }
            progressChecker?.run()

            isProgressCheckerRunning = true
        }
    }


    private  fun stopProgressChecker() {
        if (isProgressCheckerRunning) {
            downloadID = 0

            handler.removeCallbacks(progressChecker)

            progressChecker = null

            isProgressCheckerRunning = false
        }
    }

    fun checkDownloadStatus() {
        val query = DownloadManager.Query()
        query.setFilterById(downloadID)

        val cursor = downloadManager?.query(query)
        if (cursor!=null) {
            if (!cursor.moveToFirst()) {
                cursor.close()

                stopProgressChecker()

                subDownloadProgress.onNext(OnSyncProgressData(-1, 100))

                return
            }
            do {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                val status = cursor.getInt(columnIndex)

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    stopProgressChecker()

                    subDownloadProgress.onNext(OnSyncProgressData(100, 100))
                }

            } while (cursor.moveToNext())
            cursor.close()
        }
    }

    private fun checkProgress() {
        val query = DownloadManager.Query()
        query.setFilterById(downloadID)

        val cursor = downloadManager?.query(query)

        if (cursor!=null) {

            if (!cursor.moveToFirst()) {

                cursor.close()

                return
            }
            do {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                val downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                val status = cursor.getInt(columnIndex)

                if (status == DownloadManager.STATUS_RUNNING) {
                    var progress = ((downloaded * 100L) / total)

                    if (progress>=100) {
                        progress = 99
                    }

                    subDownloadProgress.onNext(OnSyncProgressData(progress.toInt(), 100))
                }

            } while (cursor.moveToNext())
            cursor.close()
        }
    }
}
