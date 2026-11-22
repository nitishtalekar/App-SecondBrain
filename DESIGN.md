# Design

**World: paper and ink.** A well-used notebook. Warm off-white paper in light, warm near-black ink-wash in dark (never pure black or white). One accent, a clay/terracotta, used only for the primary action, selection and focus.

## Colour
All neutrals are tinted toward the paper hue (~45-80°). Roles are defined in `ui/theme/Color.kt` and consumed only through `MaterialTheme.colorScheme`.

| Role | Light | Dark |
|---|---|---|
| background / surface | `#F7F4EE` paper | `#14120F` ink-wash |
| onSurface | `#1F1C17` | `#ECE6DA` |
| onSurfaceVariant | `#5F594E` | `#B1A999` |
| outlineVariant (hairlines) | `#DDD7CA` | `#38332C` |
| primary (clay) | `#A8452B` | `#E7926F` |
| primaryContainer | `#F2DDD3` | `#5A2A18` |

Text contrast is at least 4.5:1 in both themes. Dynamic Color is intentionally off so the identity stays consistent; both schemes are hand-built, not inverted.

**Category colour** is a small dot only (muted, mid-tone so it reads on both themes). Never a filled chip, never text colour.

## Type
- **Newsreader** (variable serif, bundled) is the content voice: screen titles, item titles, note text.
- **System sans** carries UI chrome: metadata, labels, buttons.
- Scale mapped to the Material roles in `ui/theme/Type.kt`; sp units only so it follows system font size.

## Shape & elevation
Radii from `MaterialTheme.shapes` (12 / 16 / 28 dp). Elevation is tonal (surface containers), never drop shadows. Lists are flat rows separated by hairlines, not cards. No nested containers.

## Layout
8 dp grid, 20 dp screen gutters, content capped at 640 dp and centred on wide screens. Three destinations (Recent, Library, Search) in a bottom bar; one FAB for capture. Edge-to-edge with system insets. Predictive back works everywhere.

## Motion
Material standard easing, short durations; one authored moment (new items fade in). Honours system animation scale.

## Copy
Short, plain, sentence case. Controls name their action ("Save", "Change category").
