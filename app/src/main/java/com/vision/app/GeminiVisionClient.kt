package com.vision.app
import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object GeminiVisionClient {
  suspend fun analyzeBitmap(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
    val scaled = if (maxOf(bitmap.width, bitmap.height) > 1280) {
      val ratio = 1280f / maxOf(bitmap.width, bitmap.height)
      Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
    } else bitmap
    val bytes = ByteArrayOutputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 80, out); out.toByteArray() }
    if (scaled !== bitmap) scaled.recycle()
    val parts = JSONArray()
      .put(JSONObject().put("text", prompt))
      .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))))
    val key = Prefs.apiKey.trim()
    require(key.isNotEmpty()) { "Gemini API key is missing in Settings." }
    val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", parts)))
      .put("generationConfig", JSONObject().put("temperature", 0.2).put("maxOutputTokens", 1200))
    val request = Request.Builder()
      .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
      .header("x-goog-api-key", key)
      .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
    OkHttpClient().newCall(request).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) throw IllegalStateException("Gemini API ${response.code}: ${raw.take(200)}")
      val root = JSONObject(raw)
      val candidates = root.optJSONArray("candidates") ?: throw IllegalStateException("Gemini returned no candidates")
      val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: throw IllegalStateException("Gemini returned no content")
      val p = content.optJSONArray("parts") ?: return@use ""
      (0 until p.length()).mapNotNull { p.optJSONObject(it)?.optString("text") }.filter { it.isNotBlank() }.joinToString("\n")
    }
  }
}