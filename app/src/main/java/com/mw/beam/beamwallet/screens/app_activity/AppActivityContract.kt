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

package com.mw.beam.beamwallet.screens.app_activity

import com.mw.beam.beamwallet.base_screen.MvpPresenter
import com.mw.beam.beamwallet.base_screen.MvpRepository
import com.mw.beam.beamwallet.base_screen.MvpView

interface AppActivityContract {
    interface View: MvpView {
        fun showOpenFragment()
        fun showWalletFragment()
        fun showTransactionDetailsFragment(txId: String)
        fun startNewSnackbar(assetId:Int, onUndo: () -> Unit, onDismiss: () -> Unit)
    }

    interface Presenter: MvpPresenter<View> {
        fun onNewIntent(txId: String?)
        fun onPendingSend(info: PendingSendInfo)
    }

    interface Repository: MvpRepository {
        fun sendMoney(outgoingAddress: String, token: String, comment: String?, amount: Long, fee: Long, saveAddress: Boolean, assetId:Int)
        fun cancelSendMoney(token: String)
    }
}