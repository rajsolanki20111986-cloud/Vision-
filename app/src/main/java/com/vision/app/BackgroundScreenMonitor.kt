package com.vision.app

import android.content.Context
import kotlinx.coroutines.*

class BackgroundScreenMonitor(private val context: Context) {
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.Default)
    var onNewMessageDetected: ((String,String)->Unit)? = null
    var onScreenChanged: ((String)->Unit)? = null
    private var lastScreenState = ""
    private val seen = mutableSetOf<String>()
    fun startMonitoring(intervalMs: Long = 2000) { if(job?.isActive==true)return; job=scope.launch { while(isActive){ val screen=VisionAccessibilityService.currentScreenContent; if(screen!=lastScreenState){lastScreenState=screen;onScreenChanged?.invoke(screen)}; Regex("""([\\w]+):\\s*(.+?)(?=\\n|$)""").findAll(screen).forEach{val id=it.groupValues[1]+":"+it.groupValues[2];if(seen.add(id))onNewMessageDetected?.invoke(it.groupValues[1],it.groupValues[2].trim())};delay(intervalMs)}} }
    fun stopMonitoring(){job?.cancel();job=null}
    fun cleanup(){stopMonitoring();scope.cancel()}
}
