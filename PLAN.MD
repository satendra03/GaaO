# GaaO — Development Implementation Plan

**Version:** 1.0  
**IDE:** VS Code  
**Coding Assistant:** Gemini  
**Platform:** Android  
**Development Approach:** Incremental, test-driven implementation

---

# 1. Development Philosophy

GaaO shall be developed in small, independently testable phases.

The coding assistant must not generate the entire application in one attempt.

Each phase shall have:

- Clearly defined objectives.
- Limited implementation scope.
- Expected files and components.
- Acceptance criteria.
- Manual verification steps.

A phase must be verified before proceeding to the next phase.

The primary priority is functional correctness rather than visual completeness.

# 2. Phase 0 — Development Environment Setup

## Objective

Prepare the development environment and initialize the Android project.

## Required Software

1. VS Code.
2. Android Studio.
3. Android SDK.
4. Android SDK Platform Tools.
5. Compatible JDK.
6. Git.
7. Gemini coding assistant.

## Tasks

- Install and configure Android Studio.
- Install Android SDK and required platform tools.
- Configure JDK.
- Configure VS Code.
- Verify ADB availability.
- Create a Git repository.
- Generate a native Kotlin Android project.
- Configure Jetpack Compose.
- Verify Gradle Wrapper builds.

## Project Configuration

Project name: GaaO

Application ID: com.gaao.app

Language: Kotlin

UI Framework: Jetpack Compose

Build system: Gradle Kotlin DSL.

## Acceptance Criteria

- Project opens in VS Code.
- Gradle synchronization succeeds.
- Initial debug build succeeds.
- Application installs on an Android device or emulator.
- Basic application launches successfully.

**Deliverable:** A working empty Android application.

# 3. Phase 1 — Media Detection Prototype

**Priority: Highest**

## Objective

Prove that GaaO can detect external music playback.

Do not implement lyrics retrieval or the final UI in this phase.

## Tasks

### Task 1.1: Notification Listener

- Create NotificationListenerService implementation.
- Declare the service in AndroidManifest.xml.
- Configure the required service permission.
- Create a way to open Android notification-listener settings.
- Detect listener connection and disconnection.

### Task 1.2: Active Media Sessions

- Implement MediaSessionManager access.
- Retrieve active media sessions.
- Identify supported music applications.
- Handle multiple active sessions.
- Register active-session change callbacks.

### Task 1.3: Metadata Extraction

Extract:

- Track title.
- Artist.
- Album.
- Duration.
- Application package name.

### Task 1.4: Playback Monitoring

Observe:

- Playing.
- Paused.
- Stopped.
- Buffering, when reported.
- Playback position.
- Playback speed.

Register MediaController callbacks and handle session replacement.

### Task 1.5: Diagnostic Screen

Create a temporary, simple diagnostic screen showing:

```text
GaaO Media Detector

Listener Status: Connected

Music Application: YouTube Music

Song: Example Song

Artist: Example Artist

Playback: PLAYING

Position: 00:35

Speed: 1.0x
```

This is a development-only screen, not the final product UI.

## Manual Testing

| Test | Expected Result |
|---|---|
| Play YouTube Music | Detect session |
| Play Spotify | Detect session |
| Pause | State changes to PAUSED |
| Resume | State changes to PLAYING |
| Next track | Metadata changes |
| Previous track | Metadata changes |
| Seek forward | Position changes |
| Seek backward | Position changes |
| Switch music apps | Active session changes |
| Close GaaO interface | Listener behaviour is tested |
| Reopen GaaO | Active session is recovered |

## Acceptance Criteria

All supported playback scenarios must work on an actual Android device.

Any unavailable metadata or playback information must be documented rather than assumed.

**Deliverable:** A verified media detection prototype.

# 4. Phase 2 — Lyrics API Integration

## Objective

Retrieve accurate timestamped lyrics from LRCLIB.

## Tasks

### Task 2.1: Networking Setup

- Add OkHttp.
- Add Kotlin Serialization.
- Configure internet permission.
- Create the LRCLIB API client.
- Configure appropriate request headers.
- Implement timeout and error handling.

### Task 2.2: Lyrics Retrieval

- Create lyrics request and response models.
- Implement the track lookup request.
- Pass title and artist.
- Pass duration and album when available.
- Validate the returned track metadata.
- Handle missing synchronized lyrics.

### Task 2.3: LRC Parsing

- Implement the LRC parser.
- Convert timestamps to milliseconds.
- Extract lyric text.
- Handle multiple timestamps.
- Sort parsed lines chronologically.
- Reject malformed entries safely.

### Task 2.4: Test Data

Use a small set of known tracks with available synchronized lyrics.

Test:

- Valid lyrics.
- Missing lyrics.
- Instrumental tracks.
- Incorrect metadata.
- Network failures.
- Rate limiting.

## Acceptance Criteria

- Lyrics are retrieved successfully.
- Timestamped lyrics are parsed correctly.
- Incorrect matches are rejected.
- API errors are handled without crashing.

**Deliverable:** Working lyrics retrieval and parsing module.

# 5. Phase 3 — Lyrics Synchronization Engine

## Objective

Synchronize lyrics with external music playback.

## Tasks

### Task 3.1: Position Tracking

- Read playback position.
- Read playback speed.
- Use the reported position timestamp.
- Estimate position between updates.
- Stop progression when playback is paused.

### Task 3.2: Timestamp Matching

- Compare playback position with lyric timestamps.
- Identify the active lyric line.
- Identify previous and upcoming lines.
- Update active line on playback changes.

### Task 3.3: Event Handling

