---
version: alpha
name: Agent Overlay Workbench
description: Replit Agent inspired mobile workbench for floating Hermes agent bubbles.
colors:
  primary: "#1F2328"
  secondary: "#5F6368"
  tertiary: "#F26207"
  neutral: "#F3F2ED"
  surface: "#FAF9F4"
  surfaceRaised: "#FFFFFF"
  surfaceMuted: "#EDEBE3"
  border: "#D8D6CC"
  active: "#6C5CE7"
  success: "#2DA44E"
  warning: "#B7791F"
  info: "#3B82F6"
typography:
  h1:
    fontFamily: Inter
    fontSize: 1.75rem
    fontWeight: 650
    lineHeight: 1.05
    letterSpacing: "-0.035em"
  h2:
    fontFamily: Inter
    fontSize: 1.125rem
    fontWeight: 650
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  body-md:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: 450
    lineHeight: 1.45
  label:
    fontFamily: Inter
    fontSize: 0.75rem
    fontWeight: 650
    lineHeight: 1.1
  mono:
    fontFamily: JetBrains Mono
    fontSize: 0.75rem
    fontWeight: 500
    lineHeight: 1.35
rounded:
  sm: 8px
  md: 14px
  lg: 20px
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
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    padding: 12px
  button-secondary:
    backgroundColor: "{colors.surfaceRaised}"
    textColor: "{colors.primary}"
    rounded: "{rounded.md}"
    padding: 12px
  agent-bubble:
    backgroundColor: "{colors.active}"
    textColor: "#FFFFFF"
    rounded: "{rounded.pill}"
    size: 56px
  panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.primary}"
    rounded: "{rounded.xl}"
    padding: 14px
  input:
    backgroundColor: "{colors.surfaceRaised}"
    textColor: "{colors.primary}"
    rounded: "{rounded.md}"
    padding: 12px
---

## Overview

Agent Overlay uses a Replit-Agent-inspired workbench model: a calm cream workspace, a narrow activity rail, compact tool/status chrome, checkpoint-like agent cards, and an always-available agent composer. The product remains overlay-first: individual agents live as Messenger-style floating bubbles; the full-screen Activity exists for settings, roster, diagnostics, and command-link management.

Replit notes applied:

- Left activity rail with icon-only destinations.
- Top status pill showing whether the agent is resting/running.
- Workspace tabs / chips for Preview, Database, Deployments, Git, and Console metaphors.
- Agent cards use checkpoint/status language and compact secondary actions.
- Composer is anchored, rounded, and low-friction.
- Preview/settings surfaces use large calm cards with thin gray-oat borders.

## Colors

- **Primary (#1F2328):** Dense ink for readable workspace text.
- **Neutral (#F3F2ED):** Warm app chrome background, inspired by Replit's light editor shell.
- **Surface (#FAF9F4):** Main panels and cards.
- **Surface Raised (#FFFFFF):** Inputs and elevated preview cards.
- **Tertiary (#F26207):** Replit-like orange accent for active/agent-specific affordances.
- **Active (#6C5CE7):** Agent bubble identity and selected states.
- **Border (#D8D6CC):** Soft oat border; avoid cold blue-gray dividers.

## Typography

Use Inter/system sans throughout. Hierarchy comes from size, weight 650 for headings/labels, and slightly negative display tracking. Use monospace only for IDs, gateway URLs, and low-level diagnostics.

## Layout

- Full-screen app: rail + workbench column on phones/tablets where space allows; stack cards vertically on narrow screens.
- Overlay: keep compact, dark, and high-contrast over arbitrary apps.
- Primary flow: bubble → agent heads → floating chat. Full-screen only via title/gear/expand.
- Inputs should be anchored at the bottom of chat surfaces and use minimum 44px touch targets.

## Elevation & Depth

Use border-first depth. The workbench is mostly flat with subtle raised white cards and 1px oat borders. Floating overlays may use dark panels and stronger elevation because they sit above other apps.

## Shapes

- Large cards: 20–28px radius.
- Inputs/buttons: 14px radius.
- Agent bubbles and status chips: pill/circle.

## Components

- **Activity rail:** 44–52px wide, icon-only, selected item uses active accent.
- **Status pill:** rounded command/status capsule near the top.
- **Agent checkpoint card:** title, status, short transcript/summary, compact Chat and Changes controls.
- **Floating chat:** title/status header, transcript bubbles, anchored composer, explicit ↗/⚙ full-screen affordances.

## Do's and Don'ts

Do:
- Keep the main app calm, cream, and workbench-like.
- Keep agents as bubbles and floating chats by default.
- Use checkpoint/status language instead of dashboard language.
- Keep all full-screen transitions explicit.

Don't:
- Revert to dashboard-first UX.
- Hide full-screen escalation behind the whole row if a title/gear/expand affordance is clearer.
- Use pure white/black everywhere; preserve warm Replit-like surfaces.
- Let overlay controls become low-contrast over the underlying app.
