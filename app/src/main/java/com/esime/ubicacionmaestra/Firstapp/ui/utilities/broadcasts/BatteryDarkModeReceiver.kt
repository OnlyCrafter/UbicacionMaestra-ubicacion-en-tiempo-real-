package com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatDelegate

class BatteryDarkModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val isUserSetDarkMode = sharedPreferences.getBoolean("user_set_dark_mode", false)

        if (!isUserSetDarkMode) {  // Solo cambiar si el usuario no lo ha configurado manualmente
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            if (level != -1 && scale != -1) {
                val batteryPct = level * 100 / scale.toFloat()

                // Cambiar al modo oscuro si la batería está por debajo del 20%
                if (batteryPct <= 20) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                // Cambiar al modo claro si la batería está por encima del 20%
                else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }
}

