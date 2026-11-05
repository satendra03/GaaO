# GaaO
## Software Requirements Specification (SRS)

**Product:** GaaO (गाओ)  
**Tagline:** Listen. Follow. Sing.  
**Document Version:** 1.0  
**Date:** 5 November 2026  
**Platform:** Android  
**Project Type:** Personal Utility Application  
**Status:** MVP Specification

---

# 1. Introduction

## 1.1 Purpose

GaaO is a lightweight Android application designed to automatically display synchronized lyrics for music playing through other music applications.

The application aims to provide a distraction-free singing experience by displaying lyrics in a cinematic, vertically scrolling interface.

Users should not need to search for songs, manually select tracks or control music playback through GaaO.

The primary objective is simple:

**Open GaaO, automatically detect the currently playing song, and display its synchronized lyrics.**

## 1.2 Product Vision

GaaO is not a music player or streaming platform. It is a dedicated lyrics companion that works alongside existing music applications.

The application should be:

- Simple and lightweight.
- Free for personal use.
- Automatic, with no manual song selection.
- Visually minimal and immersive.
- Battery-conscious.
- Independent of unnecessary backend infrastructure.

## 1.3 Target Users

Initially, GaaO is intended for personal use by the developer.

It is designed for people who listen to music while travelling, walking, working or performing everyday activities and enjoy singing along.

---

# 2. Scope

## 2.1 In-Scope Features

The MVP will include:

1. Automatic detection of currently playing music.
2. Support for YouTube Music and Spotify.
3. Automatic song identification using metadata.
4. Automatic retrieval of timestamped lyrics.
5. Playback-position-based lyric synchronization.
6. Smooth vertical scrolling.
7. Current-line highlighting.
8. Automatic handling of song changes.
9. Pause, resume and seek synchronization.
10. Background media-session monitoring.
11. Internet connectivity handling.
12. Idle and loading screens.

## 2.2 Out-of-Scope Features

The following features are excluded from the initial release:

- Music streaming or playback.
- Playlist management.
- User registration and authentication.
- Social sharing.
- Lyrics translation.
- Manual song search.
- Floating lyrics overlay.
- Custom themes and extensive customization.
- Offline lyrics downloads.
- Cloud synchronization.
- Advertising and analytics.

---

# 3. Functional Requirements

## FR-01: Automatic Music Detection

The application shall automatically identify active music sessions from supported music applications.

Supported applications:

- YouTube Music
- Spotify

The application shall use Android's MediaSessionManager and MediaController APIs.

It shall identify the currently relevant playing session using playback state and session metadata.

## FR-02: Song Identification

The application shall retrieve available metadata, including:

- Song title.
- Artist name.
- Album name, when available.
- Track duration, when available.
- Current playback position.
- Playback state.

The metadata shall be used to identify the corresponding lyrics.

## FR-03: Automatic Lyrics Retrieval

The application shall automatically retrieve lyrics using the LRCLIB API.

The initial implementation shall prioritize timestamped lyrics.

The application shall match songs using available metadata, particularly title, artist and duration.

If matching synchronized lyrics are unavailable, the application shall display an appropriate unavailable message rather than showing incorrectly synchronized lyrics.

## FR-04: Lyrics Synchronization

The application shall synchronize displayed lyrics with the playback position reported by Android's media session.

The synchronization engine shall:

- Identify the currently active lyric line.
- Highlight the active line.
- Display previous and upcoming lines.
- Update the active line when playback progresses.
- Recalculate the lyric position after seeking.

Playback position shall be estimated using the last reported position, playback speed and elapsed time, with subsequent updates used for resynchronization.

## FR-05: Automatic Vertical Scrolling

Lyrics shall move vertically in a smooth, cinematic scrolling animation.

The current lyric shall remain visually prominent.

Previous and upcoming lines shall appear with reduced opacity.

Scrolling shall automatically follow playback progress.

## FR-06: Playback State Management

The application shall respond to playback events.

| Event | Expected behaviour |
|---|---|
| Music starts | Detect song and retrieve lyrics |
| Music pauses | Freeze lyrics |
| Music resumes | Resume synchronization |
| Next song | Retrieve new lyrics |
| Previous song | Retrieve previous song's lyrics |
| Forward seek | Update lyric position |
| Backward seek | Update lyric position |
| Music stops | Display idle screen |
| Active session changes | Identify the new playing session |

## FR-07: Background Monitoring

The application shall be capable of monitoring relevant media-session changes when it is not in the foreground, provided Android maintains its notification-listener connection.

The application shall:

- Avoid continuous polling.
- Avoid unnecessary foreground services.
- Avoid recording audio.
- Avoid microphone access.
- Recheck active media sessions when reopened or resumed.

