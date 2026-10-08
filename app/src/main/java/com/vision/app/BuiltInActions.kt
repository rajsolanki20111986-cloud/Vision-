package com.vision.app

object AppButtonMaps {
    val SPOTIFY=mapOf("play" to (.5f to .8f),"pause" to (.5f to .8f),"next" to (.75f to .8f),"previous" to (.25f to .8f))
    val YOUTUBE=mapOf("play" to (.5f to .5f),"pause" to (.5f to .5f),"fullscreen" to (.9f to .9f))
    val INSTAGRAM=mapOf("home" to (.1f to .9f),"search" to (.5f to .9f),"like" to (.1f to .5f))
    val WHATSAPP=mapOf("send" to (.9f to .95f),"message_input" to (.5f to .95f))
    val apps=mapOf("spotify" to SPOTIFY,"youtube" to YOUTUBE,"instagram" to INSTAGRAM,"whatsapp" to WHATSAPP)
}
object FastPathCommands {
    fun canHandleLocally(c:String):Boolean {val x=c.lowercase();return x.contains("pause")||x.contains("next")||x.contains("previous")||x.contains("go home")||x.contains("home screen")||(x.contains("open")&&listOf("spotify","youtube","instagram","whatsapp").any{x.contains(it)})}
    suspend fun executeLocally(c:String,s:VisionAccessibilityService?):Boolean {val x=c.lowercase();when{ x.contains("pause")->s?.executePause();x.contains("next")->s?.executeNext();x.contains("previous")->s?.executePrevious();x.contains("go home")||x.contains("home screen")->s?.pressHome();x.contains("spotify")->s?.openApp("com.spotify.music");x.contains("youtube")->s?.openApp("com.google.android.youtube");x.contains("whatsapp")->s?.openApp("com.whatsapp");x.contains("instagram")->s?.openApp("com.instagram.android");else->return false};return true}
}
object SmartCommandBuilder {fun expandCommand(command:String,context:String?=null)=command}
