package com.vision.app

import kotlinx.coroutines.delay
object FastPath {
    private var service:VisionAccessibilityService?=null
    fun setAccessibilityService(s:VisionAccessibilityService){service=s}
    suspend fun tryExecuteFastPath(command:String):Boolean{if(!FastPathCommands.canHandleLocally(command))return false;return try{FastPathCommands.executeLocally(command,service)}catch(_:Exception){false}}
    suspend fun playMusicFastPath(query:String){service?.apply{openApp("com.spotify.music");delay(1200);tapXY(.5f,.05f);delay(400);typeText(query);pressEnter();delay(800);tapXY(.5f,.8f)}}
    suspend fun searchYouTubeFastPath(query:String){service?.apply{openApp("com.google.android.youtube");delay(1200);tapXY(.1f,.05f);delay(400);typeText(query);pressEnter()}}
    suspend fun openAppFastPath(name:String){val p=mapOf("spotify" to "com.spotify.music","youtube" to "com.google.android.youtube","instagram" to "com.instagram.android","whatsapp" to "com.whatsapp","gmail" to "com.google.android.gm","chrome" to "com.android.chrome","maps" to "com.google.android.apps.maps")[name.lowercase()];if(p!=null)service?.openApp(p)}
    suspend fun navigateHomeFastPath(){service?.pressHome()}
}
