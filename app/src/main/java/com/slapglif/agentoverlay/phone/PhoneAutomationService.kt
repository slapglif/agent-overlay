package com.slapglif.agentoverlay.phone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.slapglif.agentoverlay.model.PhoneBounds
import com.slapglif.agentoverlay.model.PhoneElement
import com.slapglif.agentoverlay.model.PhoneScreenSnapshot
import java.util.concurrent.atomic.AtomicReference

/**
 * Accessibility-backed phone-control bridge.
 *
 * This mirrors the RustDesk Android control split: capture/context and input injection are
 * local privileged services, while Hermes remains the high-level control plane. The service
 * exposes Playwright-MCP-like element refs and bounding boxes so an agent can prefer semantic
 * refs over blind coordinates.
 */
class PhoneAutomationService : AccessibilityService() {
    override fun onServiceConnected() {
        instance.set(this)
        publishSnapshot()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        publishSnapshot(event)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance.get() === this) instance.set(null)
        super.onDestroy()
    }

    private fun publishSnapshot(event: AccessibilityEvent? = null) {
        val root = rootInActiveWindow ?: return
        val metrics = resources.displayMetrics
        latestSnapshot.set(
            PhoneScreenSnapshot(
                width = metrics.widthPixels,
                height = metrics.heightPixels,
                rotation = display?.rotation ?: 0,
                packageName = event?.packageName?.toString() ?: root.packageName?.toString(),
                windowTitle = event?.className?.toString(),
                elements = root.flattenElements().take(MAX_ELEMENTS)
            )
        )
    }

    fun tap(x: Int, y: Int, durationMs: Long = 60): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(1))
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long = 280): Boolean {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(120))
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val editable = root.findFirstEditable() ?: return false
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    companion object {
        private const val MAX_ELEMENTS = 80
        private val instance = AtomicReference<PhoneAutomationService?>(null)
        private val latestSnapshot = AtomicReference<PhoneScreenSnapshot?>(null)

        fun active(): PhoneAutomationService? = instance.get()
        fun snapshot(): PhoneScreenSnapshot? = latestSnapshot.get()
        fun isEnabled(): Boolean = instance.get() != null
    }
}

private fun AccessibilityNodeInfo.flattenElements(): List<PhoneElement> {
    val out = mutableListOf<PhoneElement>()
    var index = 1

    fun visit(node: AccessibilityNodeInfo?) {
        if (node == null || out.size >= 80) return
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val label = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim()
        val useful = label.isNotBlank() || !desc.isNullOrBlank() || node.isClickable || node.isEditable || node.isScrollable
        if (useful && !bounds.isEmpty) {
            out += PhoneElement(
                ref = "p${index++}",
                text = label.ifBlank { desc.orEmpty() },
                contentDescription = desc,
                bounds = PhoneBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                clickable = node.isClickable,
                editable = node.isEditable,
                scrollable = node.isScrollable,
                source = PhoneElement.Source.Accessibility
            )
        }
        for (i in 0 until node.childCount) visit(node.getChild(i))
    }

    visit(this)
    return out
}

private fun AccessibilityNodeInfo.findFirstEditable(): AccessibilityNodeInfo? {
    if (isEditable) return this
    for (i in 0 until childCount) {
        getChild(i)?.findFirstEditable()?.let { return it }
    }
    return null
}
