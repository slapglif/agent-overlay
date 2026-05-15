---
version: alpha
name: Agent Overlay Material Dark Workbench
description: Material Design 3 dark-mode-first mobile workbench for Monica/Messenger-style floating Hermes agent controls.
colors:
  primary: "#8B83FF"
  secondary: "#72B8FF"
  tertiary: "#FF7A1A"
  neutral: "#07080B"
  surface: "#101116"
  surfaceRaised: "#22252D"
  surfaceMuted: "#181A20"
  border: "#343844"
  active: "#675BE8"
  success: "#45D483"
  warning: "#FFC857"
  info: "#72B8FF"
typography:
  h1:
    fontFamily: Inter
    fontSize: 1.75rem
    fontWeight: 650
    lineHeight: 1.14
    letterSpacing: "-0.025em"
  h2:
    fontFamily: Inter
    fontSize: 1.125rem
    fontWeight: 650
    lineHeight: 1.22
    letterSpacing: "-0.01em"
  body-md:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: 450
    lineHeight: 1.43
  label:
    fontFamily: Inter
    fontSize: 0.75rem
    fontWeight: 650
    lineHeight: 1.33
  mono:
    fontFamily: JetBrains Mono
    fontSize: 0.75rem
    fontWeight: 500
    lineHeight: 1.35
rounded:
  sm: 8px
  md: 16px
  lg: 24px
  xl: 28px
  pill: 999px
spacing:
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 24px
components:
  button-primary:
    backgroundColor: "{colors.active}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    padding: 12px
  button-secondary:
    backgroundColor: "{colors.surfaceRaised}"
    textColor: "#F4F6FB"
    rounded: "{rounded.md}"
    padding: 12px
  agent-command-button:
    backgroundColor: "{colors.active}"
    textColor: "#FFFFFF"
    rounded: "{rounded.pill}"
    size: 48px
  panel:
    backgroundColor: "{colors.surface}"
    textColor: "#F4F6FB"
    rounded: "{rounded.xl}"
    padding: 14px
  input:
    backgroundColor: "{colors.surfaceRaised}"
    textColor: "#F4F6FB"
    rounded: "{rounded.md}"
    padding: 12px
---

## Overview

Agent Overlay now uses a Material Design 3 dark-mode-first workbench model with a Monica/Messenger/WhatsApp/WeChat overlay interaction: a tiny floating command button, an attached quick-action tray, agent cards, and a selected agent view. Replit remains the full-screen workflow inspiration, but the overlay follows recognizable mobile chat-assistant conventions rather than abstract graph navigation.

Applied notes:

- Left activity rail with Material-style 48dp touch affordances.
- Dark surface stack: background → panel → surface → raised surface.
- Primary action uses active indigo container, not low-contrast gray.
- Agent cards use checkpoint/status language with compact labels.
- Composer is anchored, rounded, and minimum-touch-target friendly.
- Floating overlay starts as one small circular command button, then unfolds into an attached quick-action tray before escalating to agent cards or a selected agent view.
- Chat surfaces follow “quiet chat, revealed control”: model in the header, one `+` accessory menu, an expandable activity pill for reasoning/tool steps, and a slash-command palette only after the user types `/`.

## Colors

- **Neutral (#07080B):** App background / deepest surface.
- **Surface (#101116):** Main panels, cards, and activity rail.
- **Surface Muted (#181A20):** Recessed transcript and workspace wells.
- **Surface Raised (#22252D):** Inputs, selected cards, chips, and controls.
- **Primary (#8B83FF):** Material primary accent for selected/interactive states.
- **Active (#675BE8):** Filled primary button and active bubble identity.
- **Tertiary (#FF7A1A):** Replit-like orange accent for agent-specific hints, never for bulk chrome.
- **Border (#343844):** Material outline/outlineVariant style border.

## Typography

Use Inter/system sans throughout, aligned to Material 3 roles: headline for the app title, title for card headers, body for readable descriptions, label for status pills and compact controls, mono only for IDs and gateway URLs.

## Layout

- Full-screen app: Material dark workbench with activity rail + content stack.
- Overlay: command icon → attached quick-action tray → agent card list or quick section → selected agent view remains the primary user journey.
- Full-screen Activity is secondary and opens only via title/gear/expand.
- Interactive controls should meet or exceed 48dp height where practical; compact overlay chips may be visually smaller but must sit in larger tap regions when promoted to production.

## Elevation & Depth

Use Material 3 surface containers rather than arbitrary translucency. Elevation is represented by surface luminance steps plus 1px outline borders. Overlay panels use stronger elevation because they float above other apps.

## Shapes

- Small: 8–12px for rails/icons.
- Medium: 16px for buttons/inputs.
- Large: 24–28px for cards and floating panels.
- Pill/circle: agent bubbles, status dots, and compact chips.

## Components

- **Activity rail:** 56px wide, dark surface, selected item uses primary container tint.
- **Status pill:** high-contrast label/value chip.
- **Floating command button:** one small circular always-on-top affordance with a status dot; first tap unfolds controls rather than opening a large panel immediately.
- **Quick-action tray:** compact attached list with obvious Monica/Messenger-style rows for Live chat, Agents, Lists, and Settings.
- **Agent checkpoint card:** title, ID, status, selected outline, avatar/status dot, and a View affordance that animates into the selected agent view.
- **Floating agent view:** title/status header, transcript bubbles, anchored composer, explicit ↗/⚙ full-screen affordances.
- **Agentic chat controls:** model picker in the header; composer `+` opens user-language actions such as Deep answer / Use web / Use files; activity details expand only on demand.
- **Slash-command palette:** `/commands`, `/skills`, `/reason`, `/tools` appear only when the composer starts with `/`, never as persistent transcript clutter.
- **Activity disclosure:** reasoning and tool calls show as a compact “Hermes is working / Activity” row with a plain checklist, not as fake chat participants.

## Do's and Don'ts

Do:
- Prefer Material 3 color roles, typography roles, shapes, and 48dp touch-target conventions.
- Keep the app dark-mode-first.
- Keep the initial hover affordance as a tiny circular icon; unfold to an attached action tray before opening larger agent cards or views.
- Keep all full-screen transitions explicit.

Don't:
- Revert to a light workbench unless explicitly requested.
- Let dark surfaces collapse into pure black with no hierarchy.
- Use orange as general decoration; reserve it for agent/Replit-flavored emphasis.
- Hide full-screen escalation behind the whole row if title/gear/expand is clearer.
