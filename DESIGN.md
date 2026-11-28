---
name: Second Brain
description: The Pocket Observatory. A black-and-white tool with one chosen accent and one charted globe.
colors:
  primary: "#111111"
  on-primary: "#FFFFFF"
  primary-container: "#E0E0E0"
  on-primary-container: "#111111"
  secondary: "#525252"
  on-secondary: "#FFFFFF"
  secondary-container: "#E0E0E0"
  on-secondary-container: "#111111"
  tertiary: "#0B7438"
  on-tertiary: "#FFFFFF"
  tertiary-container: "#BDF2CB"
  on-tertiary-container: "#00210B"
  background: "#FCFCFC"
  on-background: "#111111"
  surface: "#FCFCFC"
  on-surface: "#111111"
  surface-variant: "#E7E7E7"
  on-surface-variant: "#525252"
  surface-container-lowest: "#FFFFFF"
  surface-container-low: "#F2F2F2"
  surface-container: "#EBEBEB"
  surface-container-high: "#E7E7E7"
  surface-container-highest: "#E0E0E0"
  outline: "#767676"
  outline-variant: "#DCDCDC"
  inverse-surface: "#262626"
  inverse-on-surface: "#F2F2F2"
  inverse-primary: "#F5F5F5"
  error: "#B3261E"
  on-error: "#FFFFFF"
  error-container: "#F9DEDC"
  on-error-container: "#410E0B"
  primary-dark: "#F5F5F5"
  on-primary-dark: "#111111"
  primary-container-dark: "#333333"
  on-primary-container-dark: "#F5F5F5"
  secondary-dark: "#ABABAB"
  on-secondary-dark: "#1C1C1C"
  secondary-container-dark: "#333333"
  on-secondary-container-dark: "#F5F5F5"
  tertiary-dark: "#4FDB84"
  on-tertiary-dark: "#000000"
  tertiary-container-dark: "#005226"
  on-tertiary-container-dark: "#BDF2CB"
  background-dark: "#000000"
  on-background-dark: "#F5F5F5"
  surface-dark: "#000000"
  on-surface-dark: "#F5F5F5"
  surface-variant-dark: "#2C2C2C"
  on-surface-variant-dark: "#ABABAB"
  surface-container-lowest-dark: "#000000"
  surface-container-low-dark: "#111111"
  surface-container-dark: "#161616"
  surface-container-high-dark: "#1C1C1C"
  surface-container-highest-dark: "#242424"
  outline-dark: "#7A7A7A"
  outline-variant-dark: "#2C2C2C"
  inverse-surface-dark: "#E7E7E7"
  inverse-on-surface-dark: "#1C1C1C"
  inverse-primary-dark: "#111111"
  error-dark: "#F2B8B5"
  on-error-dark: "#601410"
  error-container-dark: "#8C1D18"
  on-error-container-dark: "#F9DEDC"
  cluster-1: "#C4503A"
  cluster-2: "#9C6B00"
  cluster-3: "#2F7D4A"
  cluster-4: "#007C85"
  cluster-5: "#1C6FC2"
  cluster-6: "#5B52C8"
  cluster-7: "#9440A8"
  cluster-8: "#C03A6E"
  cluster-9: "#6B7A1F"
  cluster-10: "#9A5530"
  cluster-1-dark: "#FF8F78"
  cluster-2-dark: "#EDC16A"
  cluster-3-dark: "#7FD49A"
  cluster-4-dark: "#5FD3DA"
  cluster-5-dark: "#74B6FF"
  cluster-6-dark: "#A8A0FF"
  cluster-7-dark: "#D993EA"
  cluster-8-dark: "#FF8DB6"
  cluster-9-dark: "#BFD063"
  cluster-10-dark: "#E5A47C"
  icon-ground: "#111111"
  icon-glyph: "#FFFFFF"