The lyrics interface shall be displayed only when GaaO is opened. The application shall not force itself over other applications.

## FR-08: Internet Connectivity

An active internet connection is required to retrieve lyrics.

When connectivity is unavailable, the application shall display:

"No internet connection."

The MVP shall not attempt offline lyrics retrieval.

## FR-09: Automatic State Transitions

The application shall manage the following primary states:

1. IDLE
2. DETECTING
3. LOADING_LYRICS
4. PLAYING
5. PAUSED
6. OFFLINE
7. LYRICS_UNAVAILABLE

State transitions shall occur automatically based on media-session events and network responses.

---

# 4. User Interface Requirements

## 4.1 General Design

The application shall use a minimal, dark, distraction-free design.

Design characteristics:

- Pure black background.
- White active lyrics.
- Grey previous and upcoming lyrics.
- Smooth vertical scrolling.
- Minimal typography.
- No unnecessary navigation elements.
- Full-screen immersive display.

## 4.2 Playing Screen

The playing screen shall display:

- Previous lyric lines in faded text.
- Current lyric line prominently highlighted.
- Upcoming lyric lines in faded text.

The interface shall automatically follow the current playback position.

## 4.3 Idle Screen

When no active music is playing, the application shall display:

**GaaO**

"Waiting for music..."

## 4.4 Loading Screen

When a new song is detected and lyrics are being retrieved, the application shall display:

"Finding lyrics..."

## 4.5 Offline Screen

When internet connectivity is unavailable, the application shall display:

"No internet connection."

## 4.6 Lyrics Unavailable Screen

When no suitable timestamped lyrics are found, the application shall display:

"Lyrics unavailable for this track."

---

# 5. Technical Architecture

## 5.1 Technology Stack

| Component | Selected Technology |
|---|---|
| Programming Language | Kotlin |
| UI Framework | Jetpack Compose |
| UI Design | Material 3 with custom styling |
| Media Detection | MediaSessionManager |
| Playback Information | MediaController |
| Background Access | NotificationListenerService |
| Lyrics Provider | LRCLIB API |
| Networking | OkHttp |
| JSON Parsing | Kotlin Serialization |
| Local Database | Not required |
| Backend | Not required |
| Build System | Gradle with Kotlin DSL |
| IDE | Android Studio |

## 5.2 Architecture Components

### A. Media Detection Component

Responsible for identifying active media sessions and monitoring changes.

It shall use:

- MediaSessionManager
- NotificationListenerService
- MediaController

### B. Playback Monitoring Component

Responsible for tracking:

- Playing state.
- Paused state.
- Stopped state.
- Playback position.
- Playback speed.
- Track changes.

### C. Lyrics Retrieval Component

Responsible for communicating with LRCLIB and retrieving suitable synchronized lyrics.

### D. Synchronization Engine

Responsible for matching timestamps with playback position and determining which lyric line should be highlighted.

### E. UI Component

Responsible for rendering lyrics, animations and application states using Jetpack Compose.

## 5.3 Proposed Data Flow

1. User opens GaaO.
2. Application checks notification-listener access.
3. Application identifies active media sessions.
4. Application selects the relevant playing session.
5. Metadata is extracted.
6. Lyrics are requested from LRCLIB.
7. Timestamped lyrics are parsed.
8. Synchronization engine tracks playback position.
9. UI displays and scrolls lyrics automatically.
10. Playback events trigger appropriate updates.

---

# 6. External Interface Requirements

## 6.1 Android System Integration

GaaO shall integrate with Android's media-session infrastructure.

Notification-listener access must be explicitly enabled by the user through Android settings.

The application shall process only the relevant media-session information and shall not store or process unrelated notification content.

## 6.2 Network Integration

The application shall communicate with:

LRCLIB API: https://lrclib.net/docs

The API shall be used to retrieve song metadata matches and synchronized lyrics.

Requests shall identify the GaaO application appropriately and respect server rate limits, including Retry-After responses.

## 6.3 Permissions

| Permission / Access | Requirement |
|---|---|
| Internet | Required |
| Notification Listener Access | Required |
| Microphone | Not required |
| Audio Recording | Not required |
| Storage | Not required |
| Accessibility Service | Not required |
| Display Over Other Apps | Not required |
| Root Access | Not required |

---

# 7. Non-Functional Requirements

## 7.1 Performance

- Application should launch quickly.
- Lyrics should load without unnecessary delays.
- Scrolling should remain smooth.
- Playback synchronization should be responsive.
- Unnecessary background processing must be avoided.

## 7.2 Battery Efficiency

