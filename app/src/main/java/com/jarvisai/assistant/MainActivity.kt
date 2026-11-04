package com.jarvisai.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var output: TextView

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    companion object {
        private const val SPEECH_REQUEST = 100
        private const val MIC_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(25, 35, 25, 25)
            setBackgroundColor(0xFF07111F.toInt())
        }

        val title = TextView(this).apply {
            text = "J A R V I S"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(0xFF55DFFF.toInt())
        }

        status = TextView(this).apply {
            text = "JARVIS is starting..."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(10, 25, 10, 25)
        }

        input = EditText(this).apply {
            hint = "Enter your command..."
            setSingleLine(true)
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFAAAAAA.toInt())
            setPadding(15, 12, 15, 12)
            setBackgroundColor(0xFF1A2B40.toInt())
        }

        val runButton = Button(this).apply {
            text = "RUN COMMAND"
            setOnClickListener {
                runCommand(input.text.toString())
            }
        }

        val voiceButton = Button(this).apply {
            text = "🎙 SPEAK TO JARVIS"
            setOnClickListener {
                startListening()
            }
        }

        output = TextView(this).apply {
            text = """
                AVAILABLE COMMANDS

                • Hello Jarvis
                • What time is it?
                • Open YouTube
                • Open Chrome
                • Open Settings
                • Jarvis shutdown
            """.trimIndent()

            textSize = 16f
            setTextColor(0xFFB8DFFF.toInt())
            setPadding(10, 25, 10, 10)
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(input, matchParams())
        layout.addView(runButton, matchParams())
        layout.addView(voiceButton, matchParams())
        layout.addView(output)

        setContentView(layout)

        status.text = "JARVIS ONLINE"
    }

    private fun matchParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 12
        }
    }

    private fun startListening() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MIC_PERMISSION
            )
            return
        }

        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak to JARVIS"
                )
            }

            startActivityForResult(intent, SPEECH_REQUEST)

        } catch (e: Exception) {
            status.text = "Voice recognition unavailable"
        }
    }

    @Deprecated("Compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST &&
            resultCode == RESULT_OK) {

            val words = data
                ?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )?.firstOrNull()

            if (words != null) {
                input.setText(words)
                runCommand(words)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == MIC_PERMISSION &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startListening()
        }
    }

    private fun runCommand(command: String) {

        val text = command.trim().lowercase(Locale.ROOT)

        if (text.isEmpty()) {
            status.text = "Please enter a command"
            return
        }

        when {

            text.contains("hello") ||
            text.contains("salam") ||
            text == "hi" -> {
                respond("Hello! I am JARVIS. How can I help you?")
            }

            text.contains("time") -> {
                val time = java.text.SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                ).format(java.util.Date())

                respond("The current time is $time")
            }

            text.contains("shutdown") ||
            text.contains("stop jarvis") -> {
                say("JARVIS shutting down")
                status.text = "JARVIS OFFLINE"
                finish()
            }

            text.startsWith("open ") -> {
                openApp(command.trim().substringAfter("open ").trim())
            }

            else -> {
                respond(
                    "Command received. My advanced AI brain is coming in the next development stages."
                )
            }
        }
    }

    private fun openApp(name: String) {

        val apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(
                Intent.CATEGORY_LAUNCHER
            ), 0
        )

        val match = apps.firstOrNull {
            packageManager.getApplicationLabel(
                it.activityInfo.applicationInfo
            ).toString().contains(name, true)
        }

        if (match != null) {

            val launch = packageManager.getLaunchIntentForPackage(
                match.activityInfo.packageName
            )

            if (launch != null) {
                startActivity(launch)
                respond("Opening $name")
                return
            }
        }

        if (name.contains("setting", true)) {
            startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS)
            )
            respond("Opening Settings")
        } else {
            respond("App not found: $name")
        }
    }

    private fun respond(message: String) {
        status.text = message
        output.append("\n\nJARVIS: $message")
        say(message)
    }

    private fun say(message: String) {
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS"
            )
        }
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
