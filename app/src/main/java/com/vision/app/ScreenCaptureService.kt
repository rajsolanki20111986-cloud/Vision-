package com.vision.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager

/** Explicitly user-consented display capture; frames stay in memory and are throttled. */
class ScreenCaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        const val EXTRA_INTERVAL_MS = "capture_interval_ms"
        private val frameLock = Any()
        @Volatile private var latestFrame: Bitmap? = null
        @Volatile var lastFrameAtMs: Long = 0L
            private set
        @Volatile var captureError: String = ""
            private set
        fun copyLatestFrame(): Bitmap? = synchronized(frameLock) {
            latestFrame?.takeIf { !it.isRecycled }?.copy(Bitmap.Config.ARGB_8888, false)
        }
        private const val CHANNEL_ID = "vision_screen_capture"
        private const val NOTIFICATION_ID = 4201
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var lastAcceptedFrameAt = 0L
    private var intervalMs = 1000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(NOTIFICATION_ID, notification())
        intervalMs = (intent?.getLongExtra(EXTRA_INTERVAL_MS, 1000L) ?: 1000L).coerceIn(250L, 5000L)
        if (projection == null) startProjection(intent)
        return START_NOT_STICKY
    }

    private fun startProjection(intent: Intent?) {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode == 0 || data == null) {
            captureError = "Screen-capture permission data is missing. Start again from Vision."
            stopSelf()
            return
        }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            worker = HandlerThread("VisionFrameCapture").also { it.start() }
            val handler = Handler(worker!!.looper)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
            imageReader!!.setOnImageAvailableListener({ reader -> acceptFrame(reader.acquireLatestImage()) }, handler)
            projection!!.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, handler)
            virtualDisplay = projection!!.createVirtualDisplay(
                "VisionDisplayCapture", width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader!!.surface, null, handler
            )
            captureError = ""
        } catch (e: Exception) {
            captureError = "Screen capture could not start: ${e.message ?: e.javaClass.simpleName}"
            stopSelf()
        }
    }

    private fun acceptFrame(image: Image?) {
        if (image == null) return
        image.use { frame ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastAcceptedFrameAt < intervalMs) return
            lastAcceptedFrameAt = now
            try {
                val plane = frame.planes.firstOrNull() ?: return
                val paddedWidth = plane.rowStride / plane.pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, frame.height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                val clean = if (paddedWidth != frame.width) {
                    Bitmap.createBitmap(padded, 0, 0, frame.width, frame.height).also { padded.recycle() }
                } else padded
                synchronized(frameLock) {
                    val previous = latestFrame
                    latestFrame = clean
                    previous?.takeIf { it !== clean && !it.isRecycled }?.recycle()
                    lastFrameAtMs = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                captureError = "Frame processing failed: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Vision screen capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder.setContentTitle("Vision screen understanding is active")
            .setContentText("Screen capture is active with your permission. Stop it in Vision.")
            .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build()
    }

    override fun onDestroy() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close(); imageReader = null
        projection?.stop(); projection = null
        worker?.quitSafely(); worker = null
        synchronized(frameLock) {
            latestFrame?.recycle(); latestFrame = null
            lastFrameAtMs = 0L
        }
        super.onDestroy()
    }
}