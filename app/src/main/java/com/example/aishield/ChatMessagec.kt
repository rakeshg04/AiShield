package com.example.aishield

data class ChatMessage(
    val text: String,
    val isUser: Boolean // true = user message, false = bot reply
)
