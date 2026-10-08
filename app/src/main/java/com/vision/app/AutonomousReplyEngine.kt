package com.vision.app

import kotlinx.coroutines.*

class AutonomousReplyEngine(private val liveSession: LiveSession) {
    var currentPerson=""; var currentApp=""; var currentLanguage="en"
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    suspend fun enableAutonomousReplyMode(personName:String,app:String,language:String="en"){currentPerson=personName;currentApp=app;currentLanguage=language;ConversationManager.startConversation(personName,app=app,language=language);ConversationManager.enableAutoReply()}
    suspend fun disableAutonomousReplyMode(){ConversationManager.disableAutoReply();ConversationManager.endConversation()}
    suspend fun handleIncomingMessage(sender:String,messageText:String){if(!ConversationManager.shouldReplyAutonomously())return;ConversationManager.addMessage(sender,messageText);val prompt="You are Vision.\n"+ConversationManager.getContextString()+"\n"+sender+" sent: \""+messageText+"\"\nReply briefly in "+currentLanguage+". Return only the reply.";val reply=liveSession.sendMessage(prompt).trim().lineSequence().firstOrNull().orEmpty();if(reply.isNotBlank()){VisionAccessibilityService.inst?.typeText(reply);VisionAccessibilityService.inst?.pressEnter();ConversationManager.addMessage("Vision",reply)}}
    fun shouldEndConversation():Boolean{val c=ConversationManager.getConversation()?:return false;return System.currentTimeMillis()-c.lastMessageTime>600000||c.messageCount>20}
    suspend fun endConversationNaturally(){disableAutonomousReplyMode()}
    fun cleanup(){scope.cancel()}
}
