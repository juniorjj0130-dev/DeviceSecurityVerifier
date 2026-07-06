package com.labredteam.devicesecurityverifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("LabRAT", "BootReceiver: Recebido evento $action")
        
        // No Android, o AccessibilityService é reiniciado automaticamente pelo sistema
        // se estivesse ativado antes do reboot. 
        // Este receptor serve para garantir que o processo do app seja acordado
        // e possamos executar tarefas adicionais se necessário.
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            Log.i("LabRAT", "Dispositivo reiniciado. O serviço de acessibilidade deve subir em breve.")
        }
    }
}
