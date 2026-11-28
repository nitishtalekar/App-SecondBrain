# Second Brain

A private, offline-first Android app for catching thoughts, links and photos in seconds and finding them later.
There are no folders. Everything is **tagged on-device by deterministic rules** and gathers into clusters.
**No LLM, no generative model, no account, no cloud.**

**Add → Read → Tag → Confirm → Find**

## Run it

Requirements: JDK 17, Android SDK 35 (build-tools 34/35), a device or emulator on Android 8.0+ (API 26).

```bash
./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # install on the connected device/emulator
./gradlew testDebugUnitTest             # tagging, drafts, search, clustering, globe maths
./gradlew connectedDebugAndroidTest     # database and migration tests (needs a device/emulator)
```

`connectedDebugAndroidTest` uninstalls the app afterwards, so don't point it at a phone whose data you care about.

`local.properties` must contain `sdk.dir=...` (Android Studio writes it automatically).

## Using it

Three tabs.

| Tab | What it does |
|---|---|
| **Add** | Write, paste a link, or attach a photo, then **Add to brain**. Every post is shown with its suggested tags: switch off any you don't want, add your own, correct text, remove a whole post, then **Save**. A photo containing real text becomes **two posts**, the photo and its text. Sharing into the app from elsewhere lands here too. |
| **Library** | Words, `#tags`, sites, months, `today`, `yesterday`, `"exact phrases"`, `-exclusions`, with one-letter typo tolerance. Filter by To do / To read / To buy / To watch / Done and by Notes / Links / Photos. Matching tags are suggested as you type; tap one to select it. Nothing is listed until you search or filter; browse everything in Brain. |
| **Brain** | A globe of clusters. Drag to turn, pinch to zoom, tap a cluster to travel to it and list its things, tap a dot to open one. The chips underneath do the same without the globe. |

Opening something shows just the thing. **Advanced** (top right) holds its tags and why each was added, its details, the post it arrived with, editing and deletion.

### Read, watch, buy, do

Text such as "buy oat milk", "read Piranesi" or "call mom", and links to shops, videos or articles, are marked as waiting on you. Complete them with one tap on the circle in any list, the button on the item, or **Already …** while adding. The suggestion can always be changed or cleared.

## How tagging works (explainable, local)

1. **Read.** Photos go through **ML Kit text recognition** and **image labelling** (bundled, offline, discriminative models). Links have their title, description, keywords and Open Graph data fetched with Jsoup, the only network use besides loading preview images. A failure never blocks saving; it is retried on a later launch.
2. **Tag** (`tagging/TagEngine.kt`) from several sources, so each item gets many:

| Kind | Source |
|---|---|
| Yours | `#hashtags` and tags you type |
| Topic | The weighted rules in `tags.json` |
| Keyword | Names and significant words in the text, photo labels, page keywords, words in an unread link's address |
| Source | The website a link came from |
| Format | `note`, `link`, `photo`, `from-photo`, `list`, `price`, `contact` |
| Time | Month and year saved |

3. **Confirm.** Nothing is saved until you add it. A tag you remove is remembered and never re-added automatically.
4. **Cluster** (`brain/Clusterer.kt`). The tag that explains the most not-yet-placed items forms the next cluster, with your tags and topics preferred over keywords. Things sharing nothing yet collect in *loose ends*. Clusters that share tags are joined by arcs.

## Customising tags

Rules are data. Defaults live in [`app/src/main/assets/tags.json`](app/src/main/assets/tags.json).
In the app, **Library → Settings → Tag rules** lists every tag. Open one to change the words, sites and photo labels that earn it, try it on some text, switch it off or reset it; **New tag** adds your own. For patterns and weights, use that screen's menu to **Export rules**, edit the file, then **Import rules**. Imports replace definitions with the same name and add new ones, then add any newly matching tags to saved items.

```jsonc
{
  "version": 2,
  "minScore": 2.0,                 // raw score a tag needs
  "tags": [
    {
      "tag": "recipes",
      "implies": ["food"],         // broader tags that come along
      "words": ["recipe", "preheat"],   // strong: one is enough
      "hints": ["cook", "oven"],        // weak: needs two
      "domains": ["allrecipes.com"],    // link hosts
      "labels": ["Recipe"],             // ML Kit image labels
      "rules": [                        // optional full control
        { "type": "regex", "value": "\\b\\d+\\s?(g|ml|tbsp)\\b", "weight": 2, "field": "text" }
      ]
    }
  ]
}
```

Keywords match whole words, case- and accent-insensitively; values of 5+ characters also match as a prefix. `field` restricts a rule to `text`, `url`, `meta`, `ocr` or `labels`. Invalid regexes are ignored.

## Upgrading from the folder version

The database migrates in place. Items keep their content; the folder each sat in (and its parents) and its old tags become your tags, and the new automatic tags are added on first launch. Imported category rules are not carried over; add them as tag rules.

## Architecture

Single module, feature-grouped packages, MVVM with `StateFlow`, Hilt for DI. See [DESIGN.md](DESIGN.md) and [PRODUCT.md](PRODUCT.md).

```
app/src/main/java/app/secondbrain/
├── tagging/    Pure Kotlin: rules, keywords, action detection, tag engine
├── search/     Pure Kotlin search over saved items
├── brain/      Pure Kotlin clustering and sphere geometry
├── data/       Room (one items table + migration), repositories, DataStore settings
├── ingest/     Image store, ML Kit analyzer, link metadata, draft assembly
├── ui/         Compose screens (Add, Library, Brain, Detail, Settings), theme
└── di/         Hilt modules
```

Search and clustering run in memory over all items, which suits a personal collection; move search into SQLite FTS if it grows to many thousands.

## Known limitations

- Text recognition covers Latin script.
- A capture that is still being reviewed survives rotation and background process death, but not swiping the app away.
- No backup/export of items, only of tag rules. Android's auto-backup is on for app data.
- Release build is unminified and unsigned; add signing config before distributing.