typography:
  display-small:
    fontFamily: "Manrope"
    fontSize: "36px"
    fontWeight: 700
    lineHeight: "42px"
    letterSpacing: "-0.6px"
  headline-large:
    fontFamily: "Manrope"
    fontSize: "32px"
    fontWeight: 700
    lineHeight: "38px"
    letterSpacing: "-0.5px"
  headline-medium:
    fontFamily: "Manrope"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: "34px"
    letterSpacing: "-0.4px"
  headline-small:
    fontFamily: "Manrope"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: "30px"
    letterSpacing: "-0.2px"
  title-large:
    fontFamily: "Manrope"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: "28px"
  title-medium:
    fontFamily: "Manrope"
    fontSize: "17px"
    fontWeight: 600
    lineHeight: "23px"
  title-small:
    fontFamily: "Manrope"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: "20px"
    letterSpacing: "0.1px"
  body-large:
    fontFamily: "Manrope"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: "26px"
  body-medium:
    fontFamily: "Manrope"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0.1px"
  body-small:
    fontFamily: "Manrope"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
    letterSpacing: "0.2px"
  label-large:
    fontFamily: "Manrope"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: "20px"
    letterSpacing: "0.1px"
  label-medium:
    fontFamily: "Manrope"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "16px"
    letterSpacing: "0.3px"
  label-small:
    fontFamily: "Manrope"
    fontSize: "11px"
    fontWeight: 500
    lineHeight: "16px"
    letterSpacing: "0.4px"
rounded:
  extra-small: "4px"
  small: "8px"
  medium: "12px"
  large: "16px"
  extra-large: "28px"
spacing:
  xs: "4px"
  s: "8px"
  m: "16px"
  gutter: "20px"
  l: "24px"
  xl: "32px"
  content-max-width: "640px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-large}"
    height: "52px"
  icon-button-tonal:
    backgroundColor: "{colors.secondary-container}"
    textColor: "{colors.on-secondary-container}"
    size: "52px"
  composer-surface:
    backgroundColor: "{colors.surface-container}"
    rounded: "{rounded.large}"
  draft-card:
    backgroundColor: "{colors.surface-container-low}"
    rounded: "{rounded.large}"
    padding: "16px"
  draft-field:
    backgroundColor: "{colors.surface-container-high}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.medium}"
  search-field:
    backgroundColor: "{colors.surface-container}"
    rounded: "{rounded.extra-large}"
  search-field-focus:
    backgroundColor: "{colors.surface-container-high}"
  chip-action-selected:
    backgroundColor: "{colors.tertiary-container}"
    textColor: "{colors.on-tertiary-container}"
    rounded: "{rounded.small}"
  row-thumbnail:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.on-surface-variant}"
    rounded: "{rounded.medium}"
    size: "52px"
  navigation-bar:
    backgroundColor: "{colors.surface-container-low}"
  cluster-panel:
    backgroundColor: "{colors.surface-container-low}"
    rounded: "{rounded.extra-large}"
---

# Design System: Second Brain

## Overview

**Creative North Star: "The Pocket Observatory"**

A small instrument you pull out, note something with, and later look back through. Most of the time it is for noting: a stock Material 3 tool, calm and plain, built from quiet tonal surfaces so the thing being saved or read is the loudest object on screen. It is used in passing moments, so each screen offers one primary action and keeps controls and metadata out of the way until asked.

Looking back is where the observatory appears. The Brain tab draws the collection as a globe that can be turned: grey wire on white by day, on black by night, with clusters as lit bodies and the things in them in orbit. That is the one place expression is spent.

The app is deliberately not AI, and nothing in it borrows AI visual language such as sparkles. It also rejects nested folders, productivity-app gamification and a cluttered detail page.

**Key Characteristics:**
- Calm, plain Material 3 on every working screen.
- Black or white for action and selection; the accent only for what is still waiting.
- Flat tonal layers and hairlines.
- One family, Manrope, mapped to Material type roles.
- One authored moment: the globe.

## Colors

Black, white and neutral greys carry the app, with one accent the user picks and a separate set for telling clusters apart. Roles live in `ui/theme/Color.kt` and are consumed through `MaterialTheme.colorScheme`. Dynamic Color is off. Keys ending in `-dark` are the dark scheme's value for the same role.

### Primary
- **Ink / White** (`primary`, `primary-dark`): the primary action, selection, completed marks, link hosts, and the globe's body and grid. Black in light, near-white in dark.

### Secondary
- **Grey** (`secondary`, `secondary-container`): the quiet supporting role behind tonal buttons, selected chips and the navigation indicator.

### Tertiary
- **Accent** (`tertiary`, `tertiary-container`): anything still waiting on the user: pending status text, the open completion circle, the selected "waiting on you" chip, and the ring around a waiting item on the globe. It also fills **Add to brain** on the Add screen once there is something to add. Chosen in Settings from the 24 swatches in `data/Accent.kt`; Green is the default and is what the frontmatter shows.

