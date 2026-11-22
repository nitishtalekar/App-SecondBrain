# Design

**World: pine and mist.** A cool, quiet shelf of folders. Green-grey "mist" neutrals in light, deep green-black in dark (never pure black or white). One accent, deep pine, used only for the primary action, selection and focus; a single amber tertiary is held in reserve.

## Colour
All neutrals are tinted toward the pine hue (~170-180 deg). Roles are defined in `ui/theme/Color.kt` and consumed only through `MaterialTheme.colorScheme`.

| Role | Light | Dark |
|---|---|---|
| background / surface | `#F3F6F5` mist | `#0D1413` |
| onSurface | `#141C1B` | `#E3EBEA` |
| onSurfaceVariant | `#4A5857` | `#A5B4B2` |
| outlineVariant (hairlines) | `#D3DDDB` | `#2A3736` |
| primary (pine) | `#1B6B66` | `#6FC7BF` |
| primaryContainer | `#CDE9E5` | `#134A45` |

Text contrast is at least 4.5:1 in both themes. Dynamic Color is intentionally off; both schemes are hand-built, not inverted.

**Folder colour** tints the folder glyph only (muted mid-tones from `categories.json`); subfolders inherit their parent's colour. Never a filled chip, never text colour.

## Type
**Figtree** (variable, bundled in `res/font`) for everything: one family, weights 400-700 mapped to the Material roles in `ui/theme/Type.kt`. sp units only.

## Shape & elevation
Radii from `MaterialTheme.shapes` (12 / 16 / 28 dp). Elevation is tonal (surface containers), never drop shadows. Item lists are flat rows separated by hairlines; folder tiles are the only filled containers.

## Layout
8 dp grid, 20 dp gutters, content capped at 640 dp and centred on wide screens. **No bottom bar.** Home is a two-column shelf of folder tiles (glyph, name, count); Recent, Search and Settings are icons in the home header. Folders nest to any depth: a folder shows its subfolder tiles above its own items. Items carry any number of tags (`#tag`), searchable and tappable. One FAB for capture on Home, Recent and folders. Edge-to-edge with system insets. Predictive back works everywhere.

## Motion
Material standard easing, short durations; one authored moment (new items fade in). Honours system animation scale.

## Copy
Short, plain, sentence case. Controls name their action ("Save", "Change category").
