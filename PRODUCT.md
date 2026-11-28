# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

The owner is the first user; the app should later be ready for other people to install.

The working situation is one person capturing in passing moments (standing in a shop, reading on a train, between meetings), often one-handed and interrupted. That situation comes from the original briefs and is an assumption.

## Product Purpose

Second Brain catches loose thoughts, links and photos in seconds and finds them again later.

Success:
- Adding takes two taps after typing.
- Tags are plentiful enough that any remembered detail finds the item.
- Things to read, watch, buy or do can be completed on the spot.
- An opened item shows its information first; everything else is under Advanced.

## Positioning

A second brain with no AI in the generative sense: items are tagged on-device by deterministic rules that can show why each tag was added, and that the user can export, edit and import.

## Operating Context

- Things arrive by typing, pasting, the camera, the gallery, or Android's share sheet.
- **Add**: compose, review suggested tags, confirm.
- **Library**: search by words, tags, state and type; matching tags are suggested while typing. With nothing searched, everything shows as a shuffled feed of staggered cards, or as a list.
- **Brain**: a navigable globe of clusters.
- Opening an item shows the item; tags, details, editing and deletion are under Advanced.

## Capabilities and Constraints

Current functionality:
- Text, link and photo items. A photo containing real text becomes two posts.
- Tagging from weighted rules (`app/src/main/assets/tags.json`), keywords, source site, format and time, plus the user's own tags.
- Photo text recognition and image labelling by bundled, offline, discriminative ML Kit models. Text recognition covers Latin script.
- No folders: items gather into clusters by shared tags.
- Local storage only, with no account. The network is used to fetch details and preview images of added links.
- Android auto-backup is enabled for app data.
- Tag rules can be exported and imported. Items cannot yet be exported or backed up from within the app.
- Android 8.0+ (API 26).

Permanent commitment: **no AI or LLM.** No generative model; tagging stays deterministic rules plus on-device ML Kit.

Offline-first storage, the absence of folders and the three-tab structure describe the product today. They have not been declared permanent.

Not yet ready for public distribution: the release build is unminified and unsigned.

## Brand Commitments

- Name: Second Brain.
- Voice: calm and private. Copy is short, plain, sentence case, and controls name their action.
- Never present the app as AI, including through AI sparkle iconography.
- Anti-references: nested folders, productivity-app gamification, a cluttered detail page.

## Evidence on Hand

- A working codebase with unit tests (tagging, drafts, search, clustering, globe geometry) and database and migration tests.
- App icon: `app/src/main/res/drawable/ic_launcher_foreground.xml`.
- No screenshots, user research, testimonials, usage numbers or store listing exist. Do not invent them.

## Product Principles

1. Nothing is saved until confirmed, and a failed read never blocks saving.
2. Tag generously, and make any tag removable.
3. A manual choice always beats an automatic one: removed tags stay removed.
4. One primary action per screen.
5. Information first; controls and metadata on request.

## Accessibility & Inclusion

No product-specific standard has been set.
