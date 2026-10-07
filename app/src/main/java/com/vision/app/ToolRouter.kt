package com.vision.app

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/** Function-calling tools that Gemini can invoke. */
object ToolRouter {

    private fun d(name: String, desc: String, vararg p: Pair<String, String>): JSONObject {
        val props = JSONObject()
        p.forEach { props.put(it.first, JSONObject().put("type", "STRING").put("description", it.second)) }
        val o = JSONObject().put("name", name).put("description", desc)
        if (p.isNotEmpty()) o.put("parameters", JSONObject()
            .put("type", "OBJECT").put("properties", props).put("required", JSONArray(p.map { it.first })))
        return o
    }

    fun declarations() = JSONArray().apply {
        addAll(SystemPermissionTools.declarations())
        put(d("open_app", "Open an installed app by name, e.g. YouTube, Instagram, CapCut, Phone.", "name" to "App name"))
        put(d("open_url", "Open a URL or URI (https, tel:, sms:) in the best app.", "url" to "Full URL or URI"))
        put(d("youtube_search", "Open YouTube search results.", "query" to "Search text"))
        put(d("read_screen", "Read everything visible on the current screen: texts, buttons and their positions."))
        put(d("tap_text", "Tap the element whose text or description contains this text.", "text" to "Text to tap"))
        put(d("tap_xy", "Tap exact screen coordinates taken from read_screen.", "x" to "X pixel", "y" to "Y pixel"))
        put(d("type_text", "Type text into the focused or first input field.", "text" to "Text to type"))
        put(d("press_enter", "Press Enter / Search / Send on the keyboard."))
        put(d("scroll", "Scroll the screen. 'down' reveals content further down.", "direction" to "up, down, left or right"))
        put(d("press_key", "Press a system button.", "key" to "back, home, recents or notifications"))
        put(d("media", "Control whatever media is playing.", "action" to "play_pause, next or previous"))
        put(d("volume", "Change media volume.", "direction" to "up or down"))
    }

    suspend fun run(ctx: Context, name: String, a: JSONObject): String {
        val app = ctx.applicationContext
        val result = SystemPermissionTools.run(app, name, a)
        if (result != "Unknown tool: $name") return result
        when (name) {
            "open_app" -> return openApp(app, a.optString("name")).also { delay(1500) }
            "open_url" -> return openUrl(app, a.optString("url")).also { delay(1500) }
            "youtube_search" -> return openUrl(
                app, "https://www.youtube.com/results?search_query=" + Uri.encode(a.optString("query")),
                "com.google.android.youtube"
            ).also { delay(2000) }
            "media" -> return media(app, a.optString("action"))
            "volume" -> return volume(app, a.optString("direction"))
        }
        val s = VisionAccessibilityService.inst
            ?: return "Accessibility service is not running. Ask the user to enable Vision Control in Android Accessibility settings."
        s.blockedReason()?.let { return it }
        return when (name) {
            "read_screen" -> s.readScreen()
            "tap_text" -> s.clickText(a.optString("text")).also { delay(600) }
            "tap_xy" -> (if (s.tap(a.optString("x").toDouble().toInt(), a.optString("y").toDouble().toInt())) "Tapped" else "Tap failed").also { delay(600) }
            "type_text" -> s.typeText(a.optString("text"))
            "press_enter" -> s.pressEnter().also { delay(800) }
            "scroll" -> s.scroll(a.optString("direction")).also { delay(500) }
            "press_key" -> s.press(a.optString("key")).also { delay(600) }
            else -> "Unknown tool: $name"
        }
    }

    private fun openApp(ctx: Context, name: String): String {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val hit = pm.queryIntentActivities(q, 0).firstOrNull { it.loadLabel(pm).toString().contains(name, ignoreCase = true) }
            ?: return "App not found: $name"
        val i = pm.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return "Cannot launch $name"
        return try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Opened $name. Call read_screen next." }
        catch (e: Exception) { "Launch blocked: ${e.message}. Allow 'Display over other apps' for Vision." }
    }

    private fun openUrl(ctx: Context, url: String, pkg: String? = null): String {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (pkg != null) i.setPackage(pkg)
        return try { ctx.startActivity(i); "Opened. Call read_screen next." }
        catch (e: Exception) { "Could not open: ${e.message}. Allow 'Display over other apps' for Vision." }
    }

    private fun media(ctx: Context, action: String): String {
        val code = when (action) {
            "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return "Unknown action"
        }
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return "Done"
    }

    private fun volume(ctx: Context, dir: String): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (dir == "up") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        return "Done"
    }
}
