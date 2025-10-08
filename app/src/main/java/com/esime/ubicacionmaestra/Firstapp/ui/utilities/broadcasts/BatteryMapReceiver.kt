package com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.esime.ubicacionmaestra.Firstapp.ui.saveLocation.SaveUbicacionReal

class BatteryMapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        if (level != -1 && scale != -1) {
            val batteryPct = level * 100 / scale.toFloat()

            // Cambiar al tipo de mapa NONE si la batería está por debajo del 20%
            if (batteryPct <= 20) {
                (context as? SaveUbicacionReal)?.apply {
                    changeMapToNone()
                }
            }
            // Volver al tipo de mapa NORMAL si la batería está por encima del 20%
            else {
                (context as? SaveUbicacionReal)?.apply {
                }
            }
        }
    }
}