### Neutral
- **White / Black** (`background`, `surface`): the ground in light and dark.
- **Ink** (`on-surface`): primary text.
- **Faded Ink** (`on-surface-variant`): secondary text, section labels, placeholders, completed titles.
- **Tonal Layers** (`surface-container-low` to `surface-container-high`): navigation, panels and cards sit on low; the composer and thumbnails on the middle step; fields inside cards on high.
- **Hairline** (`outline-variant`): row and section dividers.
- **Error** (`error`): destructive actions only.

### Cluster palette
Ten hues per theme (`cluster-1` to `cluster-10`). They identify clusters as dots, orbs, arcs and orbit lines. A tag prefers a stable colour and steps past ones already taken.

### Named Rules
**The Accent Means Waiting Rule.** The accent marks only things still waiting on the user, plus the active **Add to brain** button. Never use it for decoration, emphasis or completed items. (Cluster hues are a separate set and do not follow the accent.)

**The Hues Are Not Text Rule.** Cluster colours identify; they never colour text.

**The Roles Only Rule.** Screens take colour from Material roles, so light and dark resolve themselves. No raw hex in screen code.

## Typography

**Display Font:** Manrope (variable, bundled in `res/font`)
**Body Font:** Manrope
**Label/Mono Font:** none distinct

**Character:** Clean, geometric and legible at small sizes. One family for everything, in weights 400, 500, 600 and 700. Frontmatter sizes written in px are sp on Android.

### Hierarchy
- **Headline medium** (700, 28sp): tab titles.
- **Headline small** (600, 24sp): titles of pushed screens (review, settings), item headings, and a short single-line thought shown large.
- **Title large** (600, 22sp): cluster panel and empty-state titles. The composer uses this size at regular weight.
- **Title medium** (600, 17sp): row titles, card kind labels, settings sections.
- **Title small** (600, 14sp): section labels, in Faded Ink.
- **Body large** (400, 17/26sp): reading and editing item text.
- **Body medium** (400, 14/20sp): hints, snippets, fact values.
- **Label large** (600, 14sp): buttons and globe cluster names.
- **Label medium / small** (500, 12sp / 11sp): row metadata, fact labels, globe item titles.

`display-small`, `headline-large` and `body-small` are defined in `ui/theme/Type.kt`; no screen sets them directly.

### Named Rules
**The Role Rule.** Text maps to a Material type role. Use sp so it follows the system font size.

## Layout

An 8dp rhythm with 4dp for fine adjustment, 20dp side gutters, and content capped at 640dp and centred on Add, Search, Detail and Settings.

Navigation is a bottom bar on compact widths and a navigation rail from 600dp wide. The app is edge-to-edge and respects system bar and keyboard insets.

Tab screens open with a header at least 72dp tall. The primary action sits at the bottom of the screen, within thumb reach. Lists are flat rows with 12dp vertical padding, separated by inset hairlines; chips wrap or scroll horizontally.

- **Add:** a centred column: "What's on your mind?", a short writing box, **Upload photo** and **Take photo** side by side, then **Add to brain**. Review is a stack of post cards above one **Save** button.
- **Library:** search field, suggested tags while typing, a four-column grid of filter tiles with icons, and results only once something is searched or filtered.
- **Brain:** the globe fills the screen above a rail of cluster chips. A selected cluster opens a panel, below the globe (52% of height) when taller than wide, or beside it (360dp) when wider than tall.
- **Detail:** back, Advanced, the content, and at most one button (complete or undo).

## Elevation & Depth

Flat. Depth comes from the surface-container steps and 0.5 to 1dp hairlines. The app adds no shadows of its own; sheets, dialogs and snackbars keep their stock Material behaviour. The only modelled depth is on the globe, where perspective, fading with distance and shaded spheres show what is near.

### Background
Every screen sits on one flat dot grid (`ui/components/Background.kt`): 2dp dots every 20dp in `on-background` at 9%. Cards, bars and the globe are solid and cover it. Tags read in Title Case with no `#`.

