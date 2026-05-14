package com.slapglif.agentoverlay.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.slapglif.agentoverlay.MainActivity
import com.slapglif.agentoverlay.R

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isExpanded = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        if (Settings.canDrawOverlays(this)) showOverlay()
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        val params = overlayParams().apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 220
        }
        renderOverlay(params)
    }

    private fun renderOverlay(params: WindowManager.LayoutParams) {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        val view = if (isExpanded) expandedMenu(params) else orb(params)
        view.setOnTouchListener(DraggableTouchListener(params, windowManager, view))
        overlayView = view
        windowManager.addView(view, params)
    }

    private fun orb(params: WindowManager.LayoutParams): TextView = TextView(this).apply {
        text = "☤"
        contentDescription = "Agent Overlay"
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(124, 58, 237), 999f)
        setPadding(dp(18), dp(12), dp(18), dp(12))
        setOnClickListener {
            isExpanded = true
            renderOverlay(params)
        }
    }

    private fun expandedMenu(params: WindowManager.LayoutParams): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.rgb(17, 24, 39), 32f)
        setPadding(dp(18), dp(16), dp(18), dp(16))
        minimumWidth = dp(300)
        addView(TextView(context).apply {
            text = "☤ Agent Overlay"
            contentDescription = "Agent Overlay menu"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        addView(TextView(context).apply {
            text = "C&C"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(56, 189, 248))
            setPadding(0, dp(12), 0, 0)
        })
        addView(TextView(context).apply {
            text = "Hermes gateway command center is live. Open dashboard for agents, jobs, and thread chat."
            textSize = 14f
            setTextColor(Color.rgb(229, 231, 235))
            setPadding(0, dp(6), 0, dp(10))
        })
        addView(TextView(context).apply {
            text = "Open dashboard"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(124, 58, 237), 18f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { openMainActivity() }
        })
        addView(TextView(context).apply {
            text = "Collapse"
            textSize = 14f
            setTextColor(Color.rgb(203, 213, 225))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), 0)
            setOnClickListener {
                isExpanded = false
                renderOverlay(params)
            }
        })
    }

    private fun overlayParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun notification(): Notification {
        val intent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Agent Overlay active")
            .setContentText("Floating Hermes command center is running")
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Agent Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(color)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "agent_overlay"
        private const val NOTIFICATION_ID = 42
    }
}

private class DraggableTouchListener(
    private val params: WindowManager.LayoutParams,
    private val windowManager: WindowManager,
    private val view: View
) : View.OnTouchListener {
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var moved = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                moved = false
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                moved = moved || kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8
                params.x = initialX + dx
                params.y = initialY + dy
                windowManager.updateViewLayout(view, params)
                return true
            }
            MotionEvent.ACTION_UP -> return moved
        }
        return false
    }
}