- No continuous media-position polling.
- No permanent foreground service.
- No unnecessary wake locks.
- Use event-driven media-session callbacks.
- Keep background processing minimal.

## 7.3 Reliability

- Handle missing metadata.
- Handle unavailable lyrics.
- Handle network failures.
- Handle music application switching.
- Recover playback state when GaaO is reopened.
- Handle paused and stopped sessions separately.

## 7.4 Privacy

- No audio recording.
- No microphone access.
- No user accounts.
- No analytics or tracking.
- No unnecessary data collection.
- No external storage of personal information.

## 7.5 Maintainability

The application shall use a modular architecture with separate responsibilities for media detection, lyrics retrieval, synchronization and UI rendering.

---

# 8. Constraints and Assumptions

1. The application is initially developed for Android only.
2. YouTube Music and Spotify must expose usable Android media-session information.
3. Accurate synchronization depends on available playback-position updates.
4. Timestamped lyrics must exist in the selected lyrics provider.
5. Internet connectivity is required for lyrics retrieval.
6. Notification-listener access must be enabled manually.
7. Android battery-management policies may affect background reliability.
8. LRCLIB availability, rate limits and API behaviour may change.
9. Lyrics availability and copyright permissions are separate considerations.
10. The initial application is intended for personal use, not public commercial distribution.

---

# 9. Testing and Acceptance Criteria

The MVP shall be considered functionally complete when the following scenarios pass testing.

| Test ID | Test Scenario | Expected Result |
|---|---|---|
| TC-01 | Play music in YouTube Music | Song detected |
| TC-02 | Play music in Spotify | Song detected |
| TC-03 | Open GaaO while music plays | Lyrics loaded automatically |
| TC-04 | Pause music | Lyrics freeze |
| TC-05 | Resume music | Synchronization resumes |
| TC-06 | Skip to next track | New lyrics loaded |
| TC-07 | Skip to previous track | Previous lyrics loaded |
| TC-08 | Seek forward | Correct lyric displayed |
| TC-09 | Seek backward | Correct lyric displayed |
| TC-10 | Switch music applications | New active session detected |
| TC-11 | Stop music | Idle screen displayed |
| TC-12 | Disconnect internet | Offline message displayed |
| TC-13 | No synchronized lyrics found | Unavailable message displayed |
| TC-14 | Put GaaO in background | Media monitoring remains functional where Android permits |
| TC-15 | Reopen GaaO | Current session and position recovered |

Testing shall be performed on actual Android devices, not only emulators.

---

# 10. Development Roadmap

## Phase 1: Technical Proof of Concept

- Create the Android Studio project.
- Configure notification-listener access.
- Detect YouTube Music sessions.
- Detect Spotify sessions.
- Extract metadata.
- Read playback state and position.
- Test pause, resume, skip and seek.
- Verify background behaviour.

**Milestone:** Reliable media-session detection.

## Phase 2: Lyrics Integration

- Integrate LRCLIB.
- Retrieve timestamped lyrics.
- Parse LRC timestamps.
- Handle missing lyrics.
- Handle network failures.

**Milestone:** Correct lyrics retrieval.

## Phase 3: Synchronization Engine

- Implement playback-position tracking.
- Match lyric timestamps.
- Implement pause/resume.
- Handle seeking and track changes.

**Milestone:** Accurate lyric synchronization.

## Phase 4: UI Development

- Create the immersive black screen.
- Implement typography.
- Implement current-line highlighting.
- Implement faded previous and upcoming lines.
- Implement smooth vertical scrolling.
- Implement idle, loading and offline states.

**Milestone:** Complete GaaO experience.

## Phase 5: Testing and Optimization

- Test both music applications.
- Test background behaviour.
- Test network failures.
- Optimize battery consumption.
- Verify performance on actual devices.

**Milestone:** Functional personal MVP.

---

# 11. Future Enhancements

These features may be considered after successful MVP completion:

- Word-by-word lyric highlighting.
- Custom typography and font sizes.
- Additional lyrics providers.
- Additional music application support.
- Offline lyrics caching.
- Multiple scrolling animation styles.
- Landscape display.
- Optional keep-screen-awake setting.

These enhancements are not part of the initial development scope.

---

# 12. Final Project Summary

GaaO is a personal Android lyrics companion that automatically detects music playing in supported applications and displays synchronized lyrics in an immersive, distraction-free interface.

The application prioritizes simplicity, automatic operation, minimal resource consumption and privacy.

It will use native Android media-session APIs and a free external lyrics provider without requiring a dedicated backend.

**Core principle: No music playback. No manual searching. Just lyrics.**

---

**Document Status:** MVP Requirements Finalized  
**Version:** 1.0  
**Project Name:** GaaO (गाओ)