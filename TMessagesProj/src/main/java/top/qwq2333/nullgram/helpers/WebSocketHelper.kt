package top.qwq2333.nullgram.helpers

import androidx.core.util.Pair
import org.tcp2ws.tcp2wsServer
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import top.qwq2333.gen.Config
import top.qwq2333.nullgram.config.ConfigManager
import top.qwq2333.nullgram.utils.Defines
import top.qwq2333.nullgram.utils.Log
import java.net.ServerSocket

object WebSocketHelper {
    const val proxyServer = "ck2ut7v3g5zudnjw.top#affine.pro"

    private var socksPort = -1
    private var tcp2wsStarted = false
    private var tcp2wsServer: tcp2wsServer? = null

//    private const val USER_AGENT = "Telegram ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    private const val CONN_HASH = "381d52f35f552e10ad1701445dba9cd14acb7e43"

    enum class WsProvider(val num: Int, var host: String, var userAgent: String) {
        PublicProxy(0, proxyServer, "Nullgram ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
        Custom(2, ConfigManager.getStringOrDefault(Defines.wsServerHost, "")!!, "Telegram ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
    }

    @JvmStatic
    var currentProvider = when (ConfigManager.getIntOrDefault(Defines.wsBuiltInProxyBackend, WsProvider.PublicProxy.num)) {
        WsProvider.PublicProxy.num -> WsProvider.PublicProxy
        WsProvider.Custom.num -> WsProvider.Custom
        else -> WsProvider.PublicProxy
    }
        set(value) {
            if (value == WsProvider.Custom) {
                value.host = Config.wsServerHost
            }
            ConfigManager.putInt(Defines.wsBuiltInProxyBackend, value.num)
            field = value
        }

    @JvmStatic
    fun getProviders(): Pair<ArrayList<String>, ArrayList<WsProvider>> {
        val names = ArrayList<String>()
        val types = ArrayList<WsProvider>()
        names.add("Nullgram")
        types.add(WsProvider.PublicProxy)
        names.add(LocaleController.getString("AutoDownloadCustom", R.string.AutoDownloadCustom))
        types.add(WsProvider.Custom)
        return Pair(names, types)
    }

    @JvmStatic
    @get:JvmName("wsEnableTLS")
    var wsEnableTLS: Boolean
        get() = Config.wsEnableTLS
        set(value) {
            Config.wsEnableTLS = value
        }

    @JvmStatic
    fun toggleWsEnableTLS() {
        Config.toggleWsEnableTLS()
    }

    @JvmStatic
    fun getSocksPort(): Int {
        return getSocksPort(6356)
    }

    @JvmStatic
    fun wsReloadConfig() {
        setSocksConfig()
        Log.d("ws reload config: ${currentProvider.host} tls: $wsEnableTLS")
    }

    fun setSocksConfig() {
        tcp2wsServer?.setServer(currentProvider.host)
            ?.setTls(wsEnableTLS)
            ?.setUserAgent((System.getProperty("http.agent") ?: "") + " " + currentProvider.userAgent)
            ?.setConnHash(CONN_HASH)
        Log.i("userAgent: ${System.getProperty("http.agent")} ${currentProvider.userAgent}")
    }

    fun getSocksPort(port: Int): Int {
        return if (tcp2wsStarted && socksPort != -1) {
            socksPort
        } else try {
            if (port != -1) {
                socksPort = port
            } else {
                val socket = ServerSocket(0)
                socksPort = socket.localPort
                socket.close()
            }
            if (!tcp2wsStarted) {
                tcp2wsServer = tcp2wsServer()
                setSocksConfig()
                tcp2wsServer!!.start(socksPort)
                tcp2wsStarted = true
            }
            Log.d("tcp2ws started on port $socksPort")
            Log.d("serverHost: ${currentProvider.host} tls: $wsEnableTLS")
            socksPort
        } catch (e: Exception) {
            Log.e(e)
            if (port != -1) {
                getSocksPort(-1)
            } else {
                -1
            }
        }
    }
}
