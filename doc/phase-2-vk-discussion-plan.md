# Phase 2 — VK Discussion Pack Source

**Goal:** Allow users to browse and download newly published SIQ quiz packs from a VK (Vkontakte) discussion thread, directly within the pack browser dialog.

**Status:** Plan only — not implemented.

---

## Overview

VK is the primary distribution hub for the Russian "Svoya Igra" (SIGame) community. Pack authors share `.siq` files as document attachments in VK discussion threads (often in the official SIGame community or related groups). Phase 2 adds a "From VK" tab to the pack browser dialog that:

1. Fetches posts/comments from a configured VK discussion thread
2. Scans for `.siq` document attachments
3. Displays them with pack metadata (name, author, difficulty, rounds) 
4. Lets the user download selected packs to `~/Downloads`
5. Scanned packs then appear in the local "Downloads" tab

---

## Architecture

### New Dependencies

Add to `composeApp/build.gradle.kts`:

```kotlin
implementation("io.ktor:ktor-client-core:2.3.12")
implementation("io.ktor:ktor-client-cio:2.3.12")   // CIO engine — already used in server module
implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
```

*Rationale:* Ktor client is the natural choice — the project already uses Ktor server (CIO), so we stay in the same ecosystem. OkHttp could work too but adds a new dependency family.

### New Files

```
composeApp/src/desktopMain/kotlin/app/session/
├── VkSource.kt             # VK API client + data models
├── VkPackDownloader.kt     # Downloads .siq files to ~/Downloads
└── PackIndex.kt            # Local index of known packs (avoid re-downloading)
```

### Modified Files

| File | Change |
|------|--------|
| `PackBrowserDialog.kt` | Add tab bar: "Downloads" \| "From VK". VK tab shows fetchable packs |
| `DesktopSessionController.kt` | Add VK source state, `fetchFromVk()`, `downloadFromVk()` |
| `UiState.kt` | Add `vkPacks`, `isFetchingVk`, `vkTabActive` |
| `PackScanner.kt` | Add `scanDirectory()` overload or configurable path |

---

## Detailed Design

### 1. VK API Client (`VkSource.kt`)

#### VK API Methods Needed

| Method | Purpose |
|--------|---------|
| `board.getComments` | Fetch comments from a discussion thread (topic) — each comment may contain .siq attachments |
| `wall.get` | Alternative: fetch posts from a community wall with .siq attachments |
| `docs.getWallUploadServer` | Not needed for reading, only if we want to *upload* packs |

#### Data Models (kotlinx.serialization)

```kotlin
@Serializable
data class VkResponse<T>(val response: VkResponseData<T>)

@Serializable
data class VkResponseData<T>(val items: List<T>, val count: Int)

@Serializable
data class VkComment(
    val id: Int,
    val from_id: Int,
    val date: Long,
    val text: String,
    val attachments: List<VkAttachment>? = null,
)

@Serializable
data class VkAttachment(
    val type: String,  // "doc"
    val doc: VkDoc? = null,
)

@Serializable
data class VkDoc(
    val id: Int,
    val owner_id: Int,
    val title: String,
    val ext: String,
    val size: Int,
    val url: String,
    val date: Long,
)
```

#### API Client Class

```kotlin
class VkSource(
    private val accessToken: String,
    private val ownerId: Long,   // e.g. -123456789 (negative = community)
    private val topicId: Int,    // discussion topic ID
    private val httpClient: HttpClient = HttpClient(CIO),
) {
    suspend fun fetchComments(offset: Int = 0, count: Int = 100): List<VkComment>
    
    suspend fun fetchSiqAttachments(): List<VkDoc> {
        // 1. Paginate through comments
        // 2. Filter attachments where type == "doc" && ext == "siq"
        // 3. Return VkDoc list sorted by date descending
    }
}
```

#### VK API Limitations

