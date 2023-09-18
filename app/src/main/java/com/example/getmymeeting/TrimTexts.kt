package com.example.getmymeeting

class TrimTexts {
    fun trimPass(password: String): String {
        return password.replace("\\s+".toRegex(), "")
    }

    fun trimText(text: String): String{
        return text.replace("\\s+".toRegex(), " ").trim()
    }
}