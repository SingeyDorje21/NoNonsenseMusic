# Playback Widgets (Android, macOS, Windows)

## Problem
NoNonsenseMusic listeners have to open or switch to the app to start music, like a song, or get back to something they
played recently. The built-in OS media controls (Android media notification, Windows media flyout, macOS Now Playing)
cover play/pause/skip only while something is already playing; they cannot start a playlist, like a song, or show
recent plays. TBD — cost of leaving this unsolved needs validation via user interviews / usage analytics.

## Evidence
- Assumption — needs validation via user research (do listeners ask for a widget?) and analytics (how often is the app
  opened only to skip, like, or start a playlist?).
- Market signal (not user evidence): Spotify, YouTube Music, Apple Music and Poweramp all ship widgets with now-playing
  controls; Spotify and YouTube Music add like/thumbs; Apple Music and Spotify emphasize recently played / quick-start.

## Users
- **Primary**: Existing NoNonsenseMusic listeners on Android, macOS and Windows who play music in the background while
  doing something else, and want to start, steer, or like music without bringing the app forward.
  TBD — refine the segment via user research.
- **Not for**: People who aren't signed in or have no library yet (the widget only shows an invite to open the app);
  people who want to browse or search the full catalog from the widget.

## Hypothesis
We believe **home-screen / desktop widgets with now-playing controls, a like button, recently played, and pinned
playlists** will **let listeners start and steer music without opening the app** for **background listeners on
Android, macOS and Windows**.
We'll know we're right when **TBD % of active listeners on each platform have a widget installed and widget-started
plays reach TBD % of sessions within TBD weeks of release**.

## Success Metrics
| Metric | Target | How measured |
|---|---|---|
| Widget adoption (active listeners with ≥1 widget placed) | TBD — needs baseline | Widget install/remove events, per platform |
| Plays started from the widget (recent / pinned) | TBD | Play-source attribution |
| Likes made from the widget | TBD | Like-source attribution |
| Widget state accuracy (shows the right song/state) | No user-visible mismatch beyond the platform's refresh limit | QA + bug reports |

## Scope
**MVP**

Available on all three platforms (Windows as both a Widgets-board widget and a mini-player window):
- **Now playing**: album art, title, artist.
- **Controls**: play/pause, next, previous.
- **Like** the current song (reflects and toggles the same liked state as the app).
- **Recently played**: tap an item to start it.
- **Pinned playlists/albums**: up to 4 quick-start tiles.
- **Idle state**: when nothing is playing, show "resume last song" + recent/pinned instead of a blank widget.
- **Signed-out / empty state**: prompt to open the app.
- **Tap widget** → opens the app's player.
- **Sizes**: small (art + play/pause), medium (now playing + controls + like), large (adds recent and pinned) —
  where the platform supports multiple sizes.
- **Theme**: follows light/dark; Android follows the system dynamic colors.

**Windows mini-player window** — a small, optionally always-on-top window offering the same now-playing, controls and
like, for use while working in other apps.

**Out of scope**
- Seeking/scrubbing and a live progress bar — macOS/Windows widgets refresh on the OS schedule, not in real time.
- Shuffle/repeat toggles, queue preview, volume — power-user extras; revisit after MVP adoption data.
- Lyrics on the widget — space and licensing.
- Search or catalog browsing from the widget — that's the app's job.
- Friends' listening activity — revisit once Friends usage is understood.
- iOS / lock-screen / watch widgets — not requested.

## Delivery Milestones
<!-- Business outcomes, not engineering tasks. /plan turns each into a plan. -->
<!-- Status: pending | in-progress | complete -->

| # | Milestone | Outcome | Status | Plan |
|---|---|---|---|---|
| 1 | Android widget | Android listeners can place a widget that shows now playing, controls, like, recent and pinned (and music keeps playing in the background, with media notification) | in-progress | ~/.claude/plans/claude-prds-playback-widgets-prd-md-binary-rossum.md |
| 2 | macOS widget | Mac listeners get the same widget on the desktop / Notification Center | pending | — |
| 3 | Windows mini-player | Windows listeners can open a compact, always-on-top player with controls and like | pending | — |
| 4 | Windows Widgets-board widget | Windows listeners can add a NoNonsenseMusic widget to the Widgets board | pending | — |
| 5 | Measure & decide | Adoption and widget-start metrics reviewed; decide on Tier 3 extras | pending | — |

## Open Questions
- [ ] Who exactly is the primary user and what pain have they reported? (Framing and evidence were skipped.)
- [ ] Minimum OS versions: interactive macOS widget buttons need a recent macOS — what is the cut-off, and what do
      older Macs get (display-only + tap to open)?
- [ ] Can the Windows Widgets board be relied on to reach users? It's hidden behind a taskbar button and may be turned off.
- [ ] Which macOS app hosts the widget: the Flutter app or the native Mac app? This affects whether milestone 2 is shared work.
- [ ] Should the widget play while the app isn't running (cold start from a widget tap), on every platform?
- [ ] Do "pinned" tiles mean pinned albums only, or also user-chosen playlists — and who picks the 4?
- [ ] Metric targets: no baseline exists — set after instrumenting widget events.

## Risks
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Widget shows stale song/state (OS refresh limits on macOS/Windows) | High | Medium | Keep progress/time off the widget; refresh on track change; tap opens the app for the live view |
| Windows Widgets board gets little use | Medium | Medium | Ship the mini-player first (milestone 3); measure before investing more |
| Three platforms triple the work and QA | High | High | Ship one platform at a time, Android first; same feature set everywhere |
| Like/recent state differs between widget and app | Medium | Medium | Widget reads and writes the same library state as the app |
| Battery / background-limit restrictions on Android stop widget updates | Medium | Medium | TBD — validate on target devices in milestone 1 |

---
*Status: DRAFT — requirements only. Implementation planning pending via /plan.*