- **Rate limit:** ~3 requests per second per service. Implement throttling.
- **Access token:** Requires a standalone VK app registration (`https://dev.vk.com/`). The token needs `wall`, `groups`, `docs` permissions.
- **Token lifetime:** User tokens expire. Service tokens (for community apps) last indefinitely but are read-only for public content.
- **Pagination:** `board.getComments` returns max 100 per page; use `offset` parameter.
- **Privacy:** Private communities are inaccessible without membership.

### 2. Pack Index (`PackIndex.kt`)

Purpose: Track which packs have already been downloaded to avoid duplicates.

```kotlin
data class KnownPack(
    val source: Source,     // VK
    val sourceId: String,   // "vk_doc_12345_678" (ownerId_docId)
    val fileName: String,
    val downloadedAt: Long,
    val localPath: String,
)

class PackIndex(private val indexFile: Path = /* ~/.sick/pack-index.json */) {
    fun isKnown(sourceId: String): Boolean
    fun markDownloaded(pack: KnownPack)
    fun listDownloaded(): List<KnownPack>
}
```

Stored as a simple JSON file at `~/.sick/pack-index.json`.

### 3. Downloader (`VkPackDownloader.kt`)

```kotlin
class VkPackDownloader(
    private val downloadDir: Path = Paths.get(System.getProperty("user.home"), "Downloads"),
    private val httpClient: HttpClient = HttpClient(CIO),
) {
    suspend fun download(vkDoc: VkDoc): Result<Path> {
        // 1. Check PackIndex to avoid re-download
        // 2. Download from vkDoc.url using Ktor client
        // 3. Save to downloadDir/vkDoc.title (with dedup suffix if exists)
        // 4. Update PackIndex
        // 5. Return local Path
    }
}
```

### 4. UI Changes (PackBrowserDialog.kt)

Convert the dialog to a tabbed layout:

```
┌─────────────────────────────────────────────────┐
│  Browse Packs                                    │
│  ~/Downloads                                     │
│                                                  │
│  [ Downloads ]  [ From VK ]   [Refresh] [Close] │
├──────────────────────────────────────────────────┤
│                                                  │
│  (Downloads tab — existing content)              │
│  or                                               │
│  (VK tab content — see below)                    │
│                                                  │
└──────────────────────────────────────────────────┘
```

**VK Tab Layout:**

```
┌─────────────────────────────────────────────────┐
│ Settings: [VK Group ID] [Topic ID] [Token]       │
│ Configure in Settings → [Open Settings]          │
│                                                  │
│ [Connect] [Fetch]  Status: Connected (42 packs)  │
├──────────────────────────────────────────────────┤
│                                                  │
│ ┌─────────────────────────────────────────────┐  │
│ │ Pack: "Quiz Pack #42"  by @author          │  │
│ │ Difficulty: Medium | 5 rounds, 120 q       │  │
│ │ [Peek ▾] [Download] (already downloaded ✓) │  │
│ └─────────────────────────────────────────────┘  │
│                                                  │
│ ┌─────────────────────────────────────────────┐  │
│ │ Pack: "Another Pack"  by @creator          │  │
│ │ Difficulty: Hard | 3 rounds, 75 q          │  │
│ │ [Peek ▾] [Download]                        │  │
│ └─────────────────────────────────────────────┘  │
│                                                  │
│              [Download All New]                   │
└─────────────────────────────────────────────────┘
```

**Key UX details:**
- Before first use, the user must configure VK credentials (Group ID, Topic ID, Access Token)
- A "Settings" section at the top of the VK tab (collapsible) for configuration
- Status indicator: "Connected" / "Needs configuration" / "Error: invalid token"
- Each VK pack row shows: title, author (from the post), date, file size
- Uses the same `Peek` inline expansion as the Downloads tab (reuse the same `PackRow`-style component)
- "Peek" needs to scan the `.siq` from the download URL — either download temporarily to scan, or scan after download
- Downloaded packs auto-appear in the Downloads tab after refresh
- "Download All New" batch downloads all non-duplicate packs

### 5. Controller Changes (DesktopSessionController.kt)

