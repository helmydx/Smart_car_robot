@file:Suppress("SpellCheckingInspection")
package com.example.robotikm1lkt

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL

object RobotManager {
    private const val TAG = "RobotManager"
    private const val ROBOT_IP = "192.168.4.1"
    private const val BASE_URL = "http://$ROBOT_IP"

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var connectionJob: Job? = null
    private var speedJob: Job? = null
    
    private var lastCommand = "S"
    private var lastSpeedIndex = -1

    // Starts background pinging loop to update connection status
    fun startMonitoring(scope: CoroutineScope) {
        if (connectionJob != null) return // Already running
        connectionJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val success = checkConnection()
                _isConnected.value = success
                delay(2000) // Ping every 2 seconds
            }
        }
    }

    private fun checkConnection(): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL/")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            connection.responseCode == 200
        } catch (e: Exception) {
            Log.d(TAG, "Connection ping failed: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }

    // Sends movements or actions. We run it immediately in IO thread.
    fun sendCommand(cmd: String) {
        if (cmd == lastCommand) return
        lastCommand = cmd
        Log.d(TAG, "Sending command: $cmd")
        
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$BASE_URL/?State=$cmd")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 800
                connection.readTimeout = 800
                val code = connection.responseCode
                Log.d(TAG, "Command response code: $code")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command $cmd: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }

    // Debounced speed sender to prevent crashing the ESP8266 during slider drag
    fun sendSpeedIndex(index: Int, immediate: Boolean = false) {
        if (index == lastSpeedIndex) return

        speedJob?.cancel()
        
        if (immediate) {
            lastSpeedIndex = index
            executeSpeedRequest(index)
        } else {
            // Setup a 120ms debounce
            speedJob = CoroutineScope(Dispatchers.IO).launch {
                delay(120)
                lastSpeedIndex = index
                executeSpeedRequest(index)
            }
        }
    }

    private fun executeSpeedRequest(index: Int) {
        Log.d(TAG, "Sending speed command: State=$index")
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/?State=$index")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 800
            connection.readTimeout = 800
            val code = connection.responseCode
            Log.d(TAG, "Speed response code: $code")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send speed command: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }
}