### Tag rules
Reached from Settings. A searchable list of tags with a count of words and sites, then one editor per tag: **Any one of these words**, **Or two of these**, **Or a link from**, **More** (photo labels, implied tags), **Try it**, and on/off plus **Reset to default** (built-in) or **Delete tag** (the user's own). Weights and patterns stay in the exported file.

### Named Rules
**The Flat Instrument Rule.** Separate surfaces by tone or a hairline. Never add a drop shadow.

## Shapes

Softly rounded, from `MaterialTheme.shapes`: 4dp extra small, 8dp small (chips), 12dp medium (thumbnails, photos in forms, fields in cards), 16dp large (composer, cards, the detail photo), 28dp extra large (search field, dialogs, and the leading corners of the cluster panel). Buttons keep Material's fully rounded shape. Circles are used for cluster dots and everything on the globe.

## Components

Calm and plain: stock Material 3 components themed through colour, type and shape. Pressed, focus, disabled and ripple states are Material's own.

### Buttons
- **Primary:** filled `primary`, fully rounded, 52dp tall, usually full width at the bottom of the screen. Labels name the action ("Add to brain", "Save", "Mark as bought").
- **Tonal:** the 52dp square-target icon buttons beside the composer (upload, camera), and the undo button on a completed item.
- **Outlined:** secondary actions such as opening a link, editing, deleting (error-coloured icon and label) and settings actions.
- **Text:** dialog actions, clear, discard and disclosure toggles.
- **Busy:** the primary button shows a small spinner and "Reading…" in place of its label.

### Chips
- **Filter:** search state and type filters, sub-tags in a cluster, and draft tags. A removed draft tag stays visible, struck through, with a plus to restore it.
- **Waiting on you:** filter chips that select into the accent container with a check.
- **Input:** applied search tags; the whole chip removes the filter.
- **Saved tag:** under Advanced, a 32dp chip split into two 48dp targets: the name searches for the tag, the cross removes it. Tags the user added are filled; automatic ones are outlined.
- **Suggestion / Assist:** the tag cloud (name and count), "Add tag", and cluster chips led by a 10dp colour dot.

### Cards / Containers
- **Composer:** a large surface-container panel holding a borderless text field and the attached photo.
- **Draft card:** surface-container-low, 16dp radius, 16dp padding, 12dp between blocks.
- **Cluster panel:** surface-container-low with 28dp leading corners.

### Inputs / Fields
- **Composer field:** no fill and no indicator; the panel is the field.
- **Draft field:** filled surface-container-high, 12dp radius, no indicator.
- **Search field:** 28dp pill, leading search icon, clear button when filled; fill rises one tonal step on focus.
- **Editor and tag dialog:** stock filled Material fields.

### Navigation
Three destinations (Add, Library, Brain) with labels and their own icons (lightbulb, books, network; outlined, filled when selected; `res/drawable/ic_tab_*`), on surface-container-low. Pushed screens (Detail, Settings) hide it and show a back arrow. Screen changes cross-fade (220ms in, 120ms out).

### Item row
A 52dp rounded thumbnail (type icon behind any image), a title of up to two lines, an optional snippet, then status and up to three tags. Actionable items end in a 28dp completion circle: accent and open while waiting, `primary` and filled when done. A completed title is struck through in Faded Ink.

### Empty states
A centred title and one short hint, with an action where there is somewhere to go. On an empty Brain it floats over the globe on a near-opaque surface.

### The globe
Drawn on a Compose `Canvas` with true perspective (`brain/Sphere.kt`, `ui/screens/BrainGlobe.kt`). Meridians and parallels fade with depth; clusters are shaded orbs with an orbit ring; items orbit them (solid when open, hollow when completed, ringed in the accent while waiting); arcs join clusters that share tags. Labels never overlap, and nearer ones win. The cluster chips do the same job without the globe.

Clusters assemble from the centre (900ms), the globe idles slowly, keeps momentum after a flick, and travels to a chosen cluster in 750ms with ease in-out. With system animations off there is no idle spin, and travel and assembly are instant.

## Do's and Don'ts

### Do:
- **Do** give each screen one primary action.
- **Do** show information first and put controls and metadata on request.
- **Do** write short, plain, sentence-case copy, with controls that name their action.
- **Do** use stock Material 3 components, themed through roles, type and shape.
- **Do** keep expression on the Brain tab.
- **Do** offer a list-based way to do anything the globe does.
- **Do** respect the system's animation setting.

### Don't:
- **Don't** use AI sparkle iconography or present anything as AI.
- **Don't** use nested folders.
- **Don't** add productivity-app gamification.
- **Don't** clutter the detail screen; it holds the item and at most one button.
- **Don't** use the accent for anything that is not waiting on the user.
- **Don't** colour text with cluster hues.
- **Don't** add drop shadows.
