## Context

The player buzzer page (`PageRoute.kt`) serves a single-page HTML app over Ktor. After joining, players see `#buzz-section` with BUZZ and SKIP buttons inside a narrow `main` card (`min(92vw, 36rem)` wide, `padding: 2rem`). On a phone this leaves buttons at roughly 150–200px tall — too small for quick taps during gameplay.

The entire page is rendered as an inline Kotlin string; there are no external CSS/JS files to update.

## Goals / Non-Goals

**Goals:**
- Make BUZZ and SKIP each fill half the viewport height and the full viewport width after joining
- Keep the join form in its current centered card layout (only the post-join view changes)

**Non-Goals:**
- Redesigning the join form or adding animations
- Making the page responsive for desktop/tablet (it's a phone buzzer)
- Adding new game-state feedback beyond what exists today

## Decisions

**Full-screen split via flexbox on `#buzz-section`**

When `#buzz-section` becomes visible, it takes over the full viewport. Each button is a `flex: 1` child with `width: 100%`, giving a natural 50/50 split. The `body` switches from `place-items: center` grid to a flex column when the buzz section is shown.

Alternative considered: absolute positioning with `height: 50vh` per button. Rejected — flexbox is simpler and adapts if the browser chrome changes the available height.

**Hide `main` card when buzz section is active**

Rather than trying to expand the card, `main` is hidden after a successful join and `#buzz-section` is shown as a sibling of `main` at the body level. This avoids fighting the card's `width` and `border-radius` constraints.

Alternative: expand the card to full-screen. Rejected — the card's rounded corners and shadow look odd full-screen; cleaner to drop it for the active state.

**Player name and status stay visible**

A small header strip at the top of the buzz section shows the player name and the status message so players have confirmation they're connected.

## Risks / Trade-offs

- [Status text placement] Placing status inside the split layout means it overlays one of the buttons briefly → Mitigation: show it as an overlay or in a small strip between the two buttons (non-interactive area).
- [iOS Safari viewport height] `100vh` can include the browser toolbar on iOS, causing layout shift → Mitigation: use `100dvh` (dynamic viewport height) with a `100vh` fallback.

## Migration Plan

Single-file change to `PageRoute.kt`. No server restart required beyond a normal redeploy; players already on the page will see the old layout until they reload.