```kotlin
// New state
fun showVkSettings()
fun updateVkConfig(groupId: String, topicId: String, token: String)
fun connectToVk()
fun fetchVkPacks()
fun downloadVkPack(vkDocId: String)
fun downloadAllNewVkPacks()

// State carried through publishState()
vkConnected: Boolean,
vkPacks: List<VkPackItem>,
isFetchingVk: Boolean,
vkConfig: VkConfig?,
vkError: String?,
```

---

## Implementation Steps

### Step 1 — Add Dependencies
- Add Ktor client + kotlinx-serialization to `composeApp/build.gradle.kts`
- Add serialization plugin to root `build.gradle.kts` or `composeApp/build.gradle.kts`

### Step 2 — Create VK Data Models
- `VkSource.kt` with all `@Serializable` data classes
- Unit tests for JSON deserialization with sample VK API responses

### Step 3 — Implement VK API Client
- `VkSource` class with `fetchSiqAttachments()` 
- Pagination support (loop through `offset`)
- Throttling (delay between requests)
- Error handling (invalid token, network errors, rate limits)

### Step 4 — Create PackIndex
- `PackIndex` class with JSON file persistence
- Read/write index file, dedup logic

### Step 5 — Implement Downloader
- `VkPackDownloader` with `download()` method
- Progress tracking (optional — show download progress in UI)
- Handle filename conflicts (append number suffix)

### Step 6 — Add VK Configuration UI
- Settings form in the VK tab: Group ID, Topic ID, Access Token
- Save config to `~/.sick/vk-config.json`
- "Connect" button tests the connection

### Step 7 — Add VK Tab to PackBrowserDialog
- Tab bar with "Downloads" / "From VK" tabs
- VK pack list with Peek/Download buttons
- "Download All New" batch action
- Status indicators for connection state

### Step 8 — Wire Controller Methods
- `DesktopSessionController` methods for VK operations
- State management through `UiState`
- Background fetching on `Dispatchers.IO`

### Step 9 — Integration Testing
- Test with a real VK community/topic (need a test VK app token)
- Mock VK API for unit tests
- Verify download → scan → appear in Downloads tab flow

---

## Future Considerations (Post-Phase 2)

### Multiple VK Sources
Allow the user to configure multiple VK group/topic combinations. Each becomes a separate source tab or a merged feed.

### Auto-Refresh
Periodically check VK sources for new packs (e.g., on app startup or every hour). Show notification badge.

### Alternative Sources
The same architecture can support:
- **Telegram channels** (similar API via `tdlib` or Bot API)
- **RSS feeds** from pack aggregators
- **GitHub releases** from SIGame pack repositories

### Caching & Offline
Cache fetched VK pack metadata locally so the list is available without network. Show "cached N packs" indicator.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| VK API changes | Use a stable API version (`v=5.199`), pin the version in requests |
| Access token security | Store token in `~/.sick/vk-config.json` with restrictive file permissions (0600) |
| Large discussion threads (1000+ comments) | Implement efficient pagination with progress indicator; allow cancellation |
| Download failures/resume | Implement retry with exponential backoff; track partially downloaded files |
| VK being blocked in some regions | Make the source pluggable — users can configure proxy or alternate sources |
| File already exists in Downloads | Auto-rename with suffix (`pack (2).siq`), detect via PackIndex |

---

## Estimated Effort

| Component | Est. Time |
|-----------|-----------|
| Dependencies + serialization setup | 0.5h |
| VK API client + data models | 2h |
| PackIndex persistence | 1h |
| Downloader | 1h |
| VK tab UI + configuration | 3h |
| Controller wiring + state | 1.5h |
| Testing (unit + manual) | 2h |
| **Total** | **~11h** |

---

## Questions for Discussion

1. **Which VK group/discussion should be the default?** The official SIGame community (`public/sigame`) or a specific SIQ pack aggregation group?
2. **Should VK config be a settings file or a UI form?** Initially UI form + saved JSON file.
3. **Do we need a "Download All" batch mode or is per-pack selection sufficient?** Both — per-pack + a batch button.
4. **Should we also support `wall.get` as an alternative source** (wall posts vs discussion boards)? Discussion threads are more structured, but some groups share packs on the wall.
