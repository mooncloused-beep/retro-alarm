package com.retroalarm

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.retroalarm.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val NOTIFICATION_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()
        setupTimePickers()
        setupUI()
        updateTime()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun setupTimePickers() {
        binding.npHour.minValue = 0
        binding.npHour.maxValue = 23
        binding.npHour.wrapSelectorWheel = true

        binding.npMinute.minValue = 0
        binding.npMinute.maxValue = 59
        binding.npMinute.wrapSelectorWheel = true

        val calendar = Calendar.getInstance()
        binding.npHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        binding.npMinute.value = calendar.get(Calendar.MINUTE)
    }

    private fun setupUI() {
        binding.btnSetAlarm.setOnClickListener {
            setAlarm()
        }
    }

    private fun setAlarm() {
        val hour = binding.npHour.value
        val minute = binding.npMinute.value
        val difficulty = when (binding.rgDifficulty.checkedRadioButtonId) {
            R.id.rbEasy -> "easy"
            R.id.rbMedium -> "medium"
            R.id.rbHard -> "hard"
            else -> "easy"
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmTimeStr = String.format("%02d:%02d", hour, minute)

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.retroalarm.TRIGGER_ALARM"
            putExtra("difficulty", difficulty)
            putExtra("alarmTime", alarmTimeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        val prefs = getSharedPreferences("RetroAlarmPrefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("alarmSet", true)
            .putString("alarmTime", alarmTimeStr)
            .putString("difficulty", difficulty)
            .apply()

        binding.tvStatus.text = "STATUS: SET FOR $alarmTimeStr [$difficulty.uppercase()]"
        binding.tvStatus.setTextColor(getColor(R.color.retro_amber))

        Toast.makeText(this, "ALARM SET FOR $alarmTimeStr", Toast.LENGTH_SHORT).show()
    }

    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        binding.tvCurrentTime.text = String.format("%02d:%02d", hour, minute)

        binding.tvCurrentTime.postDelayed({
            updateTime()
        }, 1000)
    }
}
