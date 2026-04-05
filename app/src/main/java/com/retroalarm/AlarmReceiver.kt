package com.retroalarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.retroalarm.TRIGGER_ALARM") {
            val difficulty = intent.getStringExtra("difficulty") ?: "easy"

            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("difficulty", difficulty)
            }
            context.startActivity(alarmIntent)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("RetroAlarmPrefs", Context.MODE_PRIVATE)
            val isAlarmSet = prefs.getBoolean("alarmSet", false)
            val alarmTime = prefs.getString("alarmTime", "") ?: ""
            val difficulty = prefs.getString("difficulty", "easy") ?: "easy"

            if (isAlarmSet && alarmTime.isNotEmpty()) {
                val parts = alarmTime.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()

                    val calendar = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(java.util.Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context, 0,
                        Intent(context, AlarmReceiver::class.java).apply {
                            action = "com.retroalarm.TRIGGER_ALARM"
                            putExtra("difficulty", difficulty)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    }
}
