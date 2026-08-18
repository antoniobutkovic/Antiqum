# Antiqum

Antiqum is a Kotlin Multiplatform mobile app with a Next.js API backed by Neon Postgres and deployed on Vercel.

## Repository layout

- `src/`, `db/`, and `scripts/` contain the Next.js App Router API and Neon database setup.
- `mobile/` contains the complete Kotlin Multiplatform Android/iOS application.
- Mobile features follow `Screen -> ViewModel -> Repository -> Service -> HttpClient`; see `mobile/ARCHITECTURE_RULES.md`.

## Backend setup

Requirements: Node.js 22 or newer and a Neon Postgres database.

1. In Neon, create a project and copy its pooled connection string.
2. Copy `.env.example` to `.env.local`, set `DATABASE_URL`, and generate a long random `CRON_SECRET`.
3. Run:

   ```shell
   npm install
   npm run db:setup
   npm run museums:sync
   npm run dev
   ```

`museums:sync` performs the initial resumable Wikidata import. It can be safely restarted; the database records its cursor and only publishes removals after a complete catalog scan.

The initial API exposes:

- `GET /api/health` — checks API and database availability.
- `GET /api/categories` — returns the categories consumed by the mobile categories feature.
- `GET /api/museums` — returns cached museum records using opaque cursor pagination, server-side search, category filtering, distance filtering, and stable sorting.
- `GET /api/museums/:id` — returns enriched museum details, including visitor information, organizations, images, attribution, accessibility, and exhibitions when available.
- `GET /api/museums/Q19675/indoor` — returns Antiqum's versioned Louvre indoor schematic, curated sights, notices, and routing graph.
- `GET /api/museums/Q19675/indoor/search?q=...` — searches Louvre rooms, landmarks, wings, and curated sights.
- `POST /api/museums/Q19675/indoor/navigate` — calculates a sight, location, accessible, or nearest-public-exit route.
- `POST /api/museums/Q19675/indoor/tour` — optimizes a route through favorite sights, optionally ending at the public visitor exit.
- `GET /api/cron/sync-museums` — advances the protected Wikidata synchronization job.

## Vercel deployment

The repository root is linked to the Vercel project `antiqum` and its production alias is [antiqum.vercel.app](https://antiqum.vercel.app). Add `DATABASE_URL` to Development, Preview, and Production after provisioning Neon, run `npm run db:setup`, then redeploy. Next.js is detected automatically; `vercel.json` pins the framework explicitly.

Add both `DATABASE_URL` and `CRON_SECRET` to Vercel. The configured cron advances the resumable Wikidata scan once per day; Vercel invokes it with `Authorization: Bearer $CRON_SECRET`. Run `npm run museums:sync` once before the first production release so the API starts with a complete catalog.

Museum details are sourced from Wikidata and Wikimedia Commons. The API does not require a paid places service: fields are returned when the community-maintained source contains them, image metadata retains its available licence and creator attribution, and map links use the museum coordinates with OpenStreetMap. Images use Wikidata `P18` first, then a free Wikipedia lead image, then suitable files from the museum's Wikidata-linked Commons category or Commons files whose structured data depicts that museum, before falling back to a logo.

The mobile app is configured to use `antiqum.vercel.app`. If the production domain changes, update its host without `https://` in:

- `ANTIQUM_API_HOST` in `mobile/gradle.properties` for Android.
- `ANTIQUM_API_HOST` in `mobile/iosApp/Configuration/Config.xcconfig` for iOS.

Do not commit `.env.local`; environment files and `.vercel` metadata are ignored.

## Museum discovery app

The mobile app can be used without an account. Its main experience includes:

- a persistent first-launch tutorial that can be replayed from Settings
- optional Google and iOS-only Apple profile entry points
- a native Google Maps discovery screen with custom museum markers and selected-museum previews
- searchable and filterable museum browsing
- editorial museum detail pages
- locally persisted favorite and visited states with profile totals in Settings
- light, dark, and system appearance modes
- a Louvre-only indoor guide with a five-level schematic, 392 official-plan locations, 30 numbered visitor highlights, room/sight starting-location search, sight and visitor-exit directions, accessibility-aware routes, artwork favorites, and optimized favorite tours

Museum records are periodically synchronized from Wikidata into Neon by the backend. Android and iOS only call the Antiqum API; they never query Wikidata directly. Network access follows the existing feature architecture:

```text
MuseumsScreen -> MuseumsViewModel -> MuseumsRepository -> MuseumsService -> HttpClient
```

The Museums list uses backend cursor pagination and automatically fetches the next page as the user approaches the end of the current list. Search, category, and sort changes start a new cursor-scoped query.

Reusable theme tokens and controls live in `mobile/composeApp/src/commonMain/kotlin/com/strive/antiqum/designsystem`.

### Louvre indoor guide

Open the Louvre (`Q19675`) from its museum detail page and choose **Explore inside the Louvre**. Indoor data follows the same app architecture and is cached locally after the first successful download. Artwork favorites are also persisted locally, so the app can calculate a fallback route when museum connectivity is weak.

The floor drawing and navigation graph are an original Antiqum schematic, not a reproduction of the Louvre's copyrighted visitor map and not an official Louvre service. Room names, wings, levels, official location IDs, artwork-search links, and reviewed map anchors come from the Louvre's accessible and interactive collection plans; textual data is reused under the Etalab Open Licence. The numbered sight set combines every highlight on the May 2026 official visitor map with four major landmarks from the interactive plan. Room and lift availability changes; the UI links to the Louvre's official gallery-access page and tells visitors to follow museum signs and staff. The nearest-exit action is normal visitor guidance only—during an emergency, users must follow illuminated emergency signs and staff instructions.

## Google Maps setup

The map uses only the native Maps SDK on each platform. It does not use a Map ID, Places, Routes, Street View, or Geocoding. Museum coordinates continue to come from Wikidata.

Create two restricted Google Maps API keys in the same billing-enabled Google Cloud project:

1. Enable **Maps SDK for Android** and **Maps SDK for iOS**.
2. For Android, create `mobile/secrets.properties`:

   ```properties
   MAPS_API_KEY=YOUR_RESTRICTED_ANDROID_KEY
   ```

3. For iOS, create `mobile/iosApp/Configuration/Secrets.xcconfig`:

   ```text
   GOOGLE_MAPS_API_KEY=YOUR_RESTRICTED_IOS_KEY
   ```

Restrict the Android key to package `com.strive.antiqum` and the app signing certificate SHA-1. Restrict the iOS key to bundle identifier `com.strive.antiqum`. Both secret files are ignored by Git. The iOS SDK is integrated through Swift Package Manager; open `mobile/iosApp/iosApp.xcodeproj` normally in Xcode.
