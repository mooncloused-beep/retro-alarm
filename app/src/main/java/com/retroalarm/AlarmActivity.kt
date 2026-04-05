package com.retroalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.retroalarm.databinding.ActivityAlarmBinding
import kotlin.random.Random

class AlarmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmBinding
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var correctAnswer: Int = 0
    private var difficulty: String = "easy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        difficulty = intent.getStringExtra("difficulty") ?: "easy"
        val alarmTime = intent.getStringExtra("alarmTime") ?: "00:00"
        binding.tvAlarmTime.text = alarmTime

        generatePuzzle()
        startAlarmSound()
        startVibration()

        binding.btnDismiss.setOnClickListener {
            checkAnswer()
        }

        binding.btnSnooze.setOnClickListener {
            snoozeAlarm()
        }
    }

    private fun generatePuzzle() {
        val puzzle: Pair<String, Int>

        when (difficulty) {
            "easy" -> {
                val a = Random.nextInt(1, 10)
                val b = Random.nextInt(1, 10)
                when (Random.nextInt(3)) {
                    0 -> { puzzle = "$a + $b" to (a + b) }
                    1 -> { puzzle = "${a + b} - $a" to b }
                    else -> { puzzle = "$a × $b" to (a * b) }
                }
            }
            "medium" -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(1, 20)
                when (Random.nextInt(4)) {
                    0 -> { puzzle = "$a + $b" to (a + b) }
                    1 -> { puzzle = "$a - $b" to (a - b) }
                    2 -> { puzzle = "$a × ${b/2 + 1}" to (a * (b/2 + 1)) }
                    else -> { puzzle = "$a + $b + ${Random.nextInt(1,10)}" to (a + b + Random.nextInt(1,10)) }
                }
            }
            else -> {
                val a = Random.nextInt(10, 100)
                val b = Random.nextInt(5, 50)
                val c = Random.nextInt(1, 20)
                when (Random.nextInt(4)) {
                    0 -> { puzzle = "$a + $b - $c" to (a + b - c) }
                    1 -> { puzzle = "$a - $b + $c" to (a - b + c) }
                    2 -> { puzzle = "$a × ${b/10 + 1}" to (a * (b/10 + 1)) }
                    else -> { puzzle = "$a + $b" to (a + b) }
                }
            }
        }

        correctAnswer = puzzle.second
        binding.tvPuzzle.text = "= ${puzzle.first} ?"
    }

    private fun checkAnswer() {
        val answer = binding.etAnswer.text.toString().toIntOrNull()
        if (answer == correctAnswer) {
            stopAlarm()
            finish()
        } else {
            binding.tvHint.text = "WRONG! TRY AGAIN"
            binding.tvHint.setTextColor(getColor(R.color.retro_red))
            binding.etAnswer.text?.clear()
        }
    }

    private fun snoozeAlarm() {
        stopAlarm()
        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.retroalarm.TRIGGER_ALARM"
            putExtra("difficulty", difficulty)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozeTime,
            pendingIntent
        )
        finish()
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
