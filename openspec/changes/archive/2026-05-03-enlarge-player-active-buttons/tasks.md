## 1. HTML Structure

- [x] 1.1 Move `#buzz-section` out of `<main>` and make it a direct child of `<body>` (sibling to `<main>`)
- [x] 1.2 Add a player-name/status strip element inside `#buzz-section` between the two buttons

## 2. CSS

- [x] 2.1 Style `#buzz-section` as a full-viewport flex column (`width: 100vw`, `height: 100dvh` with `100vh` fallback, `position: fixed`, `top: 0`, `left: 0`)
- [x] 2.2 Style BUZZ and SKIP as `flex: 1` children with `width: 100%`, large font size (`clamp(2.5rem, 8vw, 5rem)`), no border-radius or a small one, and keep existing color gradients
- [x] 2.3 Style the name/status strip as a small fixed-height bar between the two buttons (non-interactive)
- [x] 2.4 Set `#buzz-section` to `display: none` initially (unchanged) and `display: flex` when shown

## 3. JavaScript

- [x] 3.1 On successful join: hide `<main>` (`display: none`) and show `#buzz-section` (`display: flex`)
- [x] 3.2 Update `#greeting` and `#status` references to use the new name/status strip element
