# Heimdall Product Requirements Document

**Status:** living product baseline
**Date:** 2026-11-28
**Product:** Heimdall, Android dialer and contacts app (fork of Koler)
**Current version:** v1.9.8 (versionCode 98)

## 1. Product Summary

Heimdall is a personal Android Phone app. It is a GPL-3.0 fork of Koler. It
handles calls through Android Telecom and reads/writes Android's call log,
contacts and block list. It has no internet permission and no database of its
own. The repository does not establish store publication or testing beyond the
owner's devices.

## 2. Problem and Users

### Problem

OEM dialers bundle spam services, ads or cloud lookups, and look different from
the rest of the owner's apps. Koler is private and simple but lacks a modern
call screen, a full contact editor and the shared design.

### Primary users

| User | Need | Surface |
|---|---|---|
| Owner (daily driver) | Reliable calls, fast contacts, consistent design | Whole app |
| Sideloading Android users | A private, offline, open-source dialer | Signed APK from GitHub releases |
| Contributor | Build and patch the fork | This repository |

## 3. Goals

1. Place, receive and end calls reliably with correct audio routing.
2. Make answering, declining and returning calls obvious and one-gesture.
3. Find, create and edit contacts without leaving the app.
4. Be the reference implementation of [DESIGN.md](DESIGN.md).
5. Stay offline and licence-compliant.

## 4. Non-goals

- Caller ID lookup services, spam databases or any network feature.
- Call recording.
- VoIP/SIP accounts.
- Paid features, ads or in-app purchases (donations only).

## 5. Functional Requirements

### 5.1 Default dialer

- Prompt for the Phone role on first launch; explain and link to Android's
  **Default apps** if declined. Track completion in `phone_setup_completed`.

### 5.2 Calls

- Incoming: full-screen intent when locked; swipe to answer or decline.
- In call: number/name, timer, avatar; keypad, mute, speaker, more (hold, add
  call, audio route); wide end-call pill.
- Audio routes: earpiece, speaker, wired headset and Bluetooth.

### 5.3 Dial pad

- T9 matching against contacts; DTMF tones and haptics (settings).
- Numbers grouped as typed; input capped at 15 digits (E.164); hint for
  incomplete numbers.

### 5.4 Recents

- Filterable list by `RecentType`; optional grouping of consecutive calls.
- Tap to call back; open details for the number or contact.

### 5.5 Contacts

- Alphabet index, favourites, search pill.
- Contact editor: names, company, multiple phones and emails, photo; read-only
  contacts are explained; discard prompt for unsaved edits.
- Accept "add contact" and "edit contact" intents from other apps.
- Multi-select delete with a warning that synced accounts are affected.

### 5.6 Blocking and settings

- Block/unblock numbers via `BlockedNumberContract`.
- Settings listed in [SCHEMA.md](SCHEMA.md#4-settings-datastore).

## 6. Technical Requirements

- Kotlin, Jetpack Compose (Material 3), Hilt, DataStore.
- Modules: `koler` (app), `chooloolib` (UI, theme, repositories), `kontacts`.
- JDK 21, compile SDK 37, target SDK 36, min SDK 29.

## 7. Success Metrics

| Area | Target | Evidence |
|---|---|---|
| Call reliability | No missed or dropped calls caused by the app | Owner use |
| Audio routing | Correct route on every transition | Manual matrix per release |
| Privacy | No `INTERNET` permission in the merged manifest | `aapt dump permissions` per release |
| Licence | Source, LICENSE and NOTICE published per release | Release checklist |

## 8. Reliability and Privacy Invariants

- Never add the internet permission or a network SDK.
- Call handling must not wait on contact or call-log queries.
- Never keep a copy of contacts or call history outside the system providers.
- Sign releases with the same key so updates install over existing copies.

## 9. Risks and Dependencies

| Risk | Mitigation |
|---|---|
| OEM Telecom quirks | Test on the owner's device each release; keep Koler's fallbacks |
| Google Sans font files inherited from Koler (`chooloolib/src/main/res/font/`) are not openly licensed | Used only by the *Classic* style. Remove them or switch Classic to the system font before any store or public binary release |
| Upstream drift | Fetch `upstream` (Chooloo/koler) and merge fixes |
| Trademark confusion | Distinct name, icon, app ID and "not affiliated" note |

## 10. Release Acceptance

A release is ready when it builds signed, installs over the previous version,
passes an incoming/outgoing/audio-route check on the owner's phone, has no
internet permission, and its GitHub tag carries matching source, LICENSE and
NOTICE.
