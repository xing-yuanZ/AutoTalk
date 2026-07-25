package com.autotalk.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 网络状态监听，用于语音识别自动选择端侧/云端。 */
class NetworkMonitor(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isWifi = MutableStateFlow(false)
    val isWifi: StateFlow<Boolean> = _isWifi

    /** 简易刷新：读取当前活动网络能力（Android 7+ 可用，注册回调需在生命周期内管理，这里按需轮询即可）。 */
    fun refresh() {
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        _isConnected.value = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isWifi.value = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
