# Product

## Platform

Android phones, Android 10 (API 29) or later. Distributed as signed APKs from
this repository's GitHub releases. No store listing yet.

## Users

- **The owner (Biowulf21)**, as the primary daily user replacing the stock
  Phone and Contacts apps.
- **Android users who sideload open-source apps** and want a private dialer
  with no internet access.

## Product Purpose

Heimdall is the default Phone app: dial pad, call screens, call history,
contacts and blocked numbers in one app. It uses Android's own call log and
contacts, so data stays in the system and other apps keep seeing it. Success
means calls connect and route audio correctly every time, incoming calls are
easy to answer or decline, and contacts are quick to find and edit.

## Positioning

Heimdall is a personal fork of Chooloo's [Koler](https://github.com/Chooloo/koler).
It keeps Koler's calling core and adds the shared Expressive design, redesigned
call screens, a standalone contact editor, a home drawer and phone-number
formatting. It installs alongside Koler under its own app ID and is not
affiliated with or endorsed by Chooloo.

## Operating Context

- Must be the default Phone app (`RoleManager.ROLE_DIALER`) to handle calls and
  manage blocked numbers.
- No internet permission. Links (donate, source) open in the user's browser.
- Contacts may belong to sync accounts (Google, Exchange); edits and deletes
  propagate to them.

## Capabilities and Constraints

- Dial pad with T9 contact search, tones and haptics, 15-digit input cap,
  formatted numbers.
- Incoming call screen with swipe to answer/decline; in-call screen with keypad,
  mute, speaker, more (hold, add call, audio route).
- Recents with filters and optional grouping; return call in one tap.
- Contacts: list with alphabet index, favourites, create/edit (from other apps
  too), multi-select delete.
- Blocked numbers through Android's block list.
- Settings: default screen, theme mode, accent colour, design style,
  left-handed mode, avatar shape.

## Brand Commitments

- Name **Heimdall**; launcher label **Phone**.
- App ID `io.github.biowulf21.heimdall`. Never reuse Koler's name, icon or app ID.
- Always credit Koler and keep its GPL notice (see NOTICE).

## Monetisation

None. Heimdall is free and GPL-3.0-or-later. The only money flow is an optional
donation link (Buy Me a Coffee); the README also points to Koler's own donation
link. Donations unlock nothing and buy no support, warranty or priority. Never
add ads, paid features or in-app purchases.

## Evidence on Hand

Owner-tested on his own devices; screenshots in `docs/screenshots/`. No beta
group, telemetry or store reviews.

## Product Principles

- Calls first: never let UI work delay answering or ending a call.
- System data, system providers; no shadow database.
- No network, no analytics.
- Match [DESIGN.md](DESIGN.md); Heimdall is its reference implementation.

## Accessibility & Inclusion

Left-handed mode, system font scaling, content descriptions on call controls.
Koler's translations are kept. No formal conformance target.
