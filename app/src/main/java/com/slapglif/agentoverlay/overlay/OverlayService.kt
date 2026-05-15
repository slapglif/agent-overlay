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
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.slapglif.agentoverlay.MainActivity
import com.slapglif.agentoverlay.R

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var mode = OverlayMode.Bubble
    private var selectedAgent = AGENTS.first()
    private val floatingMessages = mutableListOf(
        "Hermes" to "Standing by in Live Hermes run. Ask normally; I’ll reason, use tools, or run commands when needed."
    )

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        if (Settings.canDrawOverlays(this)) showOverlay()
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        val params = overlayParams().apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 240
        }
        renderOverlay(params)
    }

    private fun renderOverlay(params: WindowManager.LayoutParams) {
        overlayView?.let { old ->
            old.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(90).withEndAction {
                runCatching { windowManager.removeView(old) }
            }.start()
        }
        params.flags = if (mode == OverlayMode.Chat) {
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        val view = when (mode) {
            OverlayMode.Bubble -> commandBubble(params)
            OverlayMode.Tray -> actionTray(params)
            OverlayMode.AgentList -> agentCardList(params)
            OverlayMode.Chat -> chatWindow(params)
            OverlayMode.Automation -> automationWindow(params)
        }
        view.alpha = 0f
        view.scaleX = if (mode == OverlayMode.Bubble) 0.78f else 0.88f
        view.scaleY = if (mode == OverlayMode.Bubble) 0.78f else 0.88f
        view.pivotX = dp(24).toFloat()
        view.pivotY = dp(24).toFloat()
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(if (mode == OverlayMode.Bubble) 150 else 220)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        view.setOnTouchListener(DraggableTouchListener(params, windowManager, view))
        overlayView = view
        windowManager.addView(view, params)
    }

    private fun commandBubble(params: WindowManager.LayoutParams): FrameLayout = FrameLayout(this).apply {
        contentDescription = "Agent command button"
        background = rounded(INDIGO, 999f, Color.argb(96, 255, 255, 255))
        elevation = dp(18).toFloat()
        setPadding(dp(4), dp(4), dp(4), dp(4))
        addView(TextView(context).apply {
            text = "☤"
            gravity = Gravity.CENTER
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER))
        addView(View(context).apply {
            background = rounded(SUCCESS, 999f, Color.WHITE)
        }, FrameLayout.LayoutParams(dp(12), dp(12), Gravity.END or Gravity.BOTTOM).apply {
            rightMargin = dp(1)
            bottomMargin = dp(1)
        })
        setOnClickListener {
            mode = OverlayMode.Tray
            renderOverlay(params)
        }
    }

    private fun actionTray(params: WindowManager.LayoutParams): LinearLayout = LinearLayout(this).apply {
        contentDescription = "Quick action tray"
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        addView(commandGlyph(), LinearLayout.LayoutParams(dp(54), dp(54)).apply { rightMargin = dp(8) })

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(PANEL, 28f, Color.argb(42, 255, 255, 255))
            elevation = dp(18).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = "Hermes"
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(TEXT)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(actionChip("×") {
                    mode = OverlayMode.Bubble
                    renderOverlay(params)
                })
            })

            addView(trayRow("💬", "Live chat", "Open the current agent", SUCCESS) {
                selectedAgent = AGENTS.first()
                mode = OverlayMode.Chat
                renderOverlay(params)
            })
            addView(trayRow("●", "Agents", "Switch active assistant", INDIGO) {
                mode = OverlayMode.AgentList
                renderOverlay(params)
            })
            addView(trayRow("☰", "Lists", "Queues and recent runs", INFO) {
                mode = OverlayMode.AgentList
                renderOverlay(params)
            })
            addView(trayRow("⌖", "Phone tools", "Inspect, tap, type, swipe", SUCCESS) {
                mode = OverlayMode.Automation
                renderOverlay(params)
            })
            addView(trayRow("⚙", "Settings", "Gateway and overlay controls", MUTED) { openMainActivity() })
        }, LinearLayout.LayoutParams(dp(248), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun agentCardList(params: WindowManager.LayoutParams): LinearLayout = panel(322, 0).apply {
        contentDescription = "Agent card list"
        addView(hoverControls(params, title = "agents"))
        addView(TextView(context).apply {
            text = "Agent card list"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(TEXT)
            setPadding(0, dp(12), 0, dp(4))
        })
        addView(TextView(context).apply {
            text = "Tap a card to animate into that agent view. Use ↗ / ⚙ for full-screen sections."
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, 0, 0, dp(10))
        })
        AGENTS.forEach { agent ->
            addView(agentRow(agent) {
                selectedAgent = agent
                mode = OverlayMode.Chat
                renderOverlay(params)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        }
        addView(actionText("Back to quick actions", MUTED) {
            mode = OverlayMode.Tray
            renderOverlay(params)
        })
    }

    private fun chatWindow(params: WindowManager.LayoutParams): LinearLayout = panel(340, 430).apply {
        contentDescription = "Floating agent chat"
        addView(hoverControls(params, title = "agent view"))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(10))
            addView(agentBubble(selectedAgent, compact = true), LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(10) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = selectedAgent.title
                    contentDescription = "${selectedAgent.title} title bar"
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(TEXT)
                    setOnClickListener { openMainActivity() }
                })
                addView(TextView(context).apply {
                    text = "${selectedAgent.status} • agent view"
                    textSize = 12f
                    setTextColor(statusColor(selectedAgent.status))
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(actionChip("↗") { openMainActivity() })
        })
        addView(activityRibbon())
        addView(ScrollView(context).apply {
            background = rounded(Color.argb(80, 0, 0, 0), 20f, Color.argb(18, 255, 255, 255))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                floatingMessages.forEach { (sender, body) ->
                    addView(message(sender, body, alignEnd = sender == "You"))
                }
            })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        lateinit var input: EditText
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
            input = EditText(context).apply {
                hint = "Message ${selectedAgent.shortName} or /commands"
                textSize = 14f
                minHeight = dp(46)
                maxLines = 3
                setSingleLine(false)
                setTextColor(TEXT)
                setHintTextColor(SUBTLE)
                background = rounded(Color.argb(26, 255, 255, 255), 18f, Color.argb(28, 255, 255, 255))
                setPadding(dp(12), 0, dp(12), 0)
            }
            addView(input, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(8) })
            addView(actionChip("Send", wide = true) {
                val text = input.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    floatingMessages += "You" to text
                    floatingMessages += "Gateway" to "Queued for Hermes gateway: $text"
                    input.text?.clear()
                    renderOverlay(params)
                }
            })
        })
    }

    private fun automationWindow(params: WindowManager.LayoutParams): LinearLayout = panel(340, 360).apply {
        contentDescription = "Phone automation tools"
        addView(hoverControls(params, title = "phone tools"))
        addView(TextView(context).apply {
            text = "Phone automation"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(TEXT)
            setPadding(0, dp(12), 0, dp(4))
        })
        addView(TextView(context).apply {
            text = "RustDesk-style local control plane: accessibility tree, bounding boxes, tap/type/swipe, and future OCR/vision frames."
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, 0, 0, dp(10))
        })
        addView(toolRow("phone.snapshot", "Current app, screen size, semantic refs"))
        addView(toolRow("phone.tap", "Tap by coordinate or ref, e.g. p12"))
        addView(toolRow("phone.type", "Type into focused/editable fields"))
        addView(toolRow("phone.swipe", "Scroll or gesture with start/end bounds"))
        addView(actionText("Open full phone tool console", INDIGO) { openMainActivity() })
    }

    private fun activityRibbon(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(9))
        background = rounded(Color.argb(22, 255, 255, 255), 999f, Color.argb(24, 255, 255, 255))
        setPadding(dp(10), dp(7), dp(10), dp(7))
        addView(TextView(context).apply {
            text = "Ready · Auto · activity hides until needed"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(MUTED)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(TextView(context).apply {
            text = "⋯"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(TEXT)
        })
    }

    private fun hoverControls(params: WindowManager.LayoutParams, title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(Color.rgb(19, 20, 26), 999f, Color.argb(46, 255, 255, 255))
        setPadding(dp(8), dp(6), dp(8), dp(6))
        addView(TextView(context).apply {
            text = "☤"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(INDIGO)
        })
        addView(TextView(context).apply {
            text = "  $title"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(MUTED)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(actionChip("⌁") {
            mode = OverlayMode.Tray
            renderOverlay(params)
        })
        addView(actionChip("☰") {
            mode = OverlayMode.AgentList
            renderOverlay(params)
        })
        addView(actionChip("⚙") { openMainActivity() })
        addView(actionChip("×") {
            mode = OverlayMode.Bubble
            renderOverlay(params)
        })
    }

    private fun toolRow(name: String, subtitle: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.argb(18, 255, 255, 255), 16f, Color.argb(24, 255, 255, 255))
        setPadding(dp(10), dp(8), dp(10), dp(8))
        addView(TextView(context).apply {
            text = name
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(INFO)
        })
        addView(TextView(context).apply {
            text = subtitle
            textSize = 11f
            setTextColor(MUTED)
        })
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
    }

    private fun trayRow(icon: String, title: String, subtitle: String, color: Int, onClick: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(Color.argb(18, 255, 255, 255), 18f, Color.argb(24, 255, 255, 255))
        setPadding(dp(9), dp(8), dp(9), dp(8))
        setOnClickListener { onClick() }
        addView(TextView(context).apply {
            text = icon
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            background = rounded(Color.argb(34, Color.red(color), Color.green(color), Color.blue(color)), 999f, Color.argb(62, Color.red(color), Color.green(color), Color.blue(color)))
        }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(10); topMargin = dp(4); bottomMargin = dp(4) })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(TEXT)
            })
            addView(TextView(context).apply {
                text = subtitle
                textSize = 11f
                setTextColor(MUTED)
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun agentRow(agent: OverlayAgent, onSelect: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(Color.argb(22, 255, 255, 255), 22f, Color.argb(30, 255, 255, 255))
        setPadding(dp(11), dp(11), dp(11), dp(11))
        setOnClickListener { onSelect() }
        addView(agentBubble(agent, compact = true), LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(10) })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = agent.title
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(TEXT)
                setOnClickListener { onSelect() }
            })
            addView(TextView(context).apply {
                text = "${agent.status} • ${agent.description}"
                textSize = 11f
                setTextColor(MUTED)
                setOnClickListener { onSelect() }
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(actionChip("View") { onSelect() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) })
        addView(TextView(context).apply {
            text = "●"
            textSize = 14f
            setTextColor(statusColor(agent.status))
        })
    }

    private fun agentBubble(agent: OverlayAgent, compact: Boolean): FrameLayout = FrameLayout(this).apply {
        background = rounded(agent.color, 999f, Color.argb(70, 255, 255, 255))
        elevation = dp(if (compact) 10 else 16).toFloat()
        addView(TextView(context).apply {
            text = agent.initials
            gravity = Gravity.CENTER
            textSize = if (compact) 14f else 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        addView(View(context).apply {
            background = rounded(statusColor(agent.status), 999f)
        }, FrameLayout.LayoutParams(dp(11), dp(11), Gravity.END or Gravity.BOTTOM).apply {
            rightMargin = dp(2)
            bottomMargin = dp(2)
        })
    }

    private fun commandGlyph(): FrameLayout = FrameLayout(this).apply {
        background = rounded(INDIGO, 999f, Color.argb(88, 255, 255, 255))
        elevation = dp(12).toFloat()
        addView(TextView(context).apply {
            text = "☤"
            gravity = Gravity.CENTER
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun message(sender: String, body: String, alignEnd: Boolean = false): TextView = TextView(this).apply {
        text = "$sender\n$body"
        textSize = 13f
        setLineSpacing(0f, 1.08f)
        setTextColor(TEXT)
        background = rounded(if (alignEnd) Color.argb(66, 113, 112, 255) else Color.argb(30, 255, 255, 255), 18f, Color.argb(26, 255, 255, 255))
        setPadding(dp(10), dp(8), dp(10), dp(8))
        val width = dp(250)
        layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = if (alignEnd) Gravity.END else Gravity.START
            bottomMargin = dp(8)
        }
    }

    private fun panel(widthDp: Int, heightDp: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(PANEL, 30f, Color.argb(34, 255, 255, 255))
        elevation = dp(18).toFloat()
        setPadding(dp(14), dp(12), dp(14), dp(14))
        minimumWidth = dp(widthDp)
        if (heightDp > 0) minimumHeight = dp(heightDp)
    }

    private fun actionText(textValue: String, color: Int, onClick: () -> Unit): TextView = TextView(this).apply {
        text = textValue
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(12), dp(12), 0)
        setOnClickListener { onClick() }
    }

    private fun actionChip(textValue: String, wide: Boolean = false, onClick: () -> Unit = {}): TextView = TextView(this).apply {
        text = textValue
        gravity = Gravity.CENTER
        textSize = if (wide) 13f else 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TEXT)
        background = rounded(if (wide) INDIGO else Color.rgb(35, 36, 44), 999f, Color.argb(34, 255, 255, 255))
        minWidth = dp(if (wide) 58 else 34)
        minHeight = dp(34)
        setPadding(dp(if (wide) 14 else 9), dp(7), dp(if (wide) 14 else 9), dp(7))
        setOnClickListener { onClick() }
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
            .setContentTitle("Agent command button active")
            .setContentText("Tap the floating Hermes icon for quick actions")
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

    private fun rounded(color: Int, radius: Float, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(color)
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun statusColor(status: String): Int = when (status) {
        "running" -> SUCCESS
        "failed" -> RAY_RED
        "done" -> INDIGO
        else -> WARN
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "agent_overlay"
        private const val NOTIFICATION_ID = 42
        private val TEXT = Color.rgb(246, 247, 251)
        private val MUTED = Color.rgb(160, 166, 180)
        private val SUBTLE = Color.rgb(118, 126, 143)
        private val PANEL = Color.rgb(5, 6, 8)
        private val RAY_RED = Color.rgb(255, 99, 99)
        private val INDIGO = Color.rgb(113, 112, 255)
        private val SUCCESS = Color.rgb(69, 222, 128)
        private val WARN = Color.rgb(245, 184, 76)
        private val INFO = Color.rgb(114, 184, 255)
        private val AGENTS = listOf(
            OverlayAgent("live", "Live Hermes run", "LH", "running", "streaming", Color.rgb(113, 112, 255)),
            OverlayAgent("brief", "Daily gateway brief", "DB", "idle", "ready", Color.rgb(255, 99, 99)),
            OverlayAgent("ops", "Ops sentinel", "OS", "done", "watched", Color.rgb(41, 182, 246))
        )
    }
}

private enum class OverlayMode { Bubble, Tray, AgentList, Chat, Automation }

private data class OverlayAgent(
    val id: String,
    val title: String,
    val initials: String,
    val status: String,
    val description: String,
    val color: Int
) {
    val shortName: String get() = title.substringBefore(' ')
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
    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

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
                moved = moved || kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop
                if (moved) {
                    val metrics = view.resources.displayMetrics
                    params.x = (initialX + dx).coerceIn(0, (metrics.widthPixels - view.width).coerceAtLeast(0))
                    params.y = (initialY + dy).coerceIn(0, (metrics.heightPixels - view.height).coerceAtLeast(0))
                    windowManager.updateViewLayout(view, params)
                }
                return moved
            }
            MotionEvent.ACTION_UP -> return moved
        }
        return false
    }
}