Implement synchronization for:

- Play.
- Pause.
- Resume.
- Seek forward.
- Seek backward.
- Next track.
- Previous track.
- Playback session replacement.

### Task 3.4: Synchronization Testing

Use a known timestamped LRC test fixture and simulated playback positions.

Compare expected and actual active lyric indices.

Test position corrections and timing drift.

## Acceptance Criteria

- Correct lyric line is selected for a given playback position.
- Pause freezes synchronization.
- Resume continues from the correct position.
- Seeking recalculates the active line.
- Track changes reset the previous song's synchronization state.

**Deliverable:** Functional synchronization engine.

# 6. Phase 4 — GaaO UI Development

## Objective

Implement the final immersive lyrics experience.

## Design Principles

- Full-screen display.
- Pure black background.
- White highlighted lyric.
- Previous lines faded.
- Upcoming lines faded.
- Smooth vertical scrolling.
- Minimal visual elements.
- No unnecessary controls.

## Tasks

### Task 4.1: Main Screen

Implement the full-screen lyrics layout using Jetpack Compose.

### Task 4.2: Lyrics Animation

Implement:

- Vertical scrolling.
- Current-line highlighting.
- Previous-line fading.
- Upcoming-line fading.
- Smooth transitions.

### Task 4.3: Application States

Implement:

- IDLE.
- DETECTING.
- LOADING_LYRICS.
- PLAYING.
- PAUSED.
- OFFLINE.
- LYRICS_UNAVAILABLE.
- ERROR.

### Task 4.4: State Integration

Connect the ViewModel to the media detection repository, lyrics repository and synchronization engine.

The UI shall react to state changes without directly accessing Android media APIs.

## Acceptance Criteria

- Interface follows the defined design.
- Scrolling follows the active lyric.
- No unnecessary visual controls.
- State transitions display appropriate messages.
- No excessive recompositions or scrolling jumps.

**Deliverable:** Complete GaaO user interface.

# 7. Phase 5 — Integration and Testing

## Objective

Verify that all modules work together.

## Testing Scenarios

### Music Detection

- YouTube Music.
- Spotify.
- Multiple active sessions.
- Missing metadata.
- Session replacement.

### Lyrics Retrieval

- Valid lyrics.
- No lyrics.
- No synchronized lyrics.
- Incorrect matches.
- Internet disconnection.
- API rate limiting.

### Synchronization

- Normal playback.
- Pause and resume.
- Seeking.
- Track switching.
- Playback speed changes.

### Background Behaviour

- GaaO moved to background.
- GaaO reopened.
- Screen locked.
- Notification listener disconnected.
- Device reboot.
- Battery restrictions.

### UI

- Idle screen.
- Loading screen.
- Offline screen.
- Playing screen.
- Lyrics unavailable screen.

## Acceptance Criteria

All critical test scenarios pass on a physical Android device.

Background reliability limitations must be documented.

# 8. Phase 6 — Optimization and Finalization

## Tasks

- Remove development-only diagnostic UI.
- Remove unnecessary logs.
- Review dependencies.
- Optimize scrolling.
- Review network requests.
- Verify battery consumption.
- Review application permissions.
- Test clean installation.
- Generate release APK.
- Document installation and configuration.

**Deliverable:** GaaO MVP APK.

# 9. Gemini Coding Assistant Instructions

The following instructions must be followed throughout development.

1. Read the SRS and TDD before implementing a feature.
2. Implement only the currently assigned phase.
3. Do not generate the entire application at once.
4. Do not introduce unnecessary dependencies.
5. Do not introduce a backend or database.
6. Follow the defined package structure.
7. Use Kotlin and Jetpack Compose.
8. Keep media detection independent of UI implementation.
9. Write maintainable, readable code.
10. Handle errors and application lifecycle correctly.
11. Explain every new dependency.
12. Do not modify unrelated working components.
13. Run the Gradle build after each meaningful implementation.
14. Fix compilation errors before proceeding.
15. Provide a summary of files created and modified.
16. Do not claim that a feature works without testing it.

## Required Output After Every Task

The coding assistant must provide:

- Implementation summary.
- Files created.
- Files modified.
- Dependencies added.
- Build/test result.
- Known limitations.
- Manual testing instructions.

# 10. Git and Version Control Strategy

Use Git throughout development.

Suggested milestones:

| Commit | Milestone |
|---|---|
| Initial | Android project setup |
| Media-01 | Notification listener |
| Media-02 | Active media sessions |
| Media-03 | Playback monitoring |
| Lyrics-01 | LRCLIB integration |
| Lyrics-02 | LRC parser |
| Sync-01 | Synchronization engine |
| UI-01 | Main lyrics screen |
| UI-02 | Scrolling and animations |
| Test-01 | Integration testing |
| Release-01 | MVP release |

Each milestone should be committed only after successful verification.

# 11. Final Definition of Done

GaaO MVP is complete when:

1. YouTube Music and Spotify playback can be detected.
2. Track metadata is retrieved.
3. Timestamped lyrics are fetched.
4. Lyrics synchronize with playback.
5. Pause, resume and seek work correctly.
6. Song changes are handled automatically.
7. Background monitoring works within Android's lifecycle constraints.
8. Full-screen lyrics display correctly.
9. Offline and error states are handled.
10. The application builds and installs successfully.
11. The application remains lightweight and avoids unnecessary background processing.

---

**Final Development Principle**

Build the functionality first, verify it, and then build the experience.

GaaO should remain a small personal utility, not become an unnecessarily complicated software project.

**End of Development Implementation Plan.**