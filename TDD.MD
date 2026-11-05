# GaaO — Technical Design Document (TDD)

**Version:** 1.0  
**Platform:** Android  
**Language:** Kotlin  
**IDE:** VS Code  
**Coding Assistant:** Gemini  
**Architecture:** Native Android, event-driven, modular

## 1. Architecture Overview

GaaO will use a simple layered architecture.

The application will have four primary modules:

1. Media Detection
2. Lyrics Retrieval
3. Lyrics Synchronization
4. User Interface

The modules will communicate through well-defined interfaces and observable application state.

### High-level architecture

```text
             EXTERNAL MUSIC APPS
             ┌──────────────────┐
             │  YouTube Music   │
             │     Spotify      │
             └────────┬─────────┘
                      │
                      ▼
             ANDROID MEDIA SYSTEM
             ┌──────────────────┐
             │ Notification     │
             │ Listener Service│
             └────────┬─────────┘
                      │
                      ▼
             MEDIA DETECTION LAYER
             ┌──────────────────┐
             │ MediaSession     │
             │ Manager          │
             │ MediaController  │
             └────────┬─────────┘
                      │
                      ▼
                APP STATE
                      │
             ┌────────┴─────────┐
             ▼                  ▼
       LYRICS LAYER       PLAYBACK STATE
             │                  │
             ▼                  │
          LRCLIB API            │
             │                  │
             ▼                  │
       LYRICS PARSER            │
             │                  │
             └────────┬─────────┘
                      ▼
             SYNCHRONIZATION ENGINE
                      │
                      ▼
                 UI LAYER
             ┌──────────────────┐
             │ Jetpack Compose  │
             │ Lyrics Display   │
             │ Smooth Scrolling │
             └──────────────────┘
```

## 2. Module Design

### 2.1 Media Detection Module

**Responsibility:** Identify currently active music sessions and expose relevant playback information.

Main Android APIs:

- MediaSessionManager
- MediaController
- NotificationListenerService
- MediaController.Callback

Responsibilities:

- Detect active sessions.
- Identify the relevant music application.
- Read song metadata.
- Monitor playback state changes.
- Monitor track changes.
- Read playback position and speed.
- Register and unregister callbacks appropriately.

The application shall wait for `NotificationListenerService.onListenerConnected()` before accessing active sessions.

The notification-listener service shall be declared in AndroidManifest.xml with the required binding permission.

Only relevant media-session metadata shall be processed.

### 2.2 Lyrics Retrieval Module

**Responsibility:** Retrieve suitable synchronized lyrics.

Provider:

LRCLIB API — https://lrclib.net/docs

Primary endpoint:

`GET /api/get`

Required parameters:

- track_name
- artist_name

Recommended parameter:

- duration

Optional parameter:

- album_name

The application shall use track metadata to request the corresponding lyrics.

Expected response fields:

- trackName
- artistName
- albumName
- duration
- plainLyrics
- syncedLyrics
- instrumental

The application shall prioritize `syncedLyrics`.

The application shall not display unsynchronized lyrics as if they were timestamped.

### 2.3 Lyrics Parser

**Responsibility:** Convert LRC-formatted lyrics into structured lyric lines.

Each parsed lyric entry shall contain:

- Timestamp in milliseconds.
- Lyric text.
- Sequence/order.

Conceptual data structure:

```text
LyricLine
 ├── timestamp: Long
 └── text: String
```

The parser shall support:

- Standard LRC timestamps.
- Multiple timestamps for one lyric line.
- Empty or malformed lines.
- Chronological ordering.

Invalid timestamps shall be ignored safely.

### 2.4 Synchronization Engine

**Responsibility:** Match playback position with the correct lyric line.

Inputs:

- Current playback position.
- Playback speed.
- Playback state.
- Parsed lyric timestamps.

Expected behaviour:

1. Identify the current playback position.
2. Find the latest lyric timestamp not greater than that position.
3. Mark that lyric as active.
4. Update the active line when playback progresses.
5. Freeze the active position when paused.
6. Recalculate immediately after seeking.

The engine shall use Android's reported playback-position timestamp to estimate progress between system updates.

It shall not continuously query the media application.

### 2.5 User Interface Module

**Framework:** Jetpack Compose

Responsibilities:

- Display current lyrics.
- Display previous and upcoming lines.
- Animate vertical scrolling.
- Highlight the active line.
- Render idle, loading, offline and unavailable states.

The UI shall observe application state rather than directly communicate with Android media APIs.

## 3. Recommended Package Structure

```text
com.gaao.app/
│
├── data/
│   ├── media/
│   │   ├── MediaSessionRepository.kt
│   │   └── MediaNotificationListener.kt
│   │
│   ├── lyrics/
│   │   ├── LyricsApi.kt
│   │   ├── LyricsRepository.kt
│   │   ├── LyricsResponse.kt
│   │   └── LrcParser.kt
│   │
│   └── model/
│       ├── Track.kt
│       └── LyricLine.kt
│
├── domain/
│   ├── SynchronizationEngine.kt
│   └── TrackMatcher.kt
│
├── presentation/
│   ├── GaaOScreen.kt
│   ├── LyricsViewModel.kt
│   ├── LyricsState.kt
│   └── components/
│       ├── LyricsDisplay.kt
│       └── StateMessage.kt
│
├── MainActivity.kt
│
└── GaaOApplication.kt
```

This is a proposed structure, not a requirement to create every file immediately.

## 4. Application State Management

The application shall use a single observable UI state managed by the ViewModel.

Suggested states:

