package com.example.aishield

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class ChatActivity : AppCompatActivity() {

    private lateinit var chatLayout: LinearLayout
    private lateinit var inputMessage: EditText
    private lateinit var sendBtn: Button
    private lateinit var scrollView: ScrollView
    private lateinit var service: OllamaApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatLayout = findViewById(R.id.chatLayout)
        inputMessage = findViewById(R.id.inputMessage)
        sendBtn = findViewById(R.id.sendBtn)
        scrollView = findViewById(R.id.scrollView)

        // Change this to your ngrok / LAN URL
        val retrofit = Retrofit.Builder()
            .baseUrl("https://abc123.ngrok.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(OllamaApi::class.java)

        sendBtn.setOnClickListener {
            val message = inputMessage.text.toString()
            if (message.isNotEmpty()) {
                addMessage("You: $message", Gravity.END)
                inputMessage.text.clear()
                sendMessageToOllama(message)
            }
        }
    }

    private fun sendMessageToOllama(message: String) {
        val request = OllamaRequest(model = "llama2", prompt = message)

        service.generate(request).enqueue(object : Callback<OllamaResponse> {
            override fun onResponse(call: Call<OllamaResponse>, response: Response<OllamaResponse>) {
                if (response.isSuccessful) {
                    val reply = response.body()?.response ?: "No reply"
                    addMessage("AI: $reply", Gravity.START)
                } else {
                    addMessage("Error: ${response.code()}", Gravity.START)
                }
            }

            override fun onFailure(call: Call<OllamaResponse>, t: Throwable) {
                Log.e("Chatbot", "Error: ${t.message}")
                addMessage("Failed to connect to server.", Gravity.START)
            }
        })
    }

    private fun addMessage(text: String, gravity: Int) {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = 16f
        textView.setPadding(16, 8, 16, 8)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = gravity
        textView.layoutParams = params

        chatLayout.addView(textView)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
