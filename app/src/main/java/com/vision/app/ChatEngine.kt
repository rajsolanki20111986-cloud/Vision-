package com.vision.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Text chat: plain Gemini REST call with the same Vision personality. */
object ChatEngine {
    private const val MODEL = "gemini-flash-latest" // change here if you want a specific model
    private val client = OkHttpClient()

    suspend fun reply(history: List<Pair<Boolean, String>>): String = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        history.forEach { (user, text) ->
            contents.put(JSONObject()
                .put("role", if (user) "user" else "model")
                .put("parts", JSONArray().put(JSONObject().put("text", text))))
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", Persona.PROMPT + Persona.TEXT_NOTE))))
            .put("contents", contents)
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
            .header("x-goog-api-key", Prefs.apiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (!r.isSuccessful) "Error ${r.code}: ${s.take(200)}"
            else JSONObject(s).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }
}
