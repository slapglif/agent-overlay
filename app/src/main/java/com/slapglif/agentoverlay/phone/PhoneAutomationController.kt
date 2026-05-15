package com.slapglif.agentoverlay.phone

import com.slapglif.agentoverlay.model.PhoneAutomationAction
import com.slapglif.agentoverlay.model.PhoneAutomationResult
import com.slapglif.agentoverlay.model.PhoneAutomationTool
import com.slapglif.agentoverlay.model.PhoneElement
import com.slapglif.agentoverlay.model.PhoneScreenSnapshot
import org.json.JSONArray
import org.json.JSONObject

class PhoneAutomationController {
    fun tools(): List<PhoneAutomationTool> = TOOL_DEFINITIONS

    fun toolSchemas(): JSONArray = JSONArray().apply {
        TOOL_DEFINITIONS.forEach { tool ->
            put(JSONObject()
                .put("name", tool.name)
                .put("description", tool.description)
                .put("input_schema", JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().apply { tool.parameters.forEach { put(it, JSONObject().put("type", if (it.endsWith("Ms") || it in setOf("x", "y", "startX", "startY", "endX", "endY")) "number" else "string")) } })
                    .put("additionalProperties", false)))
        }
    }

    fun executeTool(name: String, arguments: JSONObject = JSONObject()): PhoneAutomationResult = when (name) {
        "phone.snapshot", "phone.accessibility_tree", "phone.ocr", "phone.vision_snapshot" -> inspect()
        "phone.back" -> perform(PhoneAutomationAction.Back)
        "phone.home" -> perform(PhoneAutomationAction.Home)
        "phone.recents" -> perform(PhoneAutomationAction.Recents)
        "phone.tap" -> {
            val ref = arguments.optString("ref").takeIf { it.isNotBlank() }
            if (ref != null) perform(PhoneAutomationAction.TapRef(ref))
            else perform(PhoneAutomationAction.Tap(arguments.optInt("x"), arguments.optInt("y")))
        }
        "phone.type" -> perform(PhoneAutomationAction.TypeText(arguments.optString("text")))
        "phone.swipe" -> perform(PhoneAutomationAction.Swipe(
            arguments.optInt("startX"),
            arguments.optInt("startY"),
            arguments.optInt("endX"),
            arguments.optInt("endY"),
            arguments.optLong("durationMs", 280)
        ))
        else -> PhoneAutomationResult(false, name, "Unsupported phone tool: $name", currentSnapshot())
    }

    fun inspect(): PhoneAutomationResult {
        val snapshot = currentSnapshot()
        return if (snapshot == null) {
            PhoneAutomationResult(
                ok = false,
                action = "phone.snapshot",
                message = "Phone automation is not enabled. Enable Agent Overlay Control in Android Accessibility settings."
            )
        } else {
            PhoneAutomationResult(
                ok = true,
                action = "phone.snapshot",
                message = snapshot.toAgentSummary(),
                snapshot = snapshot
            )
        }
    }

    fun perform(action: PhoneAutomationAction): PhoneAutomationResult {
        val service = PhoneAutomationService.active()
        val snapshot = currentSnapshot()
        if (service == null) {
            return PhoneAutomationResult(false, action.name(), "Phone automation is not enabled. Enable Agent Overlay Control in Accessibility settings.", snapshot)
        }
        val ok = when (action) {
            PhoneAutomationAction.Snapshot -> true
            PhoneAutomationAction.Back -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            PhoneAutomationAction.Home -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            PhoneAutomationAction.Recents -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
            is PhoneAutomationAction.Tap -> service.tap(action.x, action.y)
            is PhoneAutomationAction.TapRef -> {
                val element = snapshot?.elements?.firstOrNull { it.ref == action.ref }
                if (element == null) false else service.tap(element.bounds.centerX(), element.bounds.centerY())
            }
            is PhoneAutomationAction.TypeText -> service.typeText(action.text)
            is PhoneAutomationAction.Swipe -> service.swipe(action.startX, action.startY, action.endX, action.endY, action.durationMs)
        }
        return PhoneAutomationResult(
            ok = ok,
            action = action.name(),
            message = if (ok) "Executed ${action.name()}" else "Could not execute ${action.name()}; permission, target, or focused field unavailable.",
            snapshot = currentSnapshot() ?: snapshot
        )
    }

    fun currentSnapshot(): PhoneScreenSnapshot? = PhoneAutomationService.snapshot()

    companion object {
        val TOOL_DEFINITIONS = listOf(
            PhoneAutomationTool("phone.snapshot", "Return current app/window, screen size, semantic element refs, and bounding boxes.", listOf()),
            PhoneAutomationTool("phone.accessibility_tree", "Return visible text/contentDescription nodes with Playwright-style refs and bounds.", listOf("max_elements")),
            PhoneAutomationTool("phone.vision_snapshot", "Return the same structured context used for future OCR/vision frame fusion.", listOf()),
            PhoneAutomationTool("phone.ocr", "Return text-like visible refs; OCR falls back to accessibility labels until screen capture is granted.", listOf()),
            PhoneAutomationTool("phone.tap", "Tap a coordinate or semantic ref such as p12.", listOf("x", "y", "ref")),
            PhoneAutomationTool("phone.type", "Set text into the focused or first editable field.", listOf("text")),
            PhoneAutomationTool("phone.swipe", "Swipe by coordinates for scrolling or gestures.", listOf("startX", "startY", "endX", "endY", "durationMs")),
            PhoneAutomationTool("phone.back", "Press Android back.", listOf()),
            PhoneAutomationTool("phone.home", "Press Android home.", listOf()),
            PhoneAutomationTool("phone.recents", "Open Android recents.", listOf())
        )
    }
}

fun PhoneScreenSnapshot.toAgentSummary(): String {
    val header = "${packageName ?: "unknown app"} ${width}x$height r$rotation"
    val top = elements.take(8).joinToString("\n") { it.toAgentLine() }
    return buildString {
        append("Phone context: ").append(header)
        append("\nVisible refs:")
        if (top.isBlank()) append(" none") else append('\n').append(top)
    }
}

private fun PhoneElement.toAgentLine(): String {
    val flags = listOfNotNull(
        "click".takeIf { clickable },
        "edit".takeIf { editable },
        "scroll".takeIf { scrollable }
    ).joinToString("/").ifBlank { "view" }
    return "[$ref] ${text.ifBlank { contentDescription.orEmpty() }.take(72)} @ ${bounds.left},${bounds.top},${bounds.right},${bounds.bottom} $flags"
}

private fun PhoneAutomationAction.name(): String = when (this) {
    PhoneAutomationAction.Snapshot -> "phone.snapshot"
    PhoneAutomationAction.Back -> "phone.back"
    PhoneAutomationAction.Home -> "phone.home"
    PhoneAutomationAction.Recents -> "phone.recents"
    is PhoneAutomationAction.Tap -> "phone.tap"
    is PhoneAutomationAction.TapRef -> "phone.tap"
    is PhoneAutomationAction.TypeText -> "phone.type"
    is PhoneAutomationAction.Swipe -> "phone.swipe"
}
