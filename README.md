# Antiqum

Antiqum is a Kotlin Multiplatform mobile app with a Next.js API backed by Neon Postgres and deployed on Vercel.

## Repository layout

- `src/`, `db/`, and `scripts/` contain the Next.js App Router API and Neon database setup.
- `mobile/` contains the complete Kotlin Multiplatform Android/iOS application.
- Mobile features follow `Screen -> ViewModel -> Repository -> Service -> HttpClient`; see `mobile/ARCHITECTURE_RULES.md`.

## Backend setup

Requirements: Node.js 22 or newer and a Neon Postgres database.

1. In Neon, create a project and copy its pooled connection string.
2. Copy `.env.example` to `.env.local` and set `DATABASE_URL`.
3. Run:

   ```shell
   npm install
   npm run db:setup
   npm run dev
   ```

The initial API exposes:

- `GET /api/health` — checks API and database availability.
- `GET /api/categories` — returns the categories consumed by the mobile categories feature.

## Vercel deployment

The repository root is linked to the Vercel project `antiqum` and its production alias is [antiqum.vercel.app](https://antiqum.vercel.app). Add `DATABASE_URL` to Development, Preview, and Production after provisioning Neon, run `npm run db:setup`, then redeploy. Next.js is detected automatically; `vercel.json` pins the framework explicitly.

The mobile app is configured to use `antiqum.vercel.app`. If the production domain changes, update its host without `https://` in:

- `ANTIQUM_API_HOST` in `mobile/gradle.properties` for Android.
- `ANTIQUM_API_HOST` in `mobile/iosApp/Configuration/Config.xcconfig` for iOS.

Do not commit `.env.local`; environment files and `.vercel` metadata are ignored.

## Museum discovery app

The mobile app currently launches directly into Antiqum without login. Its main experience includes:

- a first-launch location choice, currently using Zagreb as the discovery center
- a map-style discovery screen with museum markers and selected-museum previews
- searchable and filterable museum browsing
- editorial museum detail pages
- favorite and visited states for the active app session
- light, dark, and system appearance modes

Museum records are loaded directly from the official Wikidata Query Service. Network access follows the existing feature architecture:

```text
MuseumsScreen -> MuseumsViewModel -> MuseumsRepository -> MuseumsService -> HttpClient
```

Reusable theme tokens and controls live in `mobile/composeApp/src/commonMain/kotlin/com/strive/antiqum/designsystem`.
