## Why

There is currently no way to see total cycling mileage per week/month/season/year without opening Strava or counting manually. Relying on the Strava API is not viable for a personal project once Strava's paid Standard tier ($11.99/mo, effective 2026-06-30) is required for API access. Strava's manual bulk export (`activities.csv` + track files) already contains everything needed, so this change builds a self-hosted, single-user app that ingests that export instead of calling the API.

## What Changes

- New passcode-gated login screen; no public registration, no multi-user support.
- New import flow: user uploads a Strava bulk-export `.zip` via drag-and-drop or file picker; backend unzips it, parses `activities.csv`, and upserts activities with `INSERT OR IGNORE` on Strava `activity_id` so repeat imports of the same or overlapping archive never create duplicates.
- Track files (`.gpx`/`.fit.gz`) matched to an imported activity are gzip-compressed and stored as a BLOB alongside the activity row, for future use (no map screen yet).
- New dashboard screen: year switcher, hero block (yearly total distance + trip count, average weekly distance, longest trip), a 52-week horizontal bar chart with peak-week highlighting and month labels, and a card-based list of trips (not a table) each showing distance, duration, avg speed, max speed.
- New SQLite-backed persistence (single file on a configurable persistent volume, WAL mode) as the system of record; list/stats endpoints never return the `track_gpx` BLOB.
- New Kotlin/Ktor backend exposing REST endpoints for import and for weekly/monthly/season/year aggregates, and a Kotlin Compose Multiplatform (wasmJs) frontend as a thin client with no business logic.
- New docker-compose deployment: `backend` + `frontend` (nginx serving static wasmJs and proxying `/api/*` to backend), passcode and data path supplied via `.env`.
- Explicitly excluded from this change: trip detail screen, map/track visualization UI, Strava API integration, automatic sync, multi-user/sharing.

## Capabilities

### New Capabilities
- `auth`: Passcode-based login that gates access to the entire application; no accounts, no registration.
- `activity-import`: Idempotent upload and ingestion of a Strava bulk-export zip into the activities store, including track BLOB storage.
- `activity-dashboard`: Year-scoped aggregate stats (hero numbers, weekly chart) and the trip list view, backed by REST endpoints over the imported activity data.

### Modified Capabilities
(none — greenfield project, no existing specs)

## Impact

- New repo-wide setup: Kotlin/Ktor backend, Kotlin Compose Multiplatform (wasmJs) frontend, SQLite database file, docker-compose deployment, nginx reverse proxy config, `.env`-driven configuration (`DATA_PATH`, `AUTH_PASSCODE`).
- No existing code or specs are affected; this establishes the initial system.
