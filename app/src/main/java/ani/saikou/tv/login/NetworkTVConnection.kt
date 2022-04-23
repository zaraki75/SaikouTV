package ani.saikou.tv.login

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import ani.saikou.toastString
import java.io.*
import java.math.BigInteger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

//TODO use SSL sockets
object NetworkTVConnection {

    val PORT = 2413

    private var _isListening = false
    val isListening: Boolean get() = _isListening

    private var listeningThread: Thread? = null

    fun listen(onTokenReceivedCallback: OnTokenReceivedCallback) {
        var server: ServerSocket? = null
        listeningThread = thread(start = true) {
            try {
                _isListening = true
                server = ServerSocket(PORT)
                val socket = server?.accept()
                val streamReader = BufferedReader(InputStreamReader(socket?.getInputStream()))

                val readToken= streamReader.readLine()

                onTokenReceivedCallback?.onTokenReceived(readToken)
                server?.close()
                socket?.close()
            } catch (e: Exception) {
                server?.let {
                    it.close()
                }
                onTokenReceivedCallback?.onTokenReceived(null)
                _isListening = false
            }
        }
    }

    fun connect(context: Context, anilistToken: String, hostIP: String, fillSubnet: Boolean, onTokenSentCallback: OnTokenSentCallback) {
        val ip = if (fillSubnet) getIpFromHost(context, hostIP) else hostIP
        try {
            val socket = Socket(ip, PORT)
            val streamWriter = PrintWriter(socket.getOutputStream(), true)

            streamWriter.println(anilistToken)
            onTokenSentCallback.onTokenSent(true)
            socket.close()
        } catch (e: Exception) {
            onTokenSentCallback.onTokenSent(false)
        }
    }

    fun stopListening() {
        _isListening = false
        listeningThread?.let {
            it.interrupt()
        }
    }

    fun getLocalIPHost(context: Context): String? = getLocalIP(context)?.split(".")?.last()

    fun getIpFromHost(context: Context, host: String): String? {
        getLocalIPHost(context)?.let {
            return getLocalIP(context)?.replace(it, host)
        }
        return null
    }

    private fun getLocalIP(context: Context): String? {
        //TODO fix this for recent android versions
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            if (connectivityManager is ConnectivityManager) {
                var link: LinkProperties =
                        connectivityManager.getLinkProperties(connectivityManager.activeNetwork) as LinkProperties
                return link.linkAddresses.toString()
            } else {
                return null
            }
        } else {*/
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val longIp = wm.connectionInfo.ipAddress.toLong()
            val byteIp = BigInteger.valueOf(longIp).toByteArray().reversedArray()
            return InetAddress.getByAddress(byteIp).hostAddress
        //}
    }

    interface OnTokenReceivedCallback {
        fun onTokenReceived(token: String?)
    }
    interface OnTokenSentCallback {
        fun onTokenSent(sent: Boolean)
    }
}