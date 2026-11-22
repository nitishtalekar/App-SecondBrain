# Second Brain

A private, offline-first Android app for catching thoughts, links and photos in seconds and finding them later.
Everything is sorted **on-device by deterministic rules. No LLM, no generative model, no account, no cloud.**

**Add → Analyse → Auto-categorise → Save → Retrieve**

## Run it

Requirements: JDK 17, Android SDK 35 (build-tools 34/35), a device or emulator on Android 8.0+ (API 26).

```bash
./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # install on the connected device/emulator
./gradlew testDebugUnitTest             # classifier, link parser, manual-override rules (28 tests)
./gradlew connectedDebugAndroidTest     # Room DAO tests (needs a device/emulator)
```

`local.properties` must contain `sdk.dir=...` (Android Studio writes it automatically).

## Using it

| | |
|---|---|
| **Capture** | Tap **+** on Recent or Library. Type a thought, paste a link, attach a photo from the gallery or camera, tap **Save**. An unsaved draft is kept if you dismiss the sheet. |
| **Share into it** | Share text, a URL or an image from any app to *Second Brain*; it is saved immediately. |
| **Recent** | Everything you saved, newest first, with its category. A banner links to the Inbox when items need a look. |
| **Library** | Browse by category. The **Inbox** holds anything the app couldn't place confidently. Add, rename or delete categories here. |
| **Search** | Searches titles, notes, links, page metadata and text recognised inside photos. |
| **Review** | Open an item to see *why* it was filed (`Matched: breakfast, menu, cafe`), other likely categories, and **Change** to move it. A category you pick is **locked**: automatic sorting never touches it again (unless you tap *Sort automatically again*). |

## How categorisation works (explainable, local)

1. **Capture first.** The item is written to the database before any analysis, so a failure can never lose it.
2. **Analyse** (background `WorkManager` job, per item; each step isolated, partial results are kept):
   - Photos → **ML Kit text recognition** (OCR) and **ML Kit image labelling** (bundled, offline, discriminative models).
   - Links → page title, description, keywords and Open Graph data via Jsoup (only network use; skipped gracefully offline and retried later).
3. **Classify** with a weighted rule engine (`classify/Classifier.kt`):
   `score(category) = Σ rule.weight × fieldWeight × repeatBoost`, where evidence comes from five fields: text, URL, page metadata, OCR text and image labels. Rules are keywords/phrases, domains, regexes and image labels.
4. **Decide.** The best category wins if its score ≥ `minScore` **and** its confidence (score strength × margin over the runner-up) ≥ `confidenceThreshold`; otherwise the item goes to the **Inbox**. The ranked alternatives and matched terms are stored for review.

Failures never block saving: a link that can't be fetched or a photo that can't be read ends up `FAILED` (visible, searchable, with a **Try again** action, re-attempted on next launch) and is still classified from whatever evidence exists.

## Customising categories

Categories and rules are data, not code. The defaults live in
[`app/src/main/assets/categories.json`](app/src/main/assets/categories.json) and are seeded into the on-device database on first launch.

**Two ways to change them**

1. **In the app:** Settings → *Export categories*, edit the JSON in any editor, Settings → *Import categories*. Imports upsert by `id` (existing categories are updated, new ones added, nothing is deleted); thresholds are applied; automatically sorted items are re-sorted; items you placed manually are untouched. You can also add/rename/delete categories directly in Library.
2. **In the source:** edit `categories.json` before a fresh install.

**File format**

```jsonc
{
  "version": 1,
  "minScore": 2.0,              // a category needs at least this raw score to be auto-assigned
  "confidenceThreshold": 0.3,   // below this confidence the item goes to the Inbox
  "categories": [
    {
      "id": "recipes",          // stable identifier; used for upserts
      "name": "Recipes",
      "color": "#C2693F",       // dot colour in the UI
      "minScore": 3.0,          // optional per-category override
      "rules": [
        { "type": "keyword", "value": "ingredients", "weight": 3.5 },
        { "type": "keyword", "value": "slow cooker",  "weight": 2.5 },
        { "type": "domain",  "value": "allrecipes.com", "weight": 4 },
        { "type": "label",   "value": "Dish",           "weight": 3.5 },
        { "type": "regex",   "value": "\\b\\d+\\s?(g|ml|tbsp)\\b", "weight": 2, "field": "text" }
      ]
    }
  ]
}
```

| Rule type | Matches | Notes |
|---|---|---|
| `keyword` | A word or phrase, case-insensitive, whole-word. Values of 5+ characters also match as a word prefix (`recipe` → `recipes`); shorter ones match exactly or with a plural `s`. | Searched in text, URL, page metadata, OCR text and image labels unless `field` restricts it. |
| `domain` | The link's host (`goodreads.com` also matches `www.goodreads.com`, `m.goodreads.com`). | |
| `label` | An ML Kit image label, exact, case-insensitive (`Food`, `Dish`, `Landmark`, …). | |
| `regex` | A Java/Kotlin regex, case-insensitive, multiline. Invalid regexes are ignored, not fatal. | Searches text, URL and OCR by default. Remember to double-escape backslashes in JSON. |

`field` (optional) restricts a rule to `text`, `url`, `meta`, `ocr` or `labels`. Weights: ~1 weak hint, 2–3 solid signal, 4+ near-certain. Repeated hits boost a rule modestly (max ×2).

## Architecture

Single module, feature-grouped packages, MVVM with `StateFlow`, Hilt for DI. See [DESIGN.md](DESIGN.md) and [PRODUCT.md](PRODUCT.md) for the visual and product decisions.

```
app/src/main/java/app/secondbrain/
├── classify/   Pure Kotlin rule engine + config model (no Android deps, unit-tested)
├── data/       Room (items, categories), repositories, DataStore settings
├── ingest/     Image store, ML Kit analyzer, link metadata, WorkManager pipeline
├── ui/         Compose screens, navigation (type-safe routes), theme (light/dark/system)
└── di/         Hilt modules
```

Key decisions: Room for storage (`searchText` column holds all searchable text including OCR), WorkManager for resilient background processing, ML Kit bundled models (no Play Services download needed), original images copied untouched into app-private storage, Navigation Compose, DataStore for the theme preference. The list/detail UI currently reads Room entities directly; introduce UI models if the schema grows.

## Known limitations (first iteration)

- Phone layout only (content is width-capped and centred on wider screens, but there is no navigation rail yet).
- No backup/export of items, only of category rules. Android's auto-backup is on for app data.
- Release build is unminified and unsigned; add signing config before distributing.