```text
IDLE
DETECTING
LOADING_LYRICS
PLAYING
PAUSED
OFFLINE
LYRICS_UNAVAILABLE
ERROR
```

State changes shall be driven by media-session events, network results and application lifecycle events.

Kotlin Coroutines and StateFlow may be used for asynchronous work and state propagation.

## 5. Media Session Lifecycle

The application shall:

1. Register its notification-listener service.
2. Wait until the listener is connected.
3. Retrieve currently active media sessions.
4. Select the relevant playing session.
5. Register a MediaController callback.
6. Observe metadata and playback changes.
7. Replace callbacks when the active session changes.
8. Release callbacks and references when no longer needed.

On application reopening, GaaO shall recheck active sessions rather than assuming the previously observed track is still playing.

The application shall handle listener disconnection gracefully.

## 6. Session Selection Strategy

Multiple media sessions may exist simultaneously.

The initial implementation shall:

1. Prefer an actively playing session.
2. Prefer supported music applications.
3. Use Android's session priority as a selection input.
4. Avoid treating paused or stale sessions as currently playing.
5. Re-evaluate selection when playback changes.

Supported application package identifiers shall be maintained in one configuration location, not scattered across the codebase.

## 7. Lyrics Retrieval Strategy

The retrieval sequence shall be:

1. Receive new track metadata.
2. Validate the title and artist.
3. Extract duration when available.
4. Request a matching LRCLIB record.
5. Validate returned metadata.
6. Verify that synchronized lyrics are present.
7. Parse LRC timestamps.
8. Send parsed lyrics to the synchronization engine.

If the returned result is ambiguous or mismatched, it shall not be presented as a confirmed match.

Only one request should be active for a given track lookup.

Repeated metadata callbacks for the same track shall not cause repeated API requests.

The client shall identify itself in HTTP requests and respect HTTP 429 responses and their Retry-After headers.

## 8. Synchronization Design

The synchronization engine shall determine the active line using a timestamp comparison.

Conceptually:

`activeLine = latest lyric whose timestamp <= playbackPosition`

Playback position shall be estimated as:

`estimatedPosition = reportedPosition + elapsedTime × playbackSpeed`

The calculation shall only advance when playback is actively progressing.

When the player reports pause, buffering or stopped states, the engine shall stop advancing the lyric position.

A new reported position shall correct any accumulated timing difference.

The UI animation shall follow the active line without unnecessarily recomposing the entire screen.

## 9. Networking and Error Handling

Technology:

- OkHttp
- Kotlin Serialization
- Kotlin Coroutines

Expected handling:

| Condition | Handling |
|---|---|
| HTTP 200 | Parse lyrics |
| HTTP 404 | Lyrics unavailable |
| HTTP 429 | Respect Retry-After |
| HTTP 500 | Show appropriate error |
| HTTP 503 | Retry according to server guidance |
| Timeout | Handle without crashing |
| No internet | Display offline state |
| Invalid response | Reject safely |

The application shall avoid unnecessary retries and repeated API calls.

## 10. Dependencies

Keep the dependency list minimal.

Core dependencies:

- AndroidX Activity Compose
- Jetpack Compose BOM
- AndroidX Lifecycle Runtime Compose
- AndroidX Lifecycle ViewModel Compose
- Kotlin Coroutines
- OkHttp
- Kotlinx Serialization JSON

No additional database, dependency-injection framework or networking framework is required for the initial MVP.

Use compatible stable releases when initializing the project and maintain versions through Gradle's version catalog.

## 11. Build and Development Environment

Primary editor:

VS Code

Android development requirements:

- JDK compatible with the selected Android Gradle Plugin.
- Android SDK.
- Android SDK Platform Tools.
- Android Build Tools.
- Gradle Wrapper.
- Android emulator or physical Android device.
- Android Studio for SDK and emulator management.

Gradle Wrapper shall be used for builds.

The application shall be buildable through the command line.

## 12. Logging and Debugging

During development, log:

- Media-session connection status.
- Detected package name.
- Track metadata.
- Playback state changes.
- Playback position.
- Lyrics request status.
- Lyrics matching result.
- Synchronization updates.
- Error details.

Avoid logging complete lyrics or unrelated notification contents.

Development logs shall be removable or reduced before release.

## 13. Design Constraints

- No backend server.
- No user authentication.
- No database initially.
- No permanent foreground service.
- No audio recording.
- No microphone permission.
- No unnecessary background polling.
- No manual song selection.
- No unnecessary third-party dependencies.

## 14. Technical Risks

| Risk | Mitigation |
|---|---|
| Missing media metadata | Handle incomplete metadata |
| Multiple active sessions | Implement session-selection logic |
| Missing timestamps | Show lyrics unavailable |
| Incorrect song matching | Validate title, artist and duration |
| Playback position drift | Resynchronize with controller updates |
| Listener disconnection | Recheck sessions after reconnection |
| Android battery restrictions | Test on physical devices |
| API rate limiting | Throttle requests and honor Retry-After |
| Lyrics provider unavailable | Display a clear error state |

## 15. Architecture Acceptance Criteria

The architecture shall be considered validated when:

1. Supported music sessions are detected.
2. Metadata is extracted correctly.
3. Playback changes are observed.
4. Position updates are accessible.
5. The application handles session replacement.
6. Lyrics requests return valid responses.
7. LRC timestamps are parsed correctly.
8. Synchronization produces the expected active line.
9. The app does not require continuous polling.
10. The app remains stable during background and foreground transitions.

**End of Technical Design Document.**