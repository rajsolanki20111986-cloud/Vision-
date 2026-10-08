package com.vision.app

import android.media.*
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioStreamerV0_3 {
 companion object {private const val IN_RATE=16000;private const val OUT_RATE=24000;private const val GAIN=1.5f;private const val OUT_GAIN=1.2f}
 private var record:AudioRecord?=null;private var track:AudioTrack?=null;private var noiseFloor=100f
 fun initializeAudio():Boolean=try{val bi=AudioRecord.getMinBufferSize(IN_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)*2;record=AudioRecord(MediaRecorder.AudioSource.MIC,IN_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bi);val bo=AudioTrack.getMinBufferSize(OUT_RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);track=if(Build.VERSION.SDK_INT>=21)AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(OUT_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(bo).build() else null;record?.startRecording();track?.play();true}catch(_:Exception){false}
 suspend fun recordAudioFrame(size:Int=1024):ByteArray?=withContext(Dispatchers.IO){val b=ShortArray(size);val n=record?.read(b,0,size)?:0;if(n<=0)null else shortsToBytes(b.copyOf(n).map{(it*GAIN).coerceIn(Short.MIN_VALUE.toFloat(),Short.MAX_VALUE.toFloat()).toInt().toShort()}.toShortArray())}
 suspend fun playAudioFrame(data:ByteArray)=withContext(Dispatchers.IO){track?.write(shortsToBytes(bytesToShorts(data)).map{(it*OUT_GAIN).coerceIn(Short.MIN_VALUE.toFloat(),Short.MAX_VALUE.toFloat()).toInt().toShort()}.toShortArray(),0,data.size)}
 fun calibrateNoiseFloor(){noiseFloor=100f}
 fun cleanup(){record?.stop();record?.release();record=null;track?.stop();track?.release();track=null}
 private fun shortsToBytes(s:ShortArray):ByteArray{val b=ByteArray(s.size*2);for(i in s.indices){b[i*2]=(s[i].toInt() and 255).toByte();b[i*2+1]=(s[i].toInt() shr 8).toByte()};return b}
 private fun bytesToShorts(b:ByteArray):ShortArray{val s=ShortArray(b.size/2);for(i in s.indices)s[i]=((b[i*2].toInt() and 255) or ((b[i*2+1].toInt() and 255) shl 8)).toShort();return s}
}
