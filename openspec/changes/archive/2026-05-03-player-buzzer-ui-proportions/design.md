## Context

The player buzzer page is a single HTML file rendered inline in `PageRoute.kt`. The `#buzz-section` uses a CSS flexbox column to stack BUZZ and SKIP buttons, each with `flex: 1` (equal halves). The DOM currently orders BUZZ above SKIP.

Current layout (top→bottom): BUZZ (50%) → player-strip → SKIP (50%)

Target layout: SKIP (33%) → player-strip → BUZZ (67%)

## Goals / Non-Goals

**Goals:**
- SKIP button occupies the top 1/3 of the viewport
- BUZZ button occupies the bottom 2/3 of the viewport
- player-strip stays between them

**Non-Goals:**
- Changing button colors, labels, or interaction behavior
- Changing the join screen
- Responsive breakpoints or orientation-specific layouts

## Decisions

**Reorder DOM elements and reassign flex weights**

Swap the `#buzz` and `#skip` button order in the HTML so SKIP comes first (top), then player-strip, then BUZZ (bottom). Change `flex` from `1` (equal) to `1` for SKIP and `2` for BUZZ — achieving a 1:2 ratio (≈33%/67%).

Alternative considered: CSS `order` property to reorder without touching HTML. Rejected — reordering the DOM directly is simpler and keeps the source order readable.

## Risks / Trade-offs

- [Risk] Users accustomed to the old layout may tap the wrong button once. → Mitigation: Button labels and colors remain unchanged; muscle memory adapts quickly.
